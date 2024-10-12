package cn.ccg.grpc.config;

import cn.ccg.grpc.GrpcClient;
import cn.ccg.grpc.GrpcServer;
import cn.ccg.grpc.annotation.GrpcService;
import cn.ccg.grpc.annotation.GrpcServiceScan;
import cn.ccg.grpc.service.SerializeService;
import cn.ccg.grpc.service.CommonService;
import cn.ccg.grpc.service.impl.ProtoStuffSerializeService;
import cn.ccg.grpc.util.ProxyUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 *
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(GrpcProperties.class)
public class GrpcAutoConfiguration {

    @Getter
    private static AbstractApplicationContext applicationContext;

    private final GrpcProperties grpcProperties;

    private final static Map<String, Boolean> functions = new HashMap<>();

    public GrpcAutoConfiguration(AbstractApplicationContext applicationContext, GrpcProperties grpcProperties) {
        GrpcAutoConfiguration.applicationContext = applicationContext;
        this.grpcProperties = grpcProperties;
    }

    /**
     * 全局 序列化/反序列化
     */
    @Bean
    @ConditionalOnMissingBean(SerializeService.class)
    public SerializeService serializeService() {
        return new ProtoStuffSerializeService();
    }

    /**
     * RPC 服务调用
     */
    @Bean
    @ConditionalOnMissingBean(CommonService.class)
    public CommonService commonService(SerializeService serializeService) {
        return new CommonService(applicationContext, serializeService);
    }

    /**
     * RPC 服务端
     */
    @Bean
    @ConditionalOnMissingBean(GrpcServer.class)
    @ConditionalOnProperty(value = "spring.grpc.enable", havingValue = "true")
    public GrpcServer grpcService(CommonService commonService) throws Exception {
        GrpcServer server = new GrpcServer(grpcProperties, commonService);
        server.start();
        return server;
    }

    /**
     * RPC 客户端
     */
    @Bean
    @ConditionalOnMissingBean(GrpcClient.class)
    @ConditionalOnProperty(value = "spring.grpc.client.enable", havingValue = "true")
    public GrpcClient grpcClient(SerializeService serializeService) {
        Map<String, Boolean> ignore = new HashMap<>();
        if (CollectionUtils.isEmpty(grpcProperties.getRemoteServers())) {
            grpcProperties.setRemoteServers(new ArrayList<>());
        }
        for (Map.Entry<String, Boolean> function : functions.entrySet()) {
            if (!grpcProperties.getRemoteServers().contains(function.getKey())) {
                grpcProperties.getRemoteServers().add(function.getKey());
            }
            if (!function.getValue()) {
                ignore.put(function.getKey(), function.getValue());
            }
        }
        GrpcClient client = new GrpcClient(grpcProperties, ignore, serializeService);
        client.init();
        return client;
    }

    /**
     * 手动扫描 @GrpcService 注解的接口，生成动态代理类，注入到 Spring 容器
     */
    public static class ExternalGrpcServiceScannerRegistrar implements BeanFactoryAware, ImportBeanDefinitionRegistrar, ResourceLoaderAware {

        private BeanFactory beanFactory;
        private ResourceLoader resourceLoader;

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }

        @Override
        public void setResourceLoader(ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
            ClassPathBeanDefinitionScanner scanner = new ClassPathGrpcServiceScanner(beanDefinitionRegistry);
            scanner.setResourceLoader(this.resourceLoader);
            scanner.addIncludeFilter(new AnnotationTypeFilter(GrpcService.class));
            Set<BeanDefinition> beanDefinitionSet =
                    scanPackages(annotationMetadata, scanner);
            ProxyUtils.registBeans(beanFactory, beanDefinitionSet);
        }

        private Set<BeanDefinition> scanPackages(AnnotationMetadata annotationMetadata, ClassPathBeanDefinitionScanner scanner) {
            Set<BeanDefinition> beanDefinitions = new HashSet<>();
            Map<String, Object> annotationAttributes = annotationMetadata.getAnnotationAttributes(GrpcServiceScan.class.getCanonicalName());
            if (annotationAttributes == null) {
                return beanDefinitions;
            }
            String[] basePackages = (String[]) annotationAttributes.get("packages");
            if (basePackages.length == 0) {
                return beanDefinitions;
            }
            List<String> packages = new ArrayList<>(Arrays.asList(basePackages));
            if (CollectionUtils.isEmpty(packages)) {
                return beanDefinitions;
            }
            packages.forEach(pack -> {
                Set<BeanDefinition> beanDefinitionSet = scanner.findCandidateComponents(pack);
                beanDefinitions.addAll(beanDefinitionSet);
                beanDefinitionSet.forEach(bean -> {
                    Class<?> cls;
                    try {
                        cls = Class.forName(bean.getBeanClassName());
                        GrpcService grpcService = cls.getDeclaredAnnotation(GrpcService.class);
                        String serverName = grpcService.server();
                        if (!StringUtils.isEmpty(serverName)) {
                            if (!functions.containsKey(serverName) || !functions.get(serverName)) {
                                functions.put(serverName, grpcService.isCheck());
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        log.warn("scan grpc packages warn", e);
                    }
                });
            });
            return beanDefinitions;
        }
    }
    protected static class ClassPathGrpcServiceScanner extends ClassPathBeanDefinitionScanner {

        ClassPathGrpcServiceScanner(BeanDefinitionRegistry registry) {
            super(registry, false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
        }

    }
}
