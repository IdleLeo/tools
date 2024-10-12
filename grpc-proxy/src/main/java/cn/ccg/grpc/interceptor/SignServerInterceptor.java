package cn.ccg.grpc.interceptor;

import cn.ccg.grpc.util.SignUtil;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignServerInterceptor implements ServerInterceptor {


    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        if(!headers.containsKey(SignUtil.SIGN_HEADER_KEY)||!headers.containsKey(SignUtil.TIMESTAMP_HEADER_KEY)){
             return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                @Override
                public void sendHeaders(Metadata responseHeaders) {
                    super.sendHeaders(responseHeaders);
                }
            }, headers);
        }
        String key = headers.get(SignUtil.SIGN_HEADER_KEY);
        String timestatmp = headers.get(SignUtil.TIMESTAMP_HEADER_KEY);
        System.out.println(key);
        if (key.equals(SignUtil.getSignValue(call.getMethodDescriptor().getFullMethodName(),timestatmp))){
            log.debug("header received from client:" + headers);
            return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                @Override
                public void sendHeaders(Metadata responseHeaders) {
                    // @2 响应客户端设置服务端Header信息
                    //  responseHeaders.put(CUSTOM_HEADER_KEY, "customRespondValue");
                    super.sendHeaders(responseHeaders);
                }
            }, headers);
        }
      return null;
    }
}
