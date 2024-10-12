package cn.ccg.grpc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 */
@Documented
@Inherited
@Retention(RUNTIME)
public @interface GrpcService {
    /**
     * 远程服务名
     */
    String server() default "";
    boolean isCheck() default false;
}
