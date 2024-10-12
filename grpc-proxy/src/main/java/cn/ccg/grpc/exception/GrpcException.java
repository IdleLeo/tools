package cn.ccg.grpc.exception;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class GrpcException extends RuntimeException implements Serializable {
    public GrpcException(Object code, String s) {
    }
    private int errorCode = -1;

    public GrpcException(int errorCode, String message){
        super(message);
        this.errorCode = errorCode;
    }

    public GrpcException(String message){
        super(message);
    }

}
