package cn.ccg.grpc.selector;

import com.google.common.base.Preconditions;
import io.grpc.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CcgInterceptChannel {
    // Prevent instantiation
    private CcgInterceptChannel() {}

    /**
     * Create a new {@link Channel} that will call {@code interceptors} before starting a call on the
     * given channel. The first interceptor will have its {@link ClientInterceptor#interceptCall}
     * called first.
     *
     * @param channel the underlying channel to intercept.
     * @param interceptors array of interceptors to bind to {@code channel}.
     * @return a new channel instance with the interceptors applied.
     */
    public static Channel interceptForward(Channel channel, ClientInterceptor... interceptors) {
        return interceptForward(channel, Arrays.asList(interceptors));
    }

    /**
     * Create a new {@link Channel} that will call {@code interceptors} before starting a call on the
     * given channel. The first interceptor will have its {@link ClientInterceptor#interceptCall}
     * called first.
     *
     * @param channel the underlying channel to intercept.
     * @param interceptors a list of interceptors to bind to {@code channel}.
     * @return a new channel instance with the interceptors applied.
     */
    public static Channel interceptForward(Channel channel,
                                           List<? extends ClientInterceptor> interceptors) {
        List<? extends ClientInterceptor> copy = new ArrayList<ClientInterceptor>(interceptors);
        Collections.reverse(copy);
        return intercept(channel, copy);
    }

    /**
     * Create a new {@link Channel} that will call {@code interceptors} before starting a call on the
     * given channel. The last interceptor will have its {@link ClientInterceptor#interceptCall}
     * called first.
     *
     * @param channel the underlying channel to intercept.
     * @param interceptors array of interceptors to bind to {@code channel}.
     * @return a new channel instance with the interceptors applied.
     */
    public static Channel intercept(Channel channel, ClientInterceptor... interceptors) {
        return intercept(channel, Arrays.asList(interceptors));
    }

    /**
     * Create a new {@link Channel} that will call {@code interceptors} before starting a call on the
     * given channel. The last interceptor will have its {@link ClientInterceptor#interceptCall}
     * called first.
     *
     * @param channel the underlying channel to intercept.
     * @param interceptors a list of interceptors to bind to {@code channel}.
     * @return a new channel instance with the interceptors applied.
     */
    public static Channel intercept(Channel channel, List<? extends ClientInterceptor> interceptors) {
        Preconditions.checkNotNull(channel, "channel");
        for (ClientInterceptor interceptor : interceptors) {
            channel = new InterceptorChannel(channel, interceptor);
        }
        return channel;
    }

    public static class InterceptorChannel extends Channel {
        @Getter
        private final Channel channel;
        private final ClientInterceptor interceptor;

        private InterceptorChannel(Channel channel, ClientInterceptor interceptor) {
            this.channel = channel;
            this.interceptor = Preconditions.checkNotNull(interceptor, "interceptor");
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
                MethodDescriptor<ReqT, RespT> method, CallOptions callOptions) {
            return interceptor.interceptCall(method, callOptions, channel);
        }

        @Override
        public String authority() {
            return channel.authority();
        }
    }

    private static final ClientCall<Object, Object> NOOP_CALL = new ClientCall<Object, Object>() {
        @Override
        public void start(Listener<Object> responseListener, Metadata headers) {}

        @Override
        public void request(int numMessages) {}

        @Override
        public void cancel(String message, Throwable cause) {}

        @Override
        public void halfClose() {}

        @Override
        public void sendMessage(Object message) {}

        /**
         * Always returns {@code false}, since this is only used when the startup of the {@link
         * ClientCall} fails (i.e. the {@link ClientCall} is closed).
         */
        @Override
        public boolean isReady() {
            return false;
        }
    };

    /**
     * A {@link ForwardingClientCall} that delivers exceptions from its start logic to the
     * call listener.
     *
     * <p>{@link ClientCall#start(Listener, Metadata)} should not throw any
     * exception other than those caused by misuse, e.g., {@link IllegalStateException}.  {@code
     * CheckedForwardingClientCall} provides {@code checkedStart()} in which throwing exceptions is
     * allowed.
     */
    public abstract static class CheckedForwardingClientCall<ReqT, RespT>
            extends ForwardingClientCall<ReqT, RespT> {

        private ClientCall<ReqT, RespT> delegate;

        /**
         * Subclasses implement the start logic here that would normally belong to {@code start()}.
         *
         * <p>Implementation should call {@code this.delegate().start()} in the normal path. Exceptions
         * may safely be thrown prior to calling {@code this.delegate().start()}. Such exceptions will
         * be handled by {@code CheckedForwardingClientCall} and be delivered to {@code
         * responseListener}.  Exceptions <em>must not</em> be thrown after calling {@code
         * this.delegate().start()}, as this can result in {@link Listener#onClose} being
         * called multiple times.
         */
        protected abstract void checkedStart(Listener<RespT> responseListener, Metadata headers)
                throws Exception;

        protected CheckedForwardingClientCall(ClientCall<ReqT, RespT> delegate) {
            this.delegate = delegate;
        }

        @Override
        protected final ClientCall<ReqT, RespT> delegate() {
            return delegate;
        }

        @Override
        @SuppressWarnings("unchecked")
        public final void start(Listener<RespT> responseListener, Metadata headers) {
            try {
                checkedStart(responseListener, headers);
            } catch (Exception e) {
                // Because start() doesn't throw, the caller may still try to call other methods on this
                // call object. Passing these invocations to the original delegate will cause
                // IllegalStateException because delegate().start() was not called. We switch the delegate
                // to a NO-OP one to prevent the IllegalStateException. The user will finally get notified
                // about the error through the listener.
                delegate = (ClientCall<ReqT, RespT>) NOOP_CALL;
                responseListener.onClose(Status.fromThrowable(e), new Metadata());
            }
        }
    }
}
