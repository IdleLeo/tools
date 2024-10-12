package cn.ccg.grpc.service;
import com.ccg.rpc.CommonServiceProto;
import com.google.protobuf.ByteString;

public interface SerializeService {
    /**
     * 序列化
     */
    ByteString serialize(GrpcResponse response);

    /**
     * 序列化
     */
    ByteString serialize(GrpcRequest request);

    /**
     * 反序列化
     */
    GrpcRequest deserialize(CommonServiceProto.Request request);

    /**
     * 反序列化
     */
    GrpcResponse deserialize(CommonServiceProto.Response response);
}
