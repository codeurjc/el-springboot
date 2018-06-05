package io.elastest;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;
import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.impl.net.tcp.TCPNetSyslogServerConfig;
import org.springframework.stereotype.Service;

@Service
public class TcpServerService {
    private SyslogServerIF server;

    @PostConstruct
    void init() throws InterruptedException {
        SyslogServerConfigIF serverConfig = new TCPNetSyslogServerConfig(5001);

        SyslogServerEventHandlerIF handler = new SyslogServerEventHandlerIF() {
            private static final long serialVersionUID = 1L;

            @Override
            public void event(SyslogServerIF syslogServer,
                    SyslogServerEventIF event) {
                System.out.println(event.getMessage().toString());

            }
        };
        serverConfig.addEventHandler(handler);

        server = SyslogServer.createThreadedInstance("tcp_session",
                serverConfig);

    }

    @PreDestroy
    void stopServer() throws InterruptedException {
        server.shutdown();
    }

}
