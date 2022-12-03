package com.rpc.core.register;

/**
 * 注册中心接口
 */
public interface IRegisterCenter<T> {

    /**
     * 初始化, 运行前的准备操作
     * @throws Exception 通用异常
     */
    void init() throws Exception;

    /**
     * 开始运行
     * @throws Exception 通用异常
     */
    void start() throws Exception;

    /**
     * 注册信息到服务器
     * @throws Exception 通用异常
     */
    void register() throws Exception;

    /**
     * 停止运行
     * @throws Exception 通用异常
     */
    void shutdown() throws Exception;

    /**
     * 销毁连接
     * @throws Exception 通用异常
     */
    void destroy() throws Exception;

    /**
     * 获取zk客户端
     * @return
     */
    T getClient();

}
