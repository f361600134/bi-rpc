package com.rpc.core.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import com.rpc.exception.DuplicateNodeException;

/**
 * 节点管理类, 缓存所有已注册的节点信息.
 * @author Jeremy
 */
@Component
public class NodeManager implements INodeManager{
	
    /**
     * key: 节点类型
     * value: key: 节点id
     * value: 节点数据
     */
    private final Map<String, Map<Integer, NodeInfo>> nodeTypeMap = new ConcurrentHashMap<>();

    /**
     * 加入节点
     */
    public void addNode(NodeInfo node) {
        Map<Integer, NodeInfo> nodeMap = nodeTypeMap.get(node.getType());
        if (nodeMap == null) {
            nodeMap = new ConcurrentHashMap<>();
            nodeTypeMap.put(node.getType(), nodeMap);
        }
        if (nodeMap.containsKey(node.getId())) {
            throw new DuplicateNodeException("Node id [" + node.getId() + "] is already registered.");
        }
        nodeMap.put(node.getId(), node);
    }

    /**
     * 批量注册节点
     */
    public void addNodes(Collection<NodeInfo> nodes) {
        nodes.forEach(this::addNode);
    }

    /**
     * 移除一个节点
     * @param nodeId 节点id
     */
    public void removeNode(String nodeType, int nodeId) {
        Map<Integer, NodeInfo> nodeMap = nodeTypeMap.get(nodeType);
        if (nodeMap == null) {
            return;
        }
        nodeMap.remove(nodeId);
    }

    /**
     * 移除一个服务器节点
     *
     * @param node 节点对象
     */
    public void removeNode(NodeInfo node) {
        if (node == null) {
            return;
        }
        removeNode(node.getType(), node.getId());
    }

    /**
     * 根据服务器id获取节点
     *
     * @param nodeId 节点id
     * @return
     */
    public NodeInfo getNode(String nodeType, int nodeId) {
        Map<Integer, NodeInfo> nodeMap = nodeTypeMap.get(nodeType);
        if (nodeMap == null) {
            return null;
        }
        return nodeMap.get(nodeId);
    }

    /**
     * 根据节点类型获取多个服务器节点
     *
     * @param type
     * @return
     */
    public List<NodeInfo> getNodesByType(String type) {
        Map<Integer, NodeInfo> nodeMap = nodeTypeMap.get(type);
        if (nodeMap == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(nodeMap.values());
    }

    /**
     * 获取最少连接数的节点, 要抽出去做成策略
     * @param type 分组类型
     * @param need  需要的数量
     * @param filter 断言过滤
     * @return
     */
    public List<NodeInfo> getNodes(String type, int need, Predicate<? super NodeInfo> filter){
        Objects.requireNonNull(filter);
        List<NodeInfo> ret = new ArrayList<>(need);
        //拷贝一个副本, 对接点做排序
        final List<NodeInfo> nodeInfos =  getNodesByType(type);
        final int size = nodeInfos.size();
        if (size == 0) {
            return ret;
        } else if (size == 1) {
            NodeInfo node = nodeInfos.get(0);
            if (filter.test(node)){
                ret.add(node);
            }
            return ret;
        }
        //节点大于1, 排序取指定数量返回
        Collections.sort(nodeInfos, ((o1, o2) -> {
            return Integer.compare(o1.getId(), o2.getId());
        }));
        for (NodeInfo nodeInfo : nodeInfos) {
            if (filter.test(nodeInfo)){
                ret.add(nodeInfo);
            }
            if (ret.size() >= need) {
				break;
			}
        }
        return ret;
    }

    /**
     * 输出节点信息
     *
     * @return
     */
    public String info() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n节点类型总数量:").append(nodeTypeMap.keySet().size());
        for (String key: nodeTypeMap.keySet()) {
            Map<Integer, NodeInfo> nodeMap = nodeTypeMap.get(key);
            builder.append("\n当前节点类型:").append(key);
            builder.append(", 子节点数量:").append(nodeMap.size());
            builder.append(", 详情:").append(nodeMap);
        }
        return builder.toString();
    }

}
