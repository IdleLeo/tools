package cn.ccg.grpc;

import cn.ccg.grpc.config.GrpcProperties;
import cn.ccg.grpc.service.GrpcRequest;
import cn.ccg.grpc.service.GrpcResponse;
import cn.ccg.grpc.service.SerializeService;
import com.alibaba.fastjson.JSONObject;
import com.ccg.rpc.CommonServiceGrpc;
import com.ccg.rpc.CommonServiceProto;
import com.google.protobuf.ByteString;
import io.grpc.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class ServerContext {

    @Getter
    private final Channel channel;

    private final SerializeService defaultSerializeService;

    @Getter
    private final GrpcProperties grpcProperties;

    public ServerContext(Channel channel, SerializeService serializeService, GrpcProperties grpcProperties) {
        this.channel = channel;
        this.defaultSerializeService = serializeService;
        this.grpcProperties = grpcProperties;
    }

    /**
     * 处理 gRPC 请求
     */
    public GrpcResponse handle(GrpcRequest grpcRequest) {
        ByteString bytes = defaultSerializeService.serialize(grpcRequest);
        CommonServiceProto.Request request = CommonServiceProto.Request.newBuilder().setRequest(bytes).build();
        CommonServiceProto.Response response;
        try {
            grpcRequest.getClazz();
            CommonServiceGrpc.CommonServiceBlockingStub blockingStub = CommonServiceGrpc.newBlockingStub(channel).withDeadlineAfter(grpcRequest.getTimeOut(), TimeUnit.SECONDS);
            response = blockingStub.handle(request);
        } catch (Exception e) {
            throw new RuntimeException("GRPC handle error, re-handle: " + grpcRequest.getMethod() + ", argsTypes=" + JSONObject.toJSONString(grpcRequest.getArgTypes()) + ",root-cause=" + e.getMessage());
        }
        return defaultSerializeService.deserialize(response);
    }

    /**
     * 处理 heartBeat 请求
     */
    public void heartBeat() {
        CommonServiceProto.Dumy request = CommonServiceProto.Dumy.newBuilder().build();
        try {
            long timeout = grpcProperties.getTimeout() + 5;
            CommonServiceGrpc.CommonServiceBlockingStub block = CommonServiceGrpc.newBlockingStub(channel).withDeadlineAfter(timeout, TimeUnit.SECONDS);
            block.heartBeat(request);
        } catch (Exception e) {
            throw new RuntimeException("GRPC handle error, re-handle: heartBeat,root-cause=" + e.getMessage());
        }
    }
}
