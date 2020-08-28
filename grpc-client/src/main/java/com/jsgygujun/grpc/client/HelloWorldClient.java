package com.jsgygujun.grpc.client;

import com.jsgygujun.grpc.proto.helloworld.GreeterGrpc;
import com.jsgygujun.grpc.proto.helloworld.HelloReply;
import com.jsgygujun.grpc.proto.helloworld.HelloRequest;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

/**
 * gRPC HelloWorld 客户端
 *
 * @author GuJun
 * @date 2020/8/28
 */
public class HelloWorldClient {

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    public HelloWorldClient(Channel channel) {
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    public void greet(String name) {
        // 构造请求
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        try {
            // 执行请求并获取响应
            HelloReply reply = blockingStub.sayHello(request);
            System.out.println("[CLIENT] reply: " + reply.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String ip = "localhost";
        int port = 50051;
        ManagedChannel channel = ManagedChannelBuilder.forAddress(ip, port)
                .usePlaintext()
                .build();
        try {
            HelloWorldClient client = new HelloWorldClient(channel);
            client.greet("Hello, this is from client!");
        } finally {
            // 释放 ManagedChannel 资源
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
