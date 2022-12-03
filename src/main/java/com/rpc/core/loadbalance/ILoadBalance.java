package com.rpc.core.loadbalance;

import java.util.List;

/**
 * 负载均衡接口<br>
 * 客户端请求发送消息时, 选择合适的客户端进行请求<br>
 * 预计默认实现 轮询、随机、权重、最少连接
 *
 */
public interface ILoadBalance {

    /**
     * 客户端id选择
     * @param clientIds
     * @return
     */
    int select(List<Integer> clientIds);

}
