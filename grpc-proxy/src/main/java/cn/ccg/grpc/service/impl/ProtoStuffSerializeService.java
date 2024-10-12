package cn.ccg.grpc.service.impl;

import cn.ccg.grpc.service.GrpcRequest;
import cn.ccg.grpc.service.GrpcResponse;
import cn.ccg.grpc.service.SerializeService;
import cn.ccg.grpc.util.ProtobufUtils;
import com.google.protobuf.ByteString;
import com.ccg.rpc.CommonServiceProto;

/**
 * ProtoStuff 序列化/反序列化工具
 */
public class ProtoStuffSerializeService implements SerializeService {
    @Override
    public GrpcRequest deserialize(CommonServiceProto.Request request) {
        return ProtobufUtils.deserialize(request.getRequest().toByteArray(), GrpcRequest.class);
    }

    @Override
    public GrpcResponse deserialize(CommonServiceProto.Response response) {
        return ProtobufUtils.deserialize(response.getResponse().toByteArray(), GrpcResponse.class);
    }

    @Override
    public ByteString serialize(GrpcResponse response) {
        return ByteString.copyFrom(ProtobufUtils.serialize(response));
    }

    @Override
    public ByteString serialize(GrpcRequest request) {
        return ByteString.copyFrom(ProtobufUtils.serialize(request));
    }
}
