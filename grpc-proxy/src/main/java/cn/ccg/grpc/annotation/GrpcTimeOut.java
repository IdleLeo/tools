package cn.ccg.grpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcTimeOut {
    /**
     * grpc-client 连接的超时时间
     */
    long timeOut() default 7;
}
