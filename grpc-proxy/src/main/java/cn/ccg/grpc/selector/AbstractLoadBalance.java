package cn.ccg.grpc.selector;

import cn.ccg.grpc.ServerContext;
import cn.ccg.grpc.config.GrpcProperties;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Slf4j
public abstract class AbstractLoadBalance {
    protected List<ServerContext> serverContexts;

    public AbstractLoadBalance(List<ServerContext> serverContexts) {
        this.serverContexts = serverContexts;
    }

    protected final Multiset<ServerContext> results = HashMultiset.create();

    public ServerContext selectServerContext() {
        List<ServerContext> list = getInvokers();
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return doSelect();
    }

    public ServerContext selectServerContext(Object client) {
        List<ServerContext> list = getInvokers();
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return doSelect(client);
    }

    protected ServerContext doSelect() {
        return null;
    }

    public ServerContext doSelect(Object client) {
        for (ServerContext serverContext : serverContexts) {
            GrpcProperties grpcProperties;
            String appID;
            try {
                grpcProperties = serverContext.getGrpcProperties();
                appID = grpcProperties.getAppId();
                if (appID.equalsIgnoreCase(String.valueOf(client))) {
                    return serverContext;
                }
            } catch (Exception e) {
                log.warn("select serverContext failed", e);
            }
        }
        return null;
    }

    //todo 优化refesh
    public boolean addInvoker(ServerContext serverContext) {
        if (getInvokers() != null) {
            return serverContexts.add(serverContext);
        }
        return false;
    }

    //todo 优化refesh
    public boolean removeInvoker(Object key) {
        if (getInvokers() != null) {
            return serverContexts.remove(key);
        }
        return false;
    }

    //todo 不要返回集合
    public List<ServerContext> getInvokers() {
        return serverContexts;
    }

    public void result(int loop) {
        results.clear();
        if (loop < 1) {
            throw new IllegalArgumentException();
        }
        IntStream.range(0, loop).forEach(i -> results.add(selectServerContext()));
        Set<Multiset.Entry<ServerContext>> entrySet = results.entrySet();
        entrySet.stream().sorted(Comparator.comparingInt(Multiset.Entry::getCount)).forEach(System.out::println);
    }
}
