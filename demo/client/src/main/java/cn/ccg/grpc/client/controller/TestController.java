package cn.ccg.grpc.client.controller;

import cn.ccg.grpc.service.TestService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping
public class TestController {

    @Resource
    TestService testService;

    @RequestMapping("test")
    public String test() {
        return testService.sayHello("test");

    }
}
