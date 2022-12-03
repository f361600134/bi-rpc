package com.rpc.core.node;

/**
 * 统计信息
 */
public class RecordInfo {
    /**
     * 处理请求次数
     */
    private int requestNum;
    /**
     * 处理响应次数
     */
    private int responseNum;
    /**
     * 成功次数, 接收请求->响应返回. 视为成功
     */
    private int successNum;
    /**
     * 失败次数, 接受请求->响应返回处理失败, 视为失败
     */
    private int filedNum;
    /**
     * 错误次数, 处理过程中发生异常, 视为异常次数
     */
    private int errorNum;
}
