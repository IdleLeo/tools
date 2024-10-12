package cn.ccg.grpc.bind;

import cn.ccg.grpc.GrpcClient;
import cn.ccg.grpc.ServerContext;
import cn.ccg.grpc.annotation.GrpcGroup;
import cn.ccg.grpc.annotation.GrpcService;
import cn.ccg.grpc.annotation.GrpcTimeOut;
import cn.ccg.grpc.constant.GrpcResponseStatus;
import cn.ccg.grpc.exception.GrpcException;
import cn.ccg.grpc.service.GrpcRequest;
import cn.ccg.grpc.service.GrpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;
import java.util.HashMap;

@Slf4j
public class GrpcServiceProxy<T> implements InvocationHandler {

    private final Class<T> grpcService;
    private final Object invoke;

    public GrpcServiceProxy(Class<T> target, Object invoker) {
        this.grpcService = target;
        this.invoke = invoker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        String className = grpcService.getName();
        if ("toString".equals(methodName) && args.length == 0) {
            return className + "@" + invoke.hashCode();
        } else if ("hashCode".equals(methodName) && args.length == 0) {
            return invoke.hashCode();
        } else if ("equals".equals(methodName) && args.length == 1) {
            Object another = args[0];
            return proxy == another;
        }

//        Class<?> target = Class.forName(className);
//        Method invokeMethod = target.getMethod(methodName, method.getParameterTypes());
        long timeOut = 7;
        if (method.isAnnotationPresent(GrpcTimeOut.class)) {
            GrpcTimeOut timeOutConfig = method.getAnnotation(GrpcTimeOut.class);
            timeOut = timeOutConfig.timeOut();
        }
        GrpcRequest request = new GrpcRequest();
        request.setClazz(className);
        request.setMethod(methodName);
        //protobuf是按照可以序列化的字段顺序来的，所以中间有null值的会影响赋值顺序。比如。"1",null,"2", 反序列化出来是。"1","2",null。所以把null转变为"";
        request.setArgs(args);
        request.setTimeOut(timeOut);
        request.setArgTypes(method.getParameterTypes());

        GrpcResponse response;

        //查询是否存在GrpcGroup注解，如果存在，获取groupid
        String groupId = "default";
        if (method.isAnnotationPresent(GrpcGroup.class)) {
            groupId = args[method.getAnnotation(GrpcGroup.class).groupIdIndex()].toString();
        }
//todo: dbc node
//        GrpcDbcNode dbcNode = method.getAnnotation(GrpcDbcNode.class);
//        //   for (Annotation annotation : method.getAnnotations()) {
//        if (dbcNode != null) {
//            //GrpcDbcNode dbcNode = (GrpcDbcNode) annotation;
//            response = processDynamicNodeRequest(request, NodeType.DEFAULT.getByValue(dbcNode.nodeType()), args[dbcNode.valueIdx()].toString(), dbcNode.nodeName());
//        }else{
        GrpcService annotation = grpcService.getAnnotation(GrpcService.class);
        String server = annotation.server();
        response = getResponse(server, groupId, request, annotation.isCheck(), new HashMap<>());
//      }
        if (response == null) {
            throw new GrpcException(GrpcResponseStatus.ERROR.getCode(), this.getClass().getName() + ": response is null");
        }
        if (GrpcResponseStatus.SUCCESS.getCode() != response.getStatus()) {
            throw response.getException();
        }
        return response.getResult();
    }

    private GrpcResponse getResponse(String server, String groupId, GrpcRequest request, boolean isCheck, HashMap<String, String> failServerMap) {
        ServerContext serverContext;
        try {
            serverContext = getServerContext(server, groupId, isCheck, failServerMap);
        }catch (Exception e){
            log.error("{grpc client} has exception.", e);
            return null;
        }
        try {
            return serverContext.handle(request);
        }catch (Exception e){
            //标记错误的服务地址，此节点
            failServerMap.put(serverContext.getGrpcProperties().getAppId(), "1");
            GrpcResponse response = getResponse(server, groupId, request, isCheck, failServerMap);
            if (response != null) {
                return response;
            }
            throw e;
        }
    }

    private ServerContext getServerContext(String server, String groupId, boolean isCheck, HashMap<String, String> failServerMap) {
        return GrpcClient.connect(server, groupId, isCheck, failServerMap);
    }

}
