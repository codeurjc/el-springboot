package io.elastest.service;

import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import io.elastest.beats.Message;
import io.elastest.beats.MessageListener;
import io.elastest.beats.Server;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

@Service
public class BeatsServerService {
    public final Logger log = getLogger(lookup().lookupClass());

    private static EventLoopGroup group;
    private final String host = "0.0.0.0";
    private final int threadCount = 10;
    private Server server;
    private final int beatsPort = 5044;

    @PostConstruct
    void init() throws InterruptedException {
        group = new NioEventLoopGroup();
        this.startBeatsServer();
    }

    @PreDestroy
    void stopServer() throws InterruptedException {
        log.info("Shuting down Beats server");
        group.shutdownGracefully();
        server.stop();
    }

    public void startBeatsServer() throws InterruptedException {

        server = new Server(host, beatsPort, 30, threadCount);
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
        log.info("Listen at {} Beats Port", beatsPort);
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
            log.debug("The event message: {}", message);
            log.debug("The message data: {}", message.getData());
            receivedCount.incrementAndGet();
        }
    }
}
