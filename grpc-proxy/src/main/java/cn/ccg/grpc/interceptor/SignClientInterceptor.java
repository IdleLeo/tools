package cn.ccg.grpc.interceptor;

import cn.ccg.grpc.util.SignUtil;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        log.debug("call received to server:{}", callOptions.toString());
        log.debug("header received to server:{}", method.toString());
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // @1 在Header中设置需要透传的值
                String timestatmp = String.valueOf(System.currentTimeMillis());
                headers.put(SignUtil.SIGN_HEADER_KEY, SignUtil.getSignValue(method.getFullMethodName(), timestatmp));
                headers.put(SignUtil.TIMESTAMP_HEADER_KEY, timestatmp);
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onHeaders(Metadata headers) {
                        // @2 获取从服务端返回的Header信息
                        log.debug("header received from server:{}", headers);
                        super.onHeaders(headers);
                    }
                }, headers);
            }
        };
    }
}
