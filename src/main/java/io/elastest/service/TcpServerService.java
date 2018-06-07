package io.elastest.service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;
import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.impl.net.tcp.TCPNetSyslogServerConfig;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

//docker run --rm  -itd --log-driver=syslog --log-opt syslog-address=tcp://localhost:5001 --log-opt tag=sut_34_exec elastest/test-etm-alpinegitjava sh -c "while true; do echo "aaaaa"; sleep 2; done"
@Service
public class TcpServerService {
    public final Logger log = getLogger(lookup().lookupClass());

    private SyslogServerIF server;
    private int tcpPort = 5001;
    private TracesService tracesService;

    public TcpServerService(TracesService tracesService) {
        this.tracesService = tracesService;
    }

    @PostConstruct
    void init() throws InterruptedException {
        SyslogServerConfigIF serverConfig = new TCPNetSyslogServerConfig(
                tcpPort);

        SyslogServerEventHandlerIF handler = new SyslogServerEventHandlerIF() {
            private static final long serialVersionUID = 1L;

            @Override
            public void event(SyslogServerIF syslogServer,
                    SyslogServerEventIF event) {
                log.debug("The message: {}", event.getMessage());
                tracesService.processTcpTrace(event.getMessage(), event.getDate());
            }
        };
        serverConfig.addEventHandler(handler);

        server = SyslogServer.createThreadedInstance("tcp_session",
                serverConfig);
        log.info("Listen at {} TCP Port", tcpPort);
    }

    @PreDestroy
    void stopServer() throws InterruptedException {
        log.info("Shuting down TCP server");
        server.shutdown();
    }

}
