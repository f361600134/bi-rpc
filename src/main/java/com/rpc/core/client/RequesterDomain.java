package com.rpc.core.client;

import static com.cat.net.network.client.IClientState.STATE_REMOVED;
import static com.cat.net.network.client.IClientState.STATE_RESERVED;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cat.net.network.client.RpcClientStarter;
import com.rpc.core.loadbalance.ILoadBalance;

/**
 * 请求器domain<br>
 * 某个节点下的信息, 维护了某个节点服务器当前节点启用的客户端, 以及已经连接的服务器id列表<br>
 * 当前问题:
 * 1. 如果判断当前节点压力比较大? 解决方案1:每个节点有一个回调缓存, 回调缓存定期清理, 清理后的回调缓存数量比较大, 则表示有压力, 开辟新的客户端连接
 * @author Jeremy
 */
public class RequesterDomain {
	

	private static final Logger log = LoggerFactory.getLogger(RequesterDomain.class);
	
	/**
	 * 节点类型
	 */
	private String nodeType;
	
	  /**
     * 负载均衡策略, 随机一个客户端
     */
    private ILoadBalance loadBalance;

    /**
     * 已连接的服务器的id列表
     */
    private List<Integer> connectIds = new CopyOnWriteArrayList<>();

    /**
     * key:连接的服务节点id
     * value: 客户端对象
     */
    private Map<Integer, RpcClientStarter> clientMap = new ConcurrentHashMap<>();

    public RequesterDomain(String nodeType, ILoadBalance loadBalance){
    	this.nodeType = nodeType;
        this.loadBalance = loadBalance;
    }

    /**
     * 根据id获得指定客户端
     * @param id
     * @return
     */
    public RpcClientStarter getClient(int id){
        return clientMap.get(id);
    }

    /**
     * 根据id获得指定客户端
     * @param client 客户端
     * @return
     */
	public void addClient(RpcClientStarter client){
        if(client == null){
            return;
        }
        connectIds.add(client.getConnectId());
        clientMap.put(client.getConnectId(), client);
    }

    /**
     * 根据id获得指定客户端
     * @param client 客户端
     * @return
     */
    public void removeClient(RpcClientStarter client){
        if(client == null){
            return;
        }
        connectIds.remove(Integer.valueOf(client.getConnectId()));
        clientMap.remove(client.getConnectId());
    }

    /**
     * 根据策略获取一个客户端
     * @return
     */
    public RpcClientStarter getClient(){
        int clientId = this.loadBalance.select(connectIds);
        if (clientId == 0){
            return null;
        }
        return getClient(clientId);
    }

    /**
     * 获取已连接的服务ids
     * @return
     */
    public List<Integer> getConnectIds(){
        return connectIds;
    }
    
    /**
     * 启动
     */
    public void start() {
    	for (RpcClientStarter client : clientMap.values()) {
			client.connect();
		}
    }
    
    /**
     * 销毁所有连接
     */
    public void destory() {
    	for (RpcClientStarter client : clientMap.values()) {
    		client.disConnect();
		}
    }
    
    /**
     * 根据状态获取数量
     * @param state
     * @return
     */
    public int getCount(int state) {
    	int count = 0;
    	for (RpcClientStarter client : clientMap.values()) {
    		if (client.getState() == state) {
    			count ++;
    		}
    	}
    	return count;
    }
    
    public String getNodeType() {
		return nodeType;
	}
    
    /**
     * 检测新增连接<br>
     * 如果所有连接请求数同时>2, 则新开辟连接
     */
    public boolean checkCreate(int minNum) {
    	if (clientMap.size() < minNum) {//表示没有客户端连接
			return true;
		}
    	boolean bool = false;
    	for (RpcClientStarter client : clientMap.values()) {
    		int callbackNum = client.getRealCallbackCache().getCallbackMap().size();
    		bool |= callbackNum > 2; //why 2? Create the new client as long as there are redundant callback.
		}
    	return bool;
    }
    
    /**
     * 检测保留连接<br>
     * 如果所有连接的请求数同时<=0, 则关闭最新的连接
     * 
     * 如果客户端请求量不足, 则标记为保留状态<br>
     * 1. 如果客户端为未连接状态, 优先设置为保留状态
     * 2. 如果无客户端为未连接状态, 取最后的连接设置为保留状态
     */
    public void checkAndReserve() {
    	boolean bool = false;
    	for (RpcClientStarter client : clientMap.values()) {
    		int callbackNum = client.getRealCallbackCache().getCallbackMap().size();
    		bool |= callbackNum <= 0;//why 0?
		}
    	if (bool) {
    		int id = connectIds.get(connectIds.size()-1);
        	RpcClientStarter client = getClient(id);
        	if (client == null) {
    			return;
    		}
        	client.disConnect();
        	log.info("===============> 定时销毁线程执行...保留客户端id:{}", id);
		}
    }
    
    /**
     * 检测客户端移除
     */
    public void checkAndremove() {
    	Iterator<Entry<Integer, RpcClientStarter>> iter = clientMap.entrySet().iterator();
    	while (iter.hasNext()) {
    		Entry<Integer, RpcClientStarter> entry = iter.next();
    		RpcClientStarter client=entry.getValue();
    		//log.info("checkRemove ===> clientId:{}, state:{}", client.getConnectId(), client.getState());
    		//if (client.getState() == STATE_REMOVED) {
    		if(client.compareAndSet(STATE_RESERVED, STATE_REMOVED)) {
    			//如果是移除状态,从map内移除掉
    			iter.remove();
    			//并且从已连接的服务器列表内移除掉
    			connectIds.remove(entry.getKey());
    			log.info("===============> 定时销毁线程执行...移除客户端id:{}", entry.getKey());
			}
    		//FIXME,这里用了双向引用不知道是否会存在无法从堆内回收的问题,所以直接把client设置为null,让gc工作
    		client = null;
		}
    	log.debug("checkAndremove, nodeType:{}, client[id|state]:{}", nodeType, info());
    }
    
    private String info() {
    	StringBuffer sb = new StringBuffer();
    	clientMap.values().forEach(client->{
    		sb.append("[").append(client.getConnectId());
    		sb.append("|").append(client.getState()).append("],");
    	});
    	if (sb.length()>0) {
    		sb.deleteCharAt(sb.length()-1);
		}
    	return sb.toString();
    }
    
}
