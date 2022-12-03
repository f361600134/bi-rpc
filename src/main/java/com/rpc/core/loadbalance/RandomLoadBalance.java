package com.rpc.core.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RoundRobinLoadBalance 参考这个做法去实现, 缓存这些数据
 * 随机选择策略
 */
public class RandomLoadBalance extends AbstractLoadBalance{

    @Override
    public int doSelect(List<Integer> clientIds) {
        int length = clientIds.size();
        return clientIds.get(ThreadLocalRandom.current().nextInt(length));
    }

}
