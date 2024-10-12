package cn.ccg.grpc.service.impl;


import cn.ccg.grpc.service.TestService;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService {
    @Override
    public String sayHello(String name) {
        System.out.println(name + " send a message");
        return "name is " + name;
    }
}
