package com.rpc.core.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cat.net.network.client.IClientState;
import com.cat.net.network.client.RpcClientStarter;
import com.cat.net.network.controller.DefaultRpcDispatcher;
import com.cat.net.network.rpc.IRpcAuthenticationListenable;
import com.rpc.common.RpcConfig;
import com.rpc.core.loadbalance.RandomLoadBalance;
import com.rpc.core.loadbalance.RoundRobinLoadBalance;
import com.rpc.core.node.NodeInfo;
import com.rpc.core.node.NodeManager;


/**
 * 请求端管理类(客户端管理类)<br>
 * 
 * 职责: 动态初始化客户端, 从节点组拿到节点信息<br>
 * 
 * 此类注册成功后, 开启异步定时调用
 * 1. 定时任务, 用于跟服务器的心跳.
 * 2. 定时任务, 用于检测连接池内的连接是否可用, 销毁, 重建.
 * 
 * 1. 初始化客户端, 加入客户端连接池
 * 2. 定时检测客户端连接, 如果连接失效, 尝试重连
 * 3. 如果重连失败(尝试大于一定次数后), 销毁此连接.
 * 4. 检测连接数量, 如果小于指定次数, 则寻找新的节点, 创建连接. 
 */
@Component
public class RequesterManager implements InitializingBean{
	
	protected Logger logger = LoggerFactory.getLogger(RequesterManager.class);
	
    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private RpcConfig rpcConfig;

    @Autowired
    private DefaultRpcDispatcher rpcDispatcher;
    
    @Autowired(required = false)
    private IRpcAuthenticationListenable listenable;
    
    /**
     * key: 节点分组
     * value: 请求方domain
     */
    private final Map<String, RequesterDomain> clientTypeMap = new ConcurrentHashMap<>();
    
    /**
     * 客户端生产器
     */
    private final ClientCreator clientCreator = new ClientCreator();
    
    /**
     *客户端清理器 
     */
    private final ClientCleaner clientCleaner = new ClientCleaner();
    /**
     * 带有定时任务的单线程线程池<br>
     * 1.用于检测网络连接, 心跳机制<br>
     * 2.用于动态新增<br>
     * 3.用于释放连接<br>
     */
    private ScheduledExecutorService executor;
    
    /**
     * 根据分组获得指定的客户端
     * @param nodeType
     * @return
     */
    public RpcClientStarter getClient(String nodeType, int id) {
        RequesterDomain domain = clientTypeMap.get(nodeType);
        if (domain == null) {
            return null;
        }
        return domain.getClient(id);
    }

    /**
     * 根据分组根据策略随机获得客户端
     * @param nodeType
     * @return
     */
    public RpcClientStarter getClient(String nodeType) {
        RequesterDomain domain = clientTypeMap.get(nodeType);
        if (domain == null) {
            return null;
        }
        return domain.getClient();
    }

    /**
     * 添加一个客户端
     * @param client
     */
    public void addClient(RpcClientStarter client){
        String nodeType = client.getNodeType();
        RequesterDomain domain = clientTypeMap.get(nodeType);
        if (domain == null){
            domain = new RequesterDomain(nodeType, new RandomLoadBalance());
            clientTypeMap.put(nodeType, domain);
        }
        domain.addClient(client);
    }

    /**
     * 根据分组获取连接已连接的id
     * @param nodeType
     * @return
     */
    public List<Integer> getConnectIds(String nodeType) {
        RequesterDomain domain = clientTypeMap.get(nodeType);
        if (domain == null) {
            return Collections.emptyList();
        }
        return domain.getConnectIds();
    }
    
    
    /**
     * 初始化,初始化指定数量的客户端连接, 加入到连接池, 当前连接并未启动, 不可用
     */
    public void initialize() {
        //根据监听的节点, 初始化客户端. 如果没有要监听的节点, 则不需要初始化客户端连接
        for (String nodeType: rpcConfig.getListenNodeTypes()) {
        	//初始化domain
        	RequesterDomain domain = new RequesterDomain(nodeType, new RoundRobinLoadBalance());
        	clientTypeMap.put(nodeType, domain);
        	
        	//初始化节点下的连接
            List<Integer> connectIds = getConnectIds(nodeType);
            final int need = rpcConfig.getInitNum() - connectIds.size();
            if (need <= 0) {
				continue;
			}
            createNewClients(nodeType, need);
        }
    }
    
    /**
     * 创建新的客户端
     */
    private void createNewClients(String nodeType, int needNum) {
    	List<Integer> connectIds = getConnectIds(nodeType);
    	//logger.info("===============> 创建新客户端连接...节点类型:{}, 新增数量:{}, 当前连接id:{}", nodeType, needNum, connectIds);
    	List<NodeInfo> nodeInfos = nodeManager.getNodes(nodeType, needNum, nodeInfo -> {
            return !connectIds.contains(nodeInfo.getId());
        });
    	//实例化客户端
        for (NodeInfo nodeInfo: nodeInfos) {
        	RpcClientStarter client = new RpcClientStarter(rpcDispatcher, listenable, rpcConfig.getServerId(), nodeInfo.getId(),
                    nodeInfo.getType(), nodeInfo.getIp(), nodeInfo.getPort());
        	logger.info("ready to connect host:{}, port:{}, nodeType:{}", nodeInfo.getIp(), nodeInfo.getPort(), nodeInfo.getType());
        	client.connect();
        	addClient(client);
        }
    }
    
    /**
     * 启动, 开启连接, 用于连接池开启连接, 尝试连接server.<br>
     * 并且需要开启一条线程, 定时检测连接可用性<br>
     * 如果有监听的节点, 才开启线程池进行监听, 否则不坚挺
     */
    public void start() {
    	if (rpcConfig.getListenNodeTypes().length <= 0) {
			return;
		}
    	executor = Executors.newSingleThreadScheduledExecutor();
    	//定时检查连接否开启连接
    	executor.scheduleWithFixedDelay(clientCreator, 0, 10, TimeUnit.SECONDS);
    	//定时销毁连接
    	executor.scheduleWithFixedDelay(clientCleaner, 0, 10, TimeUnit.SECONDS);
    }
    
    /**
     * 
     * 关闭
     */
    public void destory() {
    	for (RequesterDomain domain : clientTypeMap.values()) {
    		domain.destory();
		}
    }
    
    /**
     * 客户端生产器
     * @author Jeremy
     */
    private final class ClientCreator implements Runnable {
		@Override
		public void run() {
			for (RequesterDomain domain : clientTypeMap.values()) {
    			int count = domain.getCount(IClientState.STATE_CONNECTED);
    			if (count >= rpcConfig.getMaxNum()) {
					continue;
				}
    			if(domain.checkCreate(rpcConfig.getInitNum())) {
    				//每次新增1个
        			createNewClients(domain.getNodeType(), 1);
    			}
    		}
		}
    }
    
    /**
     * 客户端清理器
     * @author Jeremy
     */
    private final class ClientCleaner implements Runnable{
		@Override
		public void run() {
			for (RequesterDomain domain : clientTypeMap.values()) {
				//检测释放连接
				domain.checkAndremove();
				int count = domain.getCount(IClientState.STATE_CONNECTED);
				if (count <= rpcConfig.getInitNum()) {
					continue;
				}
				//保留连接
				domain.checkAndReserve();
			}
		}
    }

	@Override
	public void afterPropertiesSet() throws Exception {
		this.initialize();
		this.start();
	}

    
}
