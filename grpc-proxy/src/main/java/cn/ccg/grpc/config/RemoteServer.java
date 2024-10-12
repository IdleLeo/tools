package cn.ccg.grpc.config;

import lombok.Data;

/**
 * 远程服务
 */
@Data
public class RemoteServer {

    /**
     * 服务名
     */
    private String server;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 服务端口号
     */
    private int port;

    /**
     * 组号
     */
    private String groupId;

    /**
     * 服务号
     */
    private String appId;

}
