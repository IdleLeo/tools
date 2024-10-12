package cn.ccg.grpc.selector;

import cn.ccg.grpc.ServerContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CcgRoundRobinLoadBalance extends AbstractLoadBalance {

    protected AtomicInteger offset = new AtomicInteger(-1);
    private final int maxOffSet;

    public CcgRoundRobinLoadBalance(List<ServerContext> serverContexts) {
        super(serverContexts);
        maxOffSet = Integer.MAX_VALUE / 10;
    }

    @Override
    protected ServerContext doSelect() {
        if (offset.get() >= maxOffSet) {
            offset.set(-1);
        }
        return getInvokers().get(offset.addAndGet(1) % getInvokers().size());
    }
}
