package cn.ccg.grpc.util;

import io.grpc.Metadata;

public class SignUtil {

    public static final Metadata.Key<String> SIGN_HEADER_KEY =
            Metadata.Key.of("sign_header_key", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> TIMESTAMP_HEADER_KEY =
            Metadata.Key.of("timestatmp_header_key", Metadata.ASCII_STRING_MARSHALLER);

    private static final String SIGN_VALUE = "FTIMAGE";

    public static final String getSignValue(String method, String timestatmp) {
        return MD5Encodes.MD5(SIGN_VALUE + method + timestatmp);
    }
}
