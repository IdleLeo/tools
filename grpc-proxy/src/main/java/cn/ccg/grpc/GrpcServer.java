package cn.ccg.grpc;

import cn.ccg.core.util.BeanUtils;
import cn.ccg.grpc.config.GrpcProperties;
import cn.ccg.grpc.config.RemoteServer;
import cn.ccg.grpc.service.CommonService;
import cn.ccg.grpc.util.Common;
import cn.ccg.grpc.util.GrpcSpringUtil;
import cn.ccg.redis.service.RedisGrpcService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcServer implements DisposableBean {

    public static final int DEFAULT_FLOW_CONTROL_WINDOW = 1048576;
    private final GrpcProperties grpcProperties;
    private final CommonService commonService;
    private int windowFlowControl = 10;
    private Server server;
    private final RemoteServer remoteServer = new RemoteServer();
    private ServerInterceptor serverInterceptor;
    private final RedisGrpcService redisStringService = GrpcSpringUtil.getBean(RedisGrpcService.class);

    public GrpcServer(GrpcProperties grpcProperties, CommonService commonService) {
        this.grpcProperties = grpcProperties;
        this.commonService = commonService;
        this.windowFlowControl = (grpcProperties.getFlowCtl() <= 0 ? windowFlowControl : grpcProperties.getFlowCtl());
    }

    public GrpcServer(GrpcProperties grpcProperties, CommonService commonService, ServerInterceptor serverInterceptor) {
        this.grpcProperties = grpcProperties;
        this.commonService = commonService;
        this.serverInterceptor = serverInterceptor;
        this.windowFlowControl = (grpcProperties.getFlowCtl() <= 0 ? windowFlowControl : grpcProperties.getFlowCtl());
    }

    /**
     * 启动服务
     *
     * @throws Exception 异常
     */
    public void start() throws Exception {
        int port = grpcProperties.getPort();
        log.info("============================ window flow control==={}", DEFAULT_FLOW_CONTROL_WINDOW * windowFlowControl);
        if (serverInterceptor != null) {
//            server = ServerBuilder.forPort(port).addService(ServerInterceptors.intercept(commonService, serverInterceptor)).build().start();
            server = NettyServerBuilder.forPort(port).maxMessageSize(GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE * windowFlowControl)
                    .flowControlWindow(DEFAULT_FLOW_CONTROL_WINDOW * 10).addService(ServerInterceptors.intercept(commonService, serverInterceptor))
                    .build().start();
        } else {
            Class<?> clazz = grpcProperties.getServerInterceptor();
            if (clazz == null) {
//                server = ServerBuilder.forPort(port).addService(commonService).build().start();
                server = NettyServerBuilder.forPort(port).maxMessageSize(GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE * windowFlowControl)
                        .flowControlWindow(DEFAULT_FLOW_CONTROL_WINDOW * 10).addService(commonService).build().start();
            } else {
//                server = ServerBuilder.forPort(port).addService(ServerInterceptors.intercept(commonService, (ServerInterceptor) clazz.newInstance())).build().start();
                server = NettyServerBuilder.forPort(port).maxMessageSize(GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE * windowFlowControl)
                        .flowControlWindow(DEFAULT_FLOW_CONTROL_WINDOW * 10).addService(ServerInterceptors.intercept(commonService, (ServerInterceptor) clazz.newInstance())).build().start();
            }
        }
        log.info("gRPC Server started, listening on port {}", server.getPort());
        try {
            String ip = grpcProperties.getHost();
            if (StringUtils.isEmpty(ip)) {
                //自动获取ip
                InetAddress ia = InetAddress.getLocalHost();
                grpcProperties.setHost(ia.getHostAddress());
            }
            BeanUtils.beanFieldCopy(grpcProperties, remoteServer);
        } catch (Exception e) {
            log.error("");
        }
        startDaemonAwaitThread();
        if (grpcProperties.isClientFlush()) {
            startExamineConnThread(grpcProperties.getDaemonThreadTimeout());
        }
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread(() -> {
            try {
                GrpcServer.this.server.awaitTermination();
            } catch (InterruptedException e) {
                log.warn("gRPC server stopped.{}", e.getMessage());
            }
        });
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    private void startExamineConnThread(long deamonThreadTimeout) {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(() -> {
            try {
                //心跳 发送此app 到redis
                //增加remote信息 appid 与remote信息 一一对应
                redisStringService.add(Common.DEFAULT_REDIS_GRPC_APP_KEY + remoteServer.getAppId(), remoteServer, deamonThreadTimeout + 5);
                //增加appid appid与groupid 多对1
                redisStringService.put(Common.DEFAULT_REDIS_GRPC_GROUP_KEY + remoteServer.getServer() + "_" + remoteServer.getGroupId(), remoteServer.getAppId(), remoteServer.getAppId());
                //addStr2RedisList(Common.DEFAULT_REDIS_SERVICE_PRE_KEY, remoteServer.getGroupId(), remoteServer.getAppId());
                //增加groupid groupid与funcname 多对1
                redisStringService.put(Common.DEFAULT_REDIS_GRPC_SERVICE_KEY + remoteServer.getServer(), remoteServer.getGroupId(), remoteServer.getGroupId());
//                addStr2RedisList(Common.DEFAULT_REDIS_SERVICE_REGISTRY_KEY, remoteServer.getServer(), remoteServer.getGroupId());
            } catch (Exception e) {
                log.error("{startExamineConnThread} has exception,", e);
            }

        }, 10, deamonThreadTimeout, TimeUnit.SECONDS);
    }

    /**
     * 销毁
     */
    @Override
    public void destroy() throws Exception {
        Optional.ofNullable(server).ifPresent(Server::shutdown);
        log.info("gRPC server stopped.");
    }
}
