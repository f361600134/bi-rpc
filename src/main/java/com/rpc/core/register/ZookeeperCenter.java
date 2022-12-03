package com.rpc.core.register;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.rpc.common.CommonConstant;
import com.rpc.common.RpcConfig;
import com.rpc.core.node.INodeManager;
import com.rpc.core.node.NodeInfo;

/**
 * zk注册中心类. 默认带有监听的zk注册中心, 实现了注册发现节点管理的基本业务 <br>
 * 节点分布如下： /1/2/2_1 /1/2/2_2  <br>
 * 1为项目名，2是节点类型划分，2_1就是节点编号  <br>
 * 所以，监听的应该是命名空间下的所有节点变动，只有本节点监听的节点才会触发 <br>
 * /fatiny/game/game_1					 <br>
 * /fatiny/gameCache/gameCache_1 		 <br>
 * /fatiny/login/login_1				 <br>
 * /fatiny/battle/battle_1				 <br>
 *
 * 1. /fatiny 初始化节点信息时判断, 作为项目节点, /game 启动时注册, 作为分类节点
 *
 * @author Jeremy
 */
@Component
public class ZookeeperCenter extends AbstractZookeeperCenter implements InitializingBean {

	private static Logger logger = LoggerFactory.getLogger(ZookeeperCenter.class);

	@Autowired
	protected RpcConfig rpcConfig;

	@Autowired
	protected INodeManager nodeManager;

	/**
	 * 节点树缓存
	 */
	protected TreeCache treeCache;
	
	/**
	 * 这里仅初始化命名空间
	 * @throws Exception
	 */
	@Override
	public void init() {
		super.init();
		// 初始化默认子节点监听器,默认监听项目名称下的所有子节点/
		String nodePath = CommonConstant.SLASH;
		// 初始化子孙节点监听器
		this.treeCache = new TreeCache(client, nodePath);
		this.treeCache.getListenable().addListener(new TreeCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
				TreeCacheEvent.Type type = event.getType();
				ChildData childData = event.getData();
				if (childData == null) {
					return;
				}
				String childPath = childData.getPath();
				if (childPath.equals(nodePath)) {
					// 如果变动节点是当前项目节点, 则忽略
					logger.debug("\n======变动节点是当前项目节点, 跳过:{}", childPath);
					return;
				}
				// 如果没有监听此节点, 跳过
				int index = childPath.indexOf(nodePath) + 1;
				int end = childPath.indexOf(nodePath, index);
				if (end < 0) {
					logger.debug("\n======当前不属于子节点, 跳过:{}", childPath);
					return;
				}
				String nodeType = childPath.substring(index, end);
				//if (!rpcConfig.getListenNodeTypes().contains(nodeType)) {
				if(!ArrayUtils.contains(rpcConfig.getListenNodeTypes(), nodeType)) {
					logger.debug("\n======没有监听此节点, 跳过:{}, nodeType:{}", childPath, nodeType);
					return;
				}

				byte[] data = childData.getData();
				String str = new String(data);
				NodeInfo node = JSON.parseObject(str, NodeInfo.class);
				if (node == null) {
					return;
				}

				if (type == TreeCacheEvent.Type.NODE_ADDED) {
					// 新增节点
					nodeManager.addNode(node);
					logger.debug("\n=====节点新增事件, 新增节点:{}, 信息:{}", node, nodeManager.info());
				} else if (type == TreeCacheEvent.Type.NODE_UPDATED) {
					// 节点数据变动
					nodeManager.removeNode(node);
					nodeManager.addNode(node);
					logger.debug("\n=====节点数据修改事件, 变动节点:{}, 信息:{}", node, nodeManager.info());
				} else if (type == TreeCacheEvent.Type.NODE_REMOVED) {
					// 移除节点
					nodeManager.removeNode(node);
					logger.debug("\n=====节点数据删除事件, 删除节点:{}, 信息:{}", node, nodeManager.info());
				}
			}
		});
	}

	/**
	 * 开始运行
	 * 
	 * @throws Exception 通用异常
	 */
	@Override
	public void start() throws Exception {
		super.start();
		this.treeCache.start();
		this.register();
	}

	/**
	 * 外部添加字节点监听
	 * 
	 * @param listenable
	 */
	public void addTreeListener(TreeCacheListener listenable) {
		if (listenable == null) {
			throw new RuntimeException("listenable can not be null");
		}
		this.treeCache.getListenable().addListener(listenable);
	}

	@Override
	public String namespace() {
		return rpcConfig.getNamespace();
	}

	@Override
	public NodeInfo createNode() {
		return serverNode = NodeInfo.create(
				rpcConfig.getServerId()
				, rpcConfig.getHost()
				, rpcConfig.getPort()
				, rpcConfig.getNodeType()
				, rpcConfig.getNamespace()
				);
	}

	@Override
	public String address() {
		return rpcConfig.getAddress();
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		logger.info("======zookeeper 开始初始化了======");
		init();
		logger.info("======zookeeper 开始启动了======");
		start();
	}
}
