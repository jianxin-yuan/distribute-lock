package com.yuan.lock.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yuan
 * @date 2020/4/16 13:54
 * 基于jedis实现的redis lock : 利用set nx ex 加锁和lua脚本解锁
 * 利用set nx ex的缺点:
 * 1.在集群环境下,如果在master上加锁成功,但还没有同步到slave上,此时master挂了,发生故障转移,slave升级为master,
 * 那么这个锁就可以被其他客户端获取,导致多个客户端获取到同一个锁
 */
public class JedisLock {

    private static int DEFAULT_EXPIRE = 20;
    private static String OK = "OK";

    public boolean tryLock(Jedis jedis, String key, String uniqueId) {
        return tryLock(jedis, key, uniqueId, DEFAULT_EXPIRE);
    }

    /**
     * 获取锁
     *
     * @param key      key
     * @param uniqueId 唯一值
     * @param expire   到期时间
     * @return boolean
     */
    public boolean tryLock(Jedis jedis, String key, String uniqueId, int expire) {
        SetParams params = new SetParams().nx().ex(expire);
        return OK.equals(jedis.set(key, uniqueId, params));
    }

    /**
     * 释放锁:使用lua脚本进行释放锁操作
     *
     * @param key      key
     * @param uniqueId 唯一值
     * @return boolean
     */
    public boolean unLock(Jedis jedis, String key, String uniqueId) {
        String luaScripts = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";

        return jedis.eval(luaScripts, Collections.singletonList(key), Collections.singletonList(uniqueId)).equals(1L);
    }

    public static void main(String[] args) throws Exception {
        int threadSize = 100;
        JedisLock lock = new JedisLock();
        JedisPool jedisPool = new JedisPool("127.0.0.1", 6379);

        String key = "yuan";
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        CountDownLatch countDownLatch = new CountDownLatch(threadSize);
        ExecutorService pool = Executors.newFixedThreadPool(threadSize);
        for (int i = 0; i < threadSize; i++) {
            pool.execute(() -> {
                String uniqueId = "" + Thread.currentThread().getId();
                Jedis jedis = jedisPool.getResource();
                try {
                    if (lock.tryLock(jedis, key, uniqueId)) {
                        successCount.incrementAndGet();
                        System.out.println(uniqueId + "加锁成功");
                    } else {
                        failedCount.incrementAndGet();
                        System.out.println(uniqueId + "加锁失败");
                    }
                } finally {
                    countDownLatch.countDown();
                    lock.unLock(jedis, key, uniqueId);
                    jedis.close();
                }
            });
        }

        countDownLatch.await();
        jedisPool.close();
        pool.shutdown();
        System.out.println("lock success count  = " + successCount.get());
        System.out.println("lock failed count  = " + failedCount.get());
    }


}
