package cn.ccg.grpc.service;

import cn.ccg.grpc.annotation.GrpcService;

//@GrpcService
public interface TestService {

    String sayHello(String name);
}
