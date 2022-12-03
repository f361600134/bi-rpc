birpc, 全名: bi-direction rpc, 这是一个基于zookeeper实现的分布式rpc框架.
其中协议层使用了protostuff,可以保证序列化的速度, 使用netty作为网络基础服务.
依赖cat-net, cat-net是一个集成了http,tcp,websocket以及rpc服务的网络库.实现了基础的rpc调用.
birpc在网络上层提供了动态发现, 自动注册, 动态扩容消峰, 动态回收等一些自动化功能.

作为RPC双向通讯工具, 实现了client to server, server to client的双向调用, 可常用于游戏业务不同节点的相互调用. 
当然也可以支持单向通讯,具体看使用者的业务场景.

#Quick Start
在此, 我们做一个Client, Server 节点的demo演示.
Client代码编写
1. 引入依赖
```xml
    <dependency>
        <groupId>com.rpc</groupId>
        <artifactId>bi-rpc</artifactId>
        <version>0.0.1</version>
    </dependency>
```
2.注入转发服务
```java
/**
 * 网络组件
 * 可以根据需要注册
 * @author Jeremy
 */
@Configuration
public class NetworkComponent {
	private static final Logger logger = LoggerFactory.getLogger(NetworkComponent.class);
	/**
	 * RPC服务
	 */
	@Bean
	public RpcDispatcher rpcController(List<IRpcController> controllers) {
		logger.info("注册[DefaultRpcDispatcher]服务, size:{}", controllers.size());
		RpcDispatcher dispatcher = new RpcDispatcher();
		try {
			dispatcher.initialize(controllers);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("注册[DefaultRpcDispatcher]服务失败, 异常:", e);
		}
		return dispatcher;
	}
	
}
```
3.加入相关配置, 当前节点是client,监听server节点
```properties
#命名规则:/框架节点/项目名字/分组类型/节点名字->data
rpc.connection.namespace = test
#远程服务地址 139.9.44.104:2181
rpc.connection.address=139.9.44.104:2181
#初始连接数
rpc.connection.initNum = 1
#最大连接数
rpc.connection.maxNum = 2
#是否开启监控
rpc.connection.monitor = false
#节点类型
rpc.connection.nodeType = client
#监听节点,表示需要监听的节点类型列表,可以配置多个,如果那些节点变动,此节点做相应处理
rpc.connection.listenNodeTypes = server
#服务器信息
#1:账号服节点,2:游戏服节点,3:战斗服节点,4:...
rpc.connection.serverId=1
rpc.connection.host=127.0.0.1
rpc.connection.port=9001
#节点类型一下弃用,不属于游戏内的配置,属于rpc配置
#cat.game.server.nodeType=2
#rpc.connection.listenNodeTypes=1,3
```
4.寻找一个服务节点进行rpc请求连接, 并设置指定回调方法
```java
RpcClientStarter client = requesterManager.getClient(ServerConstant.NODE_TYPE_SERVER);
if(client == null) {
    log.info("没有找到合适的节点, 节点类型{}", ServerConstant.NODE_TYPE_SERVER);
    continue;
}
ReqIdentityAuthenticate req = ReqIdentityAuthenticate.create(rpcConfig.getServerId(), rpcConfig.getNodeType());
log.info("目标节点类型: {}, 发送验证参数:[{}|{}], 客户端:{}", ServerConstant.NODE_TYPE_SERVER, rpcConfig.getServerId(), rpcConfig.getNodeType(), client);
client.ask(req, 300L, new ReqIdentityAuthenticateCallback());
```

5.编写回调ReqIdentityAuthenticateCallback方法
```java
/**
 * 登录回调, 如果账号服与游戏服长连接的话, 
 * @author Jeremy
 */
@Rpc(value = ProtocolId.RespIdentityAuthenticate, listen = Rpc.RESPONSE)
public class ReqIdentityAuthenticateCallback implements IResponseCallback<RespIdentityAuthenticate>{
	
	private static Logger logger = LoggerFactory.getLogger(ReqIdentityAuthenticateCallback.class);

	@Override
	public void receiveResponse(RespIdentityAuthenticate response) {
		logger.info("RPC响应返回=========> response.code:{}", response.getCode());
	}

	@Override
	public void handleException(Exception ex) {
		logger.info("RPC响应返回异常========> response error:{}", ex);
	}
}
```

6.启动入口
```java
public static void main( String[] args )
{
    long startTime = System.currentTimeMillis();
    //扫描配置环境
    AbstractApplicationContext ctx = new ClassPathXmlApplicationContext("spring-context.xml");
    ctx.registerShutdownHook();
    ctx.start();
    logger.info("Start server successful， cost time:{}ms", (System.currentTimeMillis() - startTime));
    try {
        TimeUnit.SECONDS.sleep(2000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}
```

可以启动客户端节点, 等待发现服务节点...

Server端代码编写
1.引入依赖
```xml
    <dependency>
        <groupId>com.rpc</groupId>
        <artifactId>bi-rpc</artifactId>
        <version>0.0.1</version>
    </dependency>
```

