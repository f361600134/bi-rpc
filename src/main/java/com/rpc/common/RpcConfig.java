package com.rpc.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RpcConfig {

    /**
     * 项目名称
     * /birpc/cat/game -> [{"serverId":1, "host":"0.0.0.0", "port":8081, "nodeType":1}]
     * /birpc/cat/battle -> [{"serverId":2, "host":"0.0.0.0", "port":8082, "nodeType":2}]
     */
    @Value("${rpc.connection.namespace}")
    private String namespace;
    /**
     * zkServer的连接信息,ip:port
     * 139.9.44.104:2181
     */
    @Value("${rpc.connection.address}")
    private String address;

    /**
     * 节点类型
     */
    @Value("${rpc.connection.nodeType}")
    private String nodeType;

    /**
     * 监听节点类型列表, 表示可以监听多个节点组
     */
   //@Value("#{'${rpc.connection.listenNodeTypes}'.split(',')}")
   // private List<String> listenNodeTypes;
    @Value("${rpc.connection.listenNodeTypes:}")
    private String[] listenNodeTypes;

    /**
     * 初始连接数
     */
    @Value("${rpc.connection.initNum}")
    private int initNum;

    /**
     * 最大连接数
     */
    @Value("${rpc.connection.maxNum}")
    private int maxNum;

    /**
     * 是否开启监控
     */
    @Value("${rpc.connection.monitor}")
    private boolean monitor;

    /**
     * 服务器id
     */
    @Value("${rpc.connection.serverId}")
    private int serverId;
    @Value("${rpc.connection.host:}")
    private String host;
    @Value("${rpc.connection.port:0}")
    private int port;


    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public int getInitNum() {
        return initNum;
    }

    public void setInitNum(int initNum) {
        this.initNum = initNum;
    }

    public int getMaxNum() {
        return maxNum;
    }

    public void setMaxNum(int maxNum) {
        this.maxNum = maxNum;
    }

    public boolean isMonitor() {
        return monitor;
    }

    public void setMonitor(boolean monitor) {
        this.monitor = monitor;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String[] getListenNodeTypes() {
		return listenNodeTypes;
	}
}
