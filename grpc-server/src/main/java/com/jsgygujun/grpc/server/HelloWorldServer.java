package com.jsgygujun.grpc.server;

import com.jsgygujun.grpc.proto.helloworld.GreeterGrpc;
import com.jsgygujun.grpc.proto.helloworld.HelloReply;
import com.jsgygujun.grpc.proto.helloworld.HelloRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * gRPC HelloWorld 服务端
 *
 * @author GuJun
 * @date 2020/8/28
 */
public class HelloWorldServer {

    private Server server;

    private void start() throws IOException {
        int port = 50051;
        server = ServerBuilder
                .forPort(port)
                .addService(new GreeterImpl())
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    HelloWorldServer.this.stop();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() throws Exception {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final HelloWorldServer server = new HelloWorldServer();
        server.start();
        server.blockUntilShutdown();
    }

    /**
     * 定义服务
     */
    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

}
