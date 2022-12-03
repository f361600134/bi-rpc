package com.rpc.core.loadbalance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 带有权重的负载均衡, 暂未实现, 对于权重, 最小连接, 调用次数这些参数存储, 没有想好, 所以暂不实现
 */
@Deprecated
public class WeightLoadBalance extends AbstractLoadBalance{

    /**
     * 回收时间, ms
     */
    private static final int RECYCLE_PERIOD = 60000;

    /**
     * 缓存每个节点的权重
     * key: 节点id
     * value: 权重信息
     */
    private Map<Integer, WeightInfo> methodWeightMap = new ConcurrentHashMap<>();
    private AtomicBoolean updateLock = new AtomicBoolean();


    @Override
    public int doSelect(List<Integer> clientIds) {
        return 0;
    }

    /**
     * 权重信息
     */
    static class WeightInfo{

        private AtomicLong current = new AtomicLong(0);
        private long lastUpdate;
        private int weight;

        public WeightInfo() {
        }

        public AtomicLong getCurrent() {
            return current;
        }

        public void setCurrent(AtomicLong current) {
            this.current = current;
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }

}
