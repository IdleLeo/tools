package cn.ccg.grpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.ccg.grpc.config.GrpcAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({GrpcAutoConfiguration.ExternalGrpcServiceScannerRegistrar.class})
public @interface GrpcServiceScan {
    String[] packages() default {};
}
