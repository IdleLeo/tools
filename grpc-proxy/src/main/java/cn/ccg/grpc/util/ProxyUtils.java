package cn.ccg.grpc.util;

import cn.ccg.grpc.annotation.GrpcService;
import cn.ccg.grpc.bind.GrpcServiceProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.util.StringUtils;

import java.util.Set;

@Slf4j
public class ProxyUtils {

    public static void registBeans(BeanFactory beanFactory, Set<BeanDefinition> beanDefinitions) {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            String className = beanDefinition.getBeanClassName();
            if (!StringUtils.hasText(className)) {
                continue;
            }
            try {
                //创建代理类
                Class<?> target = Class.forName(className);
                Object invoker = new Object();
                InvocationHandler invocationHandler=new GrpcServiceProxy<>(target,invoker);
                Object proxy= Proxy.newProxyInstance(GrpcService.class.getClassLoader(),new Class[]{target},invocationHandler);
                //注册到spring容器
                String beanName = ClassNameUtils.beanName(className);
                ((DefaultListableBeanFactory) beanFactory).registerSingleton(beanName, proxy);
            } catch (ClassNotFoundException e) {
                log.warn("class not found : {}", className);
            }
        }
    }
}
