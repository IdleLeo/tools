package cn.ccg.grpc.server;

import cn.ccg.grpc.annotation.GrpcServiceScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages={"cn.ccg.redis","cn.ccg.grpc","cn.ccg.grpc.service"})
@GrpcServiceScan(packages = {"cn.ccg.grpc.service"})
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

}
