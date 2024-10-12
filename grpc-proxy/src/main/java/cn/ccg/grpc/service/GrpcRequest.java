package cn.ccg.grpc.service;

import lombok.Data;

import java.io.Serializable;

@Data
public class GrpcRequest implements Serializable {
    /**
     * 接口
     */
    private String clazz;

    /**
     * 方法
     */
    private String method;

    /**
     * service 方法参数
     */
    private Object[] args;

    /**
     * 对应参数的类型
     */
    private Class<?>[] argTypes;

    private long timeOut = 7;
}
