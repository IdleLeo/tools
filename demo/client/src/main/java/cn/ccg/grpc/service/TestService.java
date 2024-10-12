package cn.ccg.grpc.service;

import cn.ccg.grpc.annotation.GrpcService;

@GrpcService(server = "test")
public interface TestService {

    String sayHello(String name);
}
