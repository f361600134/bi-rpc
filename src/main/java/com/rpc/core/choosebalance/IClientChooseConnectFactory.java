package com.rpc.core.choosebalance;

import java.util.List;

import com.rpc.core.node.NodeInfo;

/**
 * 客户端选择合适的服务节点连接工厂类
 * @author Jeremy
 * @deprecated 弃用, 使用工厂模式作为节点选择实在不合适, 直接在业务里面处理吧
 */
public interface IClientChooseConnectFactory {

    /**
     * 返回一个合适的节点
     * @param nodeInfos 所有被监听的节点信息
     * @param connectedIds 已连接的id, 被过滤的id列表
     * @return
     */
    IClientChooser newChooser(List<NodeInfo> nodeInfos, List<Integer> connectedIds);

    interface IClientChooser {
        NodeInfo next();
    }

}
