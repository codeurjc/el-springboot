package io.elastest.service;

import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

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

// docker run --rm --name zxc -e LOGSTASHHOST=172.17.0.1 -e LOGSTASHPORT=5044 -v /var/run/docker.sock:/var/run/docker.sock elastest/etm-dockbeat
@Service
public class BeatsServerService {
    public final Logger log = getLogger(lookup().lookupClass());

    private static EventLoopGroup group;
    private final String host = "0.0.0.0";
    private final int threadCount = 10;
    private Server server;
    private final int beatsPort = 5044;

    private TracesService tracesService;

    public BeatsServerService(TracesService tracesService) {
        this.tracesService = tracesService;
    }

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
        public void onNewMessage(ChannelHandlerContext ctx, Message message) {
            log.debug("The Beats message data: {}", message.getData());
            tracesService.processBeatTrace(message.getData());
        }
    }
}
