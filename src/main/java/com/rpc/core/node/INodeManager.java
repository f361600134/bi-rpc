package com.rpc.core.node;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public interface INodeManager {
	
	 /**
     * 加入节点
     */
    public void addNode(NodeInfo node); 

    /**
     * 批量注册节点
     */
    public void addNodes(Collection<NodeInfo> nodes);

    /**
     * 移除一个节点
     * @param nodeId 节点id
     */
    public void removeNode(String nodeType, int nodeId);

    /**
     * 移除一个服务节点
     *
     * @param node 节点对象
     */
    public void removeNode(NodeInfo node);

    /**
     * 根据服务器id获取节点
     *
     * @param nodeId 节点id
     * @return
     */
    public NodeInfo getNode(String nodeType, int nodeId);

    /**
     * 根据节点类型获取多个服务器节点
     *
     * @param type
     * @return
     */
    public List<NodeInfo> getNodesByType(String type);

    /**
     * 获取最少连接数的节点, 要抽出去做成策略
     * @param type 分组类型
     * @param need  需要的数量
     * @param filter 断言过滤
     * @return
     */
    public List<NodeInfo> getNodes(String type, int need, Predicate<? super NodeInfo> filter);

    /**
     * 输出节点信息
     *
     * @return
     */
    public String info();

}
