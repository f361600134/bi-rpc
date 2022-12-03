package com.rpc.core.server;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.cat.net.network.controller.DefaultRpcDispatcher;
import com.cat.net.network.tcp.RpcServerStarter;
import com.cat.net.terminal.IServer;
import com.rpc.common.RpcConfig;

public class RpcNetService implements InitializingBean{
	
	@Autowired 
	private RpcConfig config;
	
	/**
	 * 处理端, 没有回调监听, 默认使用普通连接控制, 普通转发
	 */
	@Autowired
	private DefaultRpcDispatcher rpcDispatcher;
	
	/**
	 * rpc的网络服务, 当做默认的tcp服务处理
	 */
	private IServer rpcServer;
	
	/**
	 * 启动
	 * @date 2020年7月9日
	 * @throws Exception
	 */
	public void startup() throws Exception {
		final String host = config.getHost();
		final int port = config.getPort();
		if (port > 0) {
			rpcServer = new RpcServerStarter(rpcDispatcher, host, port);
			rpcServer.startServer();
		}
	}
	
	/**
	 * 关闭
	 * @date 2020年7月9日
	 * @throws Exception
	 */
	public void shutdown() throws Exception {
		if (rpcServer != null) {
			rpcServer.stopServer();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		startup();
	}
	
	@PreDestroy
	public void destroy() throws Exception {
		shutdown();
	}

	public IServer getRpcServer() {
		return rpcServer;
	}
}
