package cn.ccg.grpc.constant;

import lombok.Getter;


@Getter
public enum GrpcResponseStatus {

    SUCCESS(0), ERROR(-1);

    private final int code;

    GrpcResponseStatus(int code) {
        this.code = code;
    }

}
