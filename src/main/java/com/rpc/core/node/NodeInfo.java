package com.rpc.core.node;

import com.alibaba.fastjson.annotation.JSONField;
import com.rpc.common.CommonConstant;

/**
 * 节点信息, 从zk中获取到信息后, 封装成此对象
 * @author Jeremy
 */
public class NodeInfo {

	/**
	 * 服务器节点ID
	 */
	private int id;

	/**
	 * 服务器节点IP地址
	 */
	private String ip;

	/**
	 * 服务器节点端口
	 */
	private int port;

	/**
	 * 服务器节点类型
	 */
	private String type;

	/**
	 * 项目名字
	 */
	private String projectName;

	/**
	 * 节点名字,由配置生成, 所有节点唯一
	 */
	private String name;

	/**
	 * 本服务节点目前连接的客户端数量<br>
	 *  定期更新此连接数, 用于客户端做连接均衡<>
	 */
	private int connectNum;

	public NodeInfo(){}

	public NodeInfo(int id, String ip, int port, String type, String projectName) {
		super();
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.type = type;
		this.projectName = projectName;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public int getConnectNum() {
		return connectNum;
	}

	public void setConnectNum(int connectNum) {
		this.connectNum = connectNum;
	}

	/**
	 * 获取节点名字<br>
	 * 组成规则: "/"+projectName+"/"+type+"/"+type+"_"+id<br>
	 * 第一个type表示分组, 第二个type与id组成唯一名字<br>
	 * eg:/fatiny/game/game_1
	 * @return
	 */
	@JSONField(serialize = false)
	public String getName(){
		if (name == null || name.equals("")){
			String temp = CommonConstant.SLASH.concat(projectName);
			temp = CommonConstant.SLASH.concat(type);
			this.name = temp.concat(temp).concat(CommonConstant.UNDERLINE).concat(String.valueOf(id));
		}
		return name;
	}

	public static NodeInfo create(int id, String ip, int port, String type, String projectName) {
		return new NodeInfo(id, ip, port, type, projectName);
	}

	@Override
	public String toString() {
		return "NodeInfo{" +
				"id=" + id +
				", ip='" + ip + '\'' +
				", port=" + port +
				", type='" + type + '\'' +
				'}';
	}
}
