package com.yuan.lock.redisson;

import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

/**
 * @author yuan
 * @date 2020/4/16 17:08
 * <p>
 * https://mp.weixin.qq.com/s/8uhYult2h_YUHT7q7YCKYQ
 */
public class RedissonLock {

    public static void main(String[] args) {
        //redLock();
    }

    /**
     * 基于redisson实现的redLock
     */
    private static void redLock() {
        String resourceName = "test";
        Config config1 = new Config();
        config1.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonClient redissonClient1 = Redisson.create(config1);
        RLock rLock1 = redissonClient1.getLock(resourceName);

        Config config2 = new Config();
        config2.useSingleServer().setAddress("redis://127.0.0.1:6380");
        RedissonClient redissonClient2 = Redisson.create(config2);
        RLock rLock2 = redissonClient2.getLock(resourceName);

        Config config3 = new Config();
        config3.useSingleServer().setAddress("redis://127.0.0.1:6381");
        RedissonClient redissonClient3 = Redisson.create(config3);
        RLock rLock3 = redissonClient3.getLock(resourceName);

        RedissonRedLock redissonRedLock = new RedissonRedLock(rLock1, rLock2, rLock3);
        try {
            // t1: 最多等待10s,t2:key有效期60s
            if (redissonRedLock.tryLock(10, 60, TimeUnit.SECONDS)) {
                System.out.println("加锁成功");
            } else {
                System.out.println("加锁失败");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            redissonRedLock.unlock();
            System.out.println("解锁");
        }
    }

    /**
     * 基于redisson的普通分布式锁
     */
    private static void distributeLock() {
        Config config = new Config();
        //单机模式
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        //主从模式
        //config.useReplicatedServers().addNodeAddress("redis://127.0.0.1:6379");
        //哨兵模式
        //config.useSentinelServers()
        //        .addSentinelAddress("redis://127.0.0.1:6379", "redis://127.0.0.1:6380", "redis://127.0.0.1:6381")
        //        .setMasterName("myMaster");
        //集群模式
        //config.useClusterServers().addNodeAddress("redis://127.0.0.1:6379", "redis://127.0.0.1:6380", "redis://127.0.0.1:6381");


        RedissonClient redissonClient = Redisson.create(config);
        RLock rLock = redissonClient.getLock("test");
        try {
            // t1: 最多等待10s,t2:key有效期60s
            if (rLock.tryLock(10, 60, TimeUnit.SECONDS)) {
                System.out.println("加锁成功");
            } else {
                System.out.println("加锁失败");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
            System.out.println("解锁");
        }
    }
}