2.Rpc服务端代码编写, Server端需要启动网络服务, 所以注入网络RpcNetService. RpcDispatcher作为请求转发器.
```java
/**
 * 网络组件
 * 可以根据需要注册
 * @author Jeremy
 */
@Configuration
public class NetworkComponent {
	
	private static final Logger logger = LoggerFactory.getLogger(NetworkComponent.class);
	/**
	 * 注册游戏服分发处理器
	 * @return
	 */
	@Bean
	public RpcNetService netService() {
		logger.info("注册[RpcNetService]服务");
		return new RpcNetService();
	}
	/*
	 * RPC客户端服务
	 * @return
	 */
	@Bean
	public RpcDispatcher rpcController(List<IRpcController> callbacks) {
		logger.info("注册[DefaultRpcDispatcher]服务, size:{}", callbacks.size());
		RpcDispatcher controller = new RpcDispatcher();
		try {
			controller.initialize(callbacks);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("注册[DefaultRpcDispatcher]服务失败, 异常:", e);
		}
		return controller;
	}
}

```
3.启动入口编写
```java
public class App 
{
    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main( String[] args )
    {
        long startTime = System.currentTimeMillis();
        //扫描配置环境
        AbstractApplicationContext ctx = new ClassPathXmlApplicationContext("spring-context.xml");
        ctx.registerShutdownHook();
        ctx.start();
        logger.info("Start server successful， cost time:{}ms", (System.currentTimeMillis() - startTime));
        try {
            TimeUnit.SECONDS.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```
4.在src/main/resources下的properties配置内,加入birpc相关配置

```properties
#命名规则:/框架节点/项目名字/分组类型/节点名字->data  
rpc.connection.namespace = test  
#zk远程服务地址 139.9.44.104:2181  
rpc.connection.address=139.9.44.104:2181  
#是否开启监控,暂未实现...预计以后增加监控功能  
rpc.connection.monitor = false  
#节点类型,当前节点类型  
rpc.connection.nodeType = server  
#监听节点,可以为空表示需要监听的节点类型列表,可以配置多个,如果那些节点变动,此节点做相应处理  
#如果值不为空,则从监听的节点分组内,拿可连接的节点,建立连接.  
#rpc.connection.listenNodeTypes=  
#初始连接数,如果有监听节点,此值生效,若rpc压力过大,会从已存在节点内寻找新的服务节点进行连接  
rpc.connection.initNum = 1  
#最大连接数,如果有监听节点,此值生效,若rpc压力过小,会释放空闲的连接,恢复连接至初始连接数  
rpc.connection.maxNum = 2  
#服务器信息  
#1:账号服节点,2:游戏服节点,3:战斗服节点,4:...  
rpc.connection.serverId=1  
rpc.connection.host=127.0.0.1  
rpc.connection.port=9001  
```
5.定义监听客户端身份验证
```java
@Controller
public class PlayerController implements IRpcController {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Rpc(value= ProtocolId.ReqIdentityAuthenticate, isAuth = false)
	public void reqIdentityAuthenticate(ISession session, ReqIdentityAuthenticate req) {
		
		RpcNetService netService = SpringContextHolder.getBean(RpcNetService.class);
		
		String nodeType = req.getNodeType();
		int nodeId = req.getNodeId();
		String secret =  req.getSecretKey();
		
		//TODO 做验证
		
		//FIXME 记录身份信息
		//在连接建立, 网络层缓存session, 但是session是否验证身份, 放在业务层去处理.
		//细节尽量在底层实现, 上层业务尽量不要关注底层.
		IServer server = netService.getRpcServer();
		if (server instanceof RpcServerStarter) {
			RpcServerStarter rpcServer =  (RpcServerStarter)server;
			rpcServer.addSession(nodeType, nodeId, session);
		}
		
		RespIdentityAuthenticate resp = new RespIdentityAuthenticate(); 
		resp.setCode(0);
		resp.setSeq(req.getSeq());
		session.push(resp);
		logger.info("收到RPC请求, 节点id:{}, 节点类型:{}, 返回结果:{}, 序列号:{}", req.getNodeId(), req.getNodeType(), 0, req.getSeq());
	}
}
```

6.服务端启动
```java
public class App 
{
    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main( String[] args )
    {
        long startTime = System.currentTimeMillis();
        //扫描配置环境
        AbstractApplicationContext ctx = new ClassPathXmlApplicationContext("spring-context.xml");
        ctx.registerShutdownHook();
        ctx.start();
        logger.info("Start server successful， cost time:{}ms", (System.currentTimeMillis() - startTime));
        try {
            TimeUnit.SECONDS.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

至此, 服务端代码编写完成. 启动服务端节点应用. 等待客户端连接上来.
当客户端日志输出, 表示连接成功
```java
com.cat.net.network.controller.RpcDispatcher - 默认分发处理器, 客户端连接服务:/127.0.0.1:9001
```

此时,请求的身份验证,客户端输出,请求的服务节点, 以及服务节点处理后的返回结果.
```java
INFO  com.test.client.module.InitialRunner - 目标节点类型: server, 发送验证参数:[1|client], 客户端:com.cat.net.network.client.RpcClientStarter@4ddff776
INFO  com.cat.net.network.client.RpcClientStarter - 发送RPC请求, 节点类型: server, 客户端:1
INFO  c.t.c.m.auth.rpc.ReqIdentityAuthenticateCallback - RPC响应返回=========> response.code:0
```

服务端日志
```java
INFO  c.c.n.n.controller.chain.HandlerResponseMessage - 监听协议验证为null, 拒绝处理, cmd:[10001]
INFO  com.test.server.module.auth.rpc.PlayerController - 收到RPC请求, 节点id:1, 节点类型:client, 返回结果:0, 序列号:1
```

