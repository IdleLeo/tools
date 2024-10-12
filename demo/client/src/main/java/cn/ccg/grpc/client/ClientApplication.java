package cn.ccg.grpc.client;

import cn.ccg.grpc.annotation.GrpcServiceScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages={"cn.ccg.redis","cn.ccg.grpc"})
@GrpcServiceScan(packages = {"cn.ccg.grpc.service"})
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

}
