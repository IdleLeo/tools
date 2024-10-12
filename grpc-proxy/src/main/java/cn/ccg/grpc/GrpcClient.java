package cn.ccg.grpc;

import cn.ccg.core.util.BeanUtils;
import cn.ccg.grpc.config.GrpcProperties;
import cn.ccg.grpc.config.RemoteServer;
import cn.ccg.grpc.interceptor.SignClientInterceptor;
import cn.ccg.grpc.selector.AbstractLoadBalance;
import cn.ccg.grpc.selector.CcgInterceptChannel;
import cn.ccg.grpc.selector.CcgRoundRobinLoadBalance;
import cn.ccg.grpc.service.SerializeService;
import cn.ccg.grpc.util.Common;
import cn.ccg.grpc.util.GrpcSpringUtil;
import cn.ccg.redis.service.RedisGrpcService;
import io.grpc.*;
import io.grpc.internal.GrpcUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcClient {

    private final GrpcProperties grpcProperties;

    private final SerializeService serializeService;

    private ClientInterceptor clientInterceptor;

    private static final Map<String, Map<String, AbstractLoadBalance>> serverMap = new ConcurrentHashMap<>();

    private RedisGrpcService redisStringService = GrpcSpringUtil.getBean(RedisGrpcService.class);

    Map<String, Boolean> checkIgnoreServices;

//    private final int DEFAULT_FRAME_SIZE = 1024 * 1024 * 4;

    public GrpcClient(GrpcProperties grpcProperties, Map<String, Boolean> ignore, SerializeService serializeService) {
        this.grpcProperties = grpcProperties;
        this.serializeService = serializeService;
        this.checkIgnoreServices = ignore;
    }

    public GrpcClient(GrpcProperties grpcProperties, SerializeService serializeService, ClientInterceptor clientInterceptor) {
        this.grpcProperties = grpcProperties;
        this.serializeService = serializeService;
        this.clientInterceptor = clientInterceptor;
    }

    /**
     * 连接远程服务
     */
    public static ServerContext connect(String serverName, String groupId, boolean isCheck, HashMap<String, String> failServerMap) {
        if (!serverMap.containsKey(serverName)) {
            throw new RuntimeException("GRPC handle error, re-handle: connect, serverName=" + serverName + ", groupId=" + groupId + ",rootcause=no connect of this server name.");
        }
        Map<String, AbstractLoadBalance> abstractLoadBalanceMap = serverMap.get(serverName);
        if (!groupId.equalsIgnoreCase("default") && !abstractLoadBalanceMap.containsKey(groupId)) {
            throw new RuntimeException("GRPC handle error, re-handle: connect, serverName=" + serverName + ", groupId=" + groupId + ",rootcause=no connect of this group id.");
        }
        AbstractLoadBalance abstractLoadBalance;
        if (!groupId.equalsIgnoreCase("default") || abstractLoadBalanceMap.containsKey(groupId)) {
            abstractLoadBalance = abstractLoadBalanceMap.get(groupId);
        } else {
            abstractLoadBalance = new CcgRoundRobinLoadBalance(new ArrayList<>());
            for (Map.Entry<String, AbstractLoadBalance> entry : abstractLoadBalanceMap.entrySet()) {
                abstractLoadBalance.getInvokers().addAll(entry.getValue().getInvokers());
            }
            abstractLoadBalanceMap.put("default", abstractLoadBalance);
        }

        while (!abstractLoadBalance.getInvokers().isEmpty()) {
            ServerContext serverContext = abstractLoadBalance.selectServerContext();
            if (serverContext == null || serverContext.getChannel() == null) {
                abstractLoadBalance.removeInvoker(serverContext);
                continue;
            }

            //如果當前接口请求失败，忽略次节点，否则因为heartbeat上报，导致可能又拿到错误的节点
            if (failServerMap.containsKey(serverContext.getGrpcProperties().getAppId())) {
                serverContext = null;
                //查看是否还有可用的，可能当前选中的就是失败的这个，没有则抛出异常，这次请求失败
                for (ServerContext backupServerContext : abstractLoadBalance.getInvokers()) {
                    if (!failServerMap.containsKey(backupServerContext.getGrpcProperties().getAppId())) {
                        serverContext = backupServerContext;
                    }
                }
            }

            if (serverContext == null) {
                throw new RuntimeException("GRPC handle error, re-handle: connect, serverName=" + serverName + ", groupId=" + groupId + ",rootcause=no serverContext or channel of this group id.");
            }
            Channel channel = serverContext.getChannel();
            ManagedChannel managedChannel = null;
            if (channel instanceof ManagedChannel) {
                managedChannel = (ManagedChannel) channel;
            } else {
                managedChannel = (ManagedChannel) ((CcgInterceptChannel.InterceptorChannel) channel).getChannel();
            }


            if (managedChannel.getState(false).equals(ConnectivityState.IDLE)) {
                try {
                    if (isCheck) {
                        serverContext.heartBeat();
                    }
                } catch (Exception e) {
                    log.error("serverName={}, groupId={},heartBeat has exception connect.", serverName, groupId, e);
                    managedChannel.shutdown();
                    abstractLoadBalance.removeInvoker(serverContext);
                    if (abstractLoadBalance.getInvokers().isEmpty()) {
                        abstractLoadBalanceMap.remove(groupId);
                    }
                    continue;
                }
            }
            if (managedChannel.isShutdown() || managedChannel.isTerminated()) {
                managedChannel.shutdown();
                abstractLoadBalance.removeInvoker(serverContext);
                if (abstractLoadBalance.getInvokers().isEmpty()) {
                    abstractLoadBalanceMap.remove(groupId);
                }
                continue;
            }
            log.debug("GRPC Server:{},Group:{},App:{},IP:{},Port:{}\r\nGRPC Server:{},Group:{}",
                    serverContext.getGrpcProperties().getServer(), serverContext.getGrpcProperties().getGroupId(),
                    serverContext.getGrpcProperties().getAppId(), serverContext.getGrpcProperties().getHost(),
                    serverContext.getGrpcProperties().getPort(), serverName, groupId);
            return serverContext;
        }
        throw new RuntimeException("GRPC handle error, re-handle: connect, serverName=" + serverName + ", groupId=" + groupId + ",rootCause=no usable connect.");
    }

    public void init() {
        List<String> functionNames = grpcProperties.getRemoteServers();//从配置文件读取
        if (!CollectionUtils.isEmpty(functionNames)) {
            for (String functionName : functionNames) {
                Map<Object, Object> groupList = redisStringService.getAll(Common.DEFAULT_REDIS_GRPC_SERVICE_KEY + functionName);
                if (CollectionUtils.isEmpty(groupList)) {
                    continue;
                }
                Map<String, AbstractLoadBalance> serverMapTmp = new ConcurrentHashMap<>();
                for (Object groupObj : groupList.keySet()) {
                    String groupId = String.valueOf(groupObj);
                    Map<Object, Object> appList = redisStringService.getAll(Common.DEFAULT_REDIS_GRPC_GROUP_KEY + functionName + "_" + groupId);
                    if (CollectionUtils.isEmpty(appList)) {
                        continue;
                    }
                    AbstractLoadBalance roundRobinLoadBalance = new CcgRoundRobinLoadBalance(new ArrayList<>());
                    for (Object appObj : appList.keySet()) {
                        String appId = String.valueOf(appObj);
                        RemoteServer server = redisStringService.get(Common.DEFAULT_REDIS_GRPC_APP_KEY + appId, RemoteServer.class);
                        if (server != null) {
                            roundRobinLoadBalance.addInvoker(setConnection(server));
                        }
                    }
                    if (!roundRobinLoadBalance.getInvokers().isEmpty()) {
                        serverMapTmp.put(groupId, roundRobinLoadBalance);
                    }
                }
                serverMap.put(functionName, serverMapTmp);
            }
        }
        if (grpcProperties.isClientFlush()) {
            startExamineConnThread(grpcProperties.getDaemonThreadTimeout());
        }
    }

    private void startExamineConnThread(long daemonThreadTimeout) {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        // 第二个参数为首次执行的延时时间
        service.scheduleWithFixedDelay(() -> {
            //task compare serverMap from redis
            //遍历redisMap,serverMap中不存在则 新增
            try {
                List<String> functionNames = grpcProperties.getRemoteServers();//从配置文件读取
                for (String functionName : functionNames) {
                    Map<Object, Object> groupList = redisStringService.getAll(Common.DEFAULT_REDIS_GRPC_SERVICE_KEY + functionName);//getRedisList(Common.DEFAULT_REDIS_SERVICE_REGISTRY_KEY, functionName);
                    if (CollectionUtils.isEmpty(groupList)) {
                        continue;
                    }
                    for (Object groupObject : groupList.keySet()) {
                        String groupId = String.valueOf(groupObject);
                        Map<Object, Object> appList = redisStringService.getAll(Common.DEFAULT_REDIS_GRPC_GROUP_KEY + functionName + "_" + groupId);
                        if (CollectionUtils.isEmpty(appList)) {
                            continue;
                        }
                        for (Object appObj : appList.keySet()) {
                            //Map<String, Map<String, AbstractLoadBalance>>
                            Map<String, AbstractLoadBalance> groupMap;
                            if (!serverMap.containsKey(functionName)) {
                                groupMap = new ConcurrentHashMap<>();
                            } else {
                                groupMap = serverMap.get(functionName);
                            }
                            AbstractLoadBalance abstractLoadBalance;
                            if (groupMap.containsKey(groupId)) {
                                abstractLoadBalance = groupMap.get(groupId);
                            } else {
                                abstractLoadBalance = new CcgRoundRobinLoadBalance(new ArrayList<>());
                            }
                            String appId = String.valueOf(appObj);
                            if (abstractLoadBalance.doSelect(appId) == null) {
                                RemoteServer server = redisStringService.get(Common.DEFAULT_REDIS_GRPC_APP_KEY + appId, RemoteServer.class);
                                if (server != null) {
                                    ServerContext serverContext = setConnection(server);
                                    if (serverContext == null) {
                                        log.error("serverName={}, groupId={},heartBeat has exception job.ServerContext is null.", functionName, groupId);
                                        continue;
                                    }
                                    try {
                                        if (!checkIgnoreServices.containsKey(functionName)) {
                                            serverContext.heartBeat();
                                        }
                                    } catch (Exception e) {
                                        log.error("serverName={}, groupId={},heartBeat has exception job.", functionName, groupId, e);
                                        continue;
                                    }
                                    abstractLoadBalance.addInvoker(serverContext);
                                    if (groupMap.containsKey("default") && groupMap.get("default").doSelect(appId) == null) {
                                        groupMap.get("default").addInvoker(serverContext);
                                    }
                                }
                            }
                            groupMap.put(groupId, abstractLoadBalance);
                            serverMap.put(functionName, groupMap);
                        }
                    }
                }
                //删除无用链接
                for (Map.Entry<String, Map<String, AbstractLoadBalance>> entry : serverMap.entrySet()) {
                    Map<String, AbstractLoadBalance> groupMap = entry.getValue();
                    for (Map.Entry<String, AbstractLoadBalance> entry1 : groupMap.entrySet()) {
                        AbstractLoadBalance ab = entry1.getValue();
                        Iterator<ServerContext> iterator = ab.getInvokers().iterator();
                        while (iterator.hasNext()) {
                            boolean isNeedRemove = false;
                            ServerContext serverContext = iterator.next();
                            Object groupId = redisStringService.getField(Common.DEFAULT_REDIS_GRPC_SERVICE_KEY + entry.getKey(), entry1.getKey());
                            if (groupId != null) {
                                Object appId = redisStringService.getField(Common.DEFAULT_REDIS_GRPC_GROUP_KEY + entry.getKey() + "_" + entry1.getKey(), serverContext.getGrpcProperties().getAppId());
                                if (appId != null) {
                                    RemoteServer server = redisStringService.get(Common.DEFAULT_REDIS_GRPC_APP_KEY + serverContext.getGrpcProperties().getAppId(), RemoteServer.class);
                                    if (server != null) {
                                        try {
                                            if (!checkIgnoreServices.containsKey(entry.getKey())) {
                                                serverContext.heartBeat();
                                            }

                                        } catch (Exception e) {
                                            log.error("serverName={}, groupId={},heartBeat has exception job.", entry.getKey(), groupId, e);
                                            isNeedRemove = true;
                                        }
                                    } else {
                                        isNeedRemove = true;
                                    }
                                }
                            }
                            if (isNeedRemove && iterator.hasNext()) {
                                iterator.remove();
                                if (ab.getInvokers().isEmpty()) {
                                    groupMap.remove(entry1.getKey());
                                    if (groupMap.isEmpty()) {
                                        serverMap.remove(entry.getKey());
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("{startExamineConnThread} has exception,", e);
            }
            //遍历serverMap
        }, 10, daemonThreadTimeout, TimeUnit.SECONDS);
    }

    private ServerContext setConnection(RemoteServer server) {
        try {
            GrpcProperties grpcPropers = new GrpcProperties();
            BeanUtils.beanFieldCopy(grpcProperties, grpcPropers);
            BeanUtils.beanFieldCopy(server, grpcPropers);
            ManagedChannel channel = ManagedChannelBuilder.forAddress(server.getHost(), server.getPort())
                    .maxInboundMessageSize(GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE * grpcPropers.getFlowCtl())
                    .usePlaintext(true).build();
            if (clientInterceptor != null) {
                Channel newChannel = CcgInterceptChannel.intercept(channel, clientInterceptor);
                return new ServerContext(newChannel, serializeService, grpcPropers);
            } else {
                Class<?> clazz = grpcPropers.getClientInterceptor();
                try {
                    if (clazz == null) {
                        Channel newChannel = CcgInterceptChannel.intercept(channel, new SignClientInterceptor());
                        return new ServerContext(newChannel, serializeService, grpcPropers);
                    } else {
                        ClientInterceptor interceptor = (ClientInterceptor) clazz.newInstance();
                        Channel newChannel = CcgInterceptChannel.intercept(channel, interceptor);
                        return new ServerContext(newChannel, serializeService, grpcPropers);
                    }
                } catch (InstantiationException | IllegalAccessException e) {
                    log.warn("ClientInterceptor cannot use, ignoring...");
                    return new ServerContext(channel, serializeService, grpcPropers);
                }
            }
        } catch (Exception e) {
            log.warn("GRPC handle error, re-handle: connect, serverName={}, groupId={}, host={}, port={},rootCause=create connect has exceptions..", server.getServer(), server.getGroupId(), server.getHost(), server.getPort(), e);
        }
        return null;
    }
}
