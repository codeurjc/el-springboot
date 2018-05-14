package io.elastest;

import static java.lang.Thread.sleep;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import io.elastest.beats.Message;
import io.elastest.beats.MessageListener;
import io.elastest.beats.Server;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

@Service
public class ServerService {
    private static EventLoopGroup group;
    private final String host = "0.0.0.0";
    private final int threadCount = 10;
    private Server server;

    @PostConstruct
    void init() throws InterruptedException {
        group = new NioEventLoopGroup();
        this.testServerShouldAcceptConcurrentConnection();
    }

    @PreDestroy
    void stopServer() throws InterruptedException {
        group.shutdownGracefully();
        server.stop();
    }

    public void testServerShouldAcceptConcurrentConnection()
            throws InterruptedException {

        server = new Server(host, 5044, 30, threadCount);
        SpyListener listener = new SpyListener();
        server.setMessageListener(listener);
        Runnable serverTask = new Runnable() {
            @Override
            public void run() {
                try {
                    server.listen();
                } catch (InterruptedException e) {
                }
            }
        };

        new Thread(serverTask).start();
        sleep(1000); // start server give is some time.

    }

    /**
     * Used to assert the number of messages send to the server
     */
    private class SpyListener extends MessageListener {
        private AtomicInteger receivedCount;

        public SpyListener() {
            super();
            receivedCount = new AtomicInteger(0);
        }

        public void onNewMessage(ChannelHandlerContext ctx, Message message) {
            System.out.println("The message: ");
            System.out.println(message.getData());
            receivedCount.incrementAndGet();
        }
    }
}
