package com.jsgygujun.grpc.server;

import com.jsgygujun.grpc.proto.helloworld.GreeterGrpc;
import com.jsgygujun.grpc.proto.helloworld.HelloReply;
import com.jsgygujun.grpc.proto.helloworld.HelloRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author GuJun
 * @date 2020/8/28
 */
public class RetryingHelloWorldServer {
    private static final float UNAVAILABLE_PERCENTAGE = 0.5F;
    private static final Random random = new Random();

    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new GreeterImpl())
                .build()
                .start();
        System.out.println("Server started, listening on " + port);

        DecimalFormat df = new DecimalFormat("#%");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    RetryingHelloWorldServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final RetryingHelloWorldServer server = new RetryingHelloWorldServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        AtomicInteger retryCounter = new AtomicInteger(0);

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            int count = retryCounter.incrementAndGet();
            if (random.nextFloat() < UNAVAILABLE_PERCENTAGE) {
                System.out.println("Returning stubbed UNAVAILABLE error. count: " + count);
                responseObserver.onError(Status.UNAVAILABLE
                        .withDescription("Greeter temporarily unavailable...").asRuntimeException());
            } else {
                System.out.println("Returning successful Hello response, count: " + count);
                HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
        }
    }
}
