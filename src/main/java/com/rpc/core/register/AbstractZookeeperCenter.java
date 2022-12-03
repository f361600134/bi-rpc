package com.rpc.core.register;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.rpc.common.CommonConstant;
import com.rpc.core.node.NodeInfo;

/**
 * zk注册中心抽象类. 这里仅仅只有对zkClient的初始化和启动, 默认生成本服务的zk节点
 * @author Jeremy
 */
public abstract class AbstractZookeeperCenter implements IRegisterCenter<CuratorFramework>{

    protected  Logger logger = LoggerFactory.getLogger(AbstractZookeeperCenter.class);

    protected String address;

    /**
     * 命名空间, zk服务器路径的根节点
     */
    protected String namespace;
    /**
     * 当前节点信息, 当前进程服务的节点信息
     */
    protected NodeInfo serverNode;
    /**
     * zkClient
     */
    protected CuratorFramework client;

    public String getNamespace() {
        return namespace;
    }

    @Override
    public CuratorFramework getClient() {
        return client;
    }
    
    /**
     * 注册中心抽象类构造器
     * @param address
     * @param namespace
     */
    public AbstractZookeeperCenter () {
    }

    /**
     * 这里仅初始化命名空间
     */
    @Override
    public void init() {
        //初始化命名空间,本服务节点
        this.namespace = namespace();
        this.serverNode = createNode();
        this.address = address();
        //初始化客户端
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        this.client = CuratorFrameworkFactory.builder().connectString(address)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(5000)
                .retryPolicy(retryPolicy)
                .namespace(namespace)
                .build();
        
        logger.info("ZookeeperStarter start to connect zk, address:{}, client初始化:{}", address, this.client == null);
    }

    /**
     * 开始运行, 启动时判断是否存在分类节点, 如果不存在则新建此节点
     * @throws Exception 通用异常
     */
    @Override
    public void start() throws Exception{
        //启动客户端
        this.client.start();
        //判断节点是否存在
        String nodePath = CommonConstant.SLASH.concat(serverNode.getType());
        Stat statExist = client.checkExists().forPath(nodePath);
        if(statExist == null){
            //节点不存在, 新建节点
            this.client.create()
                    .creatingParentContainersIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(nodePath);
        }
    }

    /**
     * 注册信息到服务器, 同时注册中心初始化所有zk节点信息
     * @throws Exception 通用异常
     */
    @Override
    public void register() throws Exception{
        //序列化节点信息, 保存至zkServer
        String json = JSON.toJSONString(this.serverNode);
        //注册到zk,当前客户端节点为临时节点,会话关闭后通知其他节点移除此节点信息
        this.client.create()
                .creatingParentContainersIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(this.serverNode.getName(), json.getBytes());
    }

    /**
     * 停止运行
     * @throws Exception 通用异常
     */
    @Override
    public void shutdown() throws Exception{
        //销毁此节点
        this.client.delete()
                .deletingChildrenIfNeeded()
                .forPath(serverNode.getType());
    }

    /**
     * 销毁连接
     */
    @Override
    public void destroy() {
        //关闭连接
        this.client.close();
    }
    
    /**
     * 命名空间
     * @return
     */
    public abstract String namespace();
    
    /**
     * 命名空间
     * @return
     */
    public abstract String address();
    
    /**
     * 节点创建
     * @return
     */
    public abstract NodeInfo createNode();

}
