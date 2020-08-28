package com.jsgygujun.grpc.client;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.jsgygujun.grpc.proto.helloworld.GreeterGrpc;
import com.jsgygujun.grpc.proto.helloworld.HelloReply;
import com.jsgygujun.grpc.proto.helloworld.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 带有重试策略的 gRPC 客户端
 *
 * @author GuJun
 * @date 2020/8/28
 */
public class RetryingHelloWorldClient {

    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;
    private final AtomicInteger succeedCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();

    protected Map<String, ?> getRetryingServiceConfig() {
        return new Gson()
                .fromJson(
                    new JsonReader(new InputStreamReader(RetryingHelloWorldClient.class.getResourceAsStream("/retrying_service_config.json"), UTF_8)),
                Map.class);
    }

    public RetryingHelloWorldClient(String host, int port, boolean enableRetries) {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext();
        if (enableRetries) {
            Map<String, ?> serviceConfig = getRetryingServiceConfig();
            channelBuilder.defaultServiceConfig(serviceConfig).enableRetry();
        }
        channel = channelBuilder.build();
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws Exception {
        channel.shutdown().awaitTermination(60, TimeUnit.SECONDS);
    }

    public void greet(String msg) {
        HelloRequest request = HelloRequest.newBuilder().setName(msg).build();
        try {
            HelloReply reply = blockingStub.sayHello(request);
            succeedCount.incrementAndGet();
            System.out.println("succeed");
        } catch (StatusRuntimeException e) {
            failedCount.incrementAndGet();
            System.out.println("failed");
        }
    }

    public void printSummary() {
        System.out.println("total: " + (succeedCount.get() + failedCount.get())
                + "\n" + ", succeed: " + succeedCount.get()
                + "\n" + ", failed; " + failedCount.get());
    }

    public static void main(String[] args) throws Exception {

        final RetryingHelloWorldClient client = new RetryingHelloWorldClient("localhost", 50051, true);
        ForkJoinPool executor = new ForkJoinPool();

        for (int i = 0; i < 1000; i++) {
            final String userId = "user" + i;
            executor.execute(
                    new Runnable() {
                        @Override
                        public void run() {
                            client.greet(userId);
                        }
                    });
        }
        executor.awaitQuiescence(100, TimeUnit.SECONDS);
        executor.shutdown();
        client.printSummary();
        client.shutdown();
    }
}

