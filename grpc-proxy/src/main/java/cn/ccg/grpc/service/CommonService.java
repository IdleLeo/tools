package cn.ccg.grpc.service;

import com.alibaba.fastjson.JSONArray;
import com.ccg.rpc.CommonServiceGrpc;
import com.ccg.rpc.CommonServiceProto;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import net.devh.springboot.autoconfigure.grpc.server.GrpcService;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.context.support.AbstractApplicationContext;
import io.grpc.stub.StreamObserver;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@GrpcService(CommonServiceGrpc.class)
public class CommonService extends CommonServiceGrpc.CommonServiceImplBase {

    private final Map<Class<?>, Object> serviceBeanMap = new ConcurrentHashMap<>();

    private final AbstractApplicationContext applicationContext;

    private final SerializeService defaultSerializationService;

    public CommonService(AbstractApplicationContext applicationContext, SerializeService serializeService) {
        this.applicationContext = applicationContext;
        this.defaultSerializationService = serializeService;
    }

    @Override
    public void handle(CommonServiceProto.Request request, StreamObserver<CommonServiceProto.Response> responseObserver) {
        GrpcRequest grpcRequest = defaultSerializationService.deserialize(request);
        GrpcResponse response = new GrpcResponse();
        FastClass serviceFastClass;
        Method matchingMethod = null;
        String className = grpcRequest.getClazz();
        try {
            Object bean = getBean(Class.forName(className));
            Object[] args = grpcRequest.getArgs();
            Class<?>[] argsTypes = grpcRequest.getArgTypes();
            matchingMethod = MethodUtils.getMatchingMethod(Class.forName(className), grpcRequest.getMethod(), argsTypes);
            if (matchingMethod == null) {
                log.error("class={},Fail to invoke method={}, argsTypes={}", className, grpcRequest.getMethod(), JSONArray.toJSONString(argsTypes));
                response.error("method=" + grpcRequest.getMethod() + " not found", new Exception("method not found Exception"),
                        new StackTraceElement[]{new StackTraceElement(className, grpcRequest.getMethod(), "grpc commonService", 49)});
                responseObserver.onCompleted();
                return;
            }
            serviceFastClass = FastClass.create(bean.getClass());
            FastMethod serviceFastMethod = serviceFastClass.getMethod(matchingMethod);
            Object result = serviceFastMethod.invoke(bean, args);
            response.success(result);
        } catch (Exception exception) {
            String message = exception.getClass().getName() + ": " + exception.getMessage();
            log.error("Fail to invoke method={}", matchingMethod, exception);
            response.error(message, exception, new StackTraceElement[]{new StackTraceElement(className, grpcRequest.getMethod(), "grpc commonService", 49)});
        }
        ByteString bytes = defaultSerializationService.serialize(response);
        CommonServiceProto.Response grpcResponse = CommonServiceProto.Response.newBuilder().setResponse(bytes).build();
        responseObserver.onNext(grpcResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void heartBeat(CommonServiceProto.Dumy request, StreamObserver<CommonServiceProto.Dumy> responseObserver) {
        CommonServiceProto.Dumy grpcResponse = CommonServiceProto.Dumy.newBuilder().build();
        responseObserver.onNext(grpcResponse);
        responseObserver.onCompleted();
    }

    /**
     * 获取 Service Bean
     */
    private Object getBean(Class<?> clazz) throws NoSuchBeanDefinitionException {
        if (serviceBeanMap.containsKey(clazz)) {
            return serviceBeanMap.get(clazz);
        }
        try {
            Object bean = applicationContext.getBean(clazz);
            serviceBeanMap.put(clazz, bean);
            return bean;
        } catch (BeansException e) {
            throw new NoSuchBeanDefinitionException(clazz);
        }
    }
}
