package com.rpc.core.choosebalance;

import java.util.List;

import com.rpc.core.node.NodeInfo;

/**
 * 提供兩種， 默认客户端连接工厂
 * @author Jeremy
 * @deprecated 弃用此类,
 */
public class DefaultClientChooseConnectFactory implements  IClientChooseConnectFactory {

    public static final DefaultClientChooseConnectFactory INSTANCE = new DefaultClientChooseConnectFactory();

    private DefaultClientChooseConnectFactory() { }

    @Override
    public IClientChooser newChooser(List<NodeInfo> nodeInfos, List<Integer> connectedIds) {
        return null;
    }

    /**
     * 最小连接数选择<br>
     * 根据rpc连接数进行选择性连接, 每次需要新连接时, 拿连接数最小的节点进行连接<br>
     */
    private static final class LeastActiveChooser implements IClientChooser {

        private final List<NodeInfo> nodeInfos;
        private final List<Integer> connectedIds;

        LeastActiveChooser(List<NodeInfo> nodeInfos, List<Integer> connectedIds) {
            this.nodeInfos = nodeInfos;
            this.connectedIds = connectedIds;
        }

        @Override
        public NodeInfo next() {
            final int size = nodeInfos.size();
            if(size == 0){
                return null;
            }else if (size == 1){
                return nodeInfos.get(0);
            }
            int conn = 0;
            NodeInfo nodeInfo = null;
            //找到最小连接数的节点服务
            for (NodeInfo node: nodeInfos) {
                //如果已经连接过此节点, 跳过
                if (connectedIds.contains(node.getId())){
                    continue;
                }
                int connectNum = node.getConnectNum();
                if (conn == 0){
                    conn = connectNum;
                    nodeInfo = node;
                }
                if (conn < connectNum){
                    conn = connectNum;
                    nodeInfo = node;
                }
            }
            return nodeInfo;
        }
    }

    /**
     * 顺序连接, 根据当前的节点id, 顺序匹配指定下标的id
     */
    private static final class RoundRobinChooser implements IClientChooser {

        private final List<NodeInfo> nodeInfos;
        private final List<Integer> connectedIds;

        RoundRobinChooser(List<NodeInfo> nodeInfos, List<Integer> connectedIds) {
            this.nodeInfos = nodeInfos;
            this.connectedIds = connectedIds;
        }

        @Override
        public NodeInfo next() {
            return null;
        }
    }

}
