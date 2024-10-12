package cn.ccg.grpc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "spring.grpc")
public class GrpcProperties {
    private boolean enable;

    private int port;

    private int flowCtl = 10;

    private List<String> remoteServers;

    private Class<?> clientInterceptor;

    private Class<?> serverInterceptor;

    public final static long REQUEST_TIMEOUT = 7L;

    private long timeout = 7L;

    private long daemonThreadTimeout = 10L;

    private String host;

    private String server;

    private String groupId = "default";

    private String appId;

    private boolean clientFlush = true;

    private boolean serviceFlush = true;
}
