package com.rpc.core.loadbalance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 循环负载均衡<br>
 *     rpc调用, 此处实现无状态服务器的调用, 所以不需要保证has一致性, 如果要保证hash一致, 使用人员自己实现
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance{

    private AtomicInteger seq = new AtomicInteger();

    @Override
    public int doSelect(List<Integer> clientIds) {
        final int num = seq.incrementAndGet();
        final int size = clientIds.size();
        int next = num % size;
        return clientIds.get(next);
    }
}
