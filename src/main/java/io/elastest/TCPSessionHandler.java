package io.elastest;

import java.net.SocketAddress;

import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;

public class TCPSessionHandler implements SyslogServerEventHandlerIF {
    private static final long serialVersionUID = -5516122648345973259L;

    public static int currentSession = 0;
    public static final String[] SESSIONS = { "one", "two", "three", "four" };
    public String id = null;
    public int eventCount[] = new int[4];
    public int closeCount[] = new int[4];
    public boolean initialized = false;
    public boolean destroyed = false;

    public TCPSessionHandler(String id) {
        this.id = id;

        for (int i = 0; i < 4; i++) {
            eventCount[i] = 0;
            closeCount[i] = 0;
        }
    }

    public void initialize(SyslogServerIF syslogServer) {
        this.initialized = true;
        System.out.println("initialized " + syslogServer.getProtocol());
    }

    public Object sessionOpened(SyslogServerIF syslogServer,
            SocketAddress socketAddress) {
        String session = SESSIONS[currentSession++];

        System.out.println("opened: " + id + "/" + session);

        return session;
    }

    protected int translate(String word) {
        if ("one".equals(word)) {
            return 0;

        } else if ("two".equals(word)) {
            return 1;

        } else if ("three".equals(word)) {
            return 2;

        } else if ("four".equals(word)) {
            return 3;
        }

        return -1;
    }

    public void event(Object session, SyslogServerIF syslogServer,
            SocketAddress socketAddress, SyslogServerEventIF event) {
        if (session != null) {
            int i = translate((String) session);

            if (i != -1) {
                eventCount[i]++;
                System.out.println(
                        id + " " + session + " " + i + " " + eventCount[i]);
            }
        }

        System.out.println("event: " + id + "/" + session.toString() + "/"
                + event.getMessage());
    }

    public void exception(Object session, SyslogServerIF syslogServer,
            SocketAddress socketAddress, Exception exception) {
        // This section is not (yet) tested; a bit tricky to cause a
        // SocketException -- but not impossible
        if (session != null) {
            System.out.println("exception: " + id + "/" + session.toString()
                    + ": " + exception);

        } else {
            System.out.println("exception: " + id + ": " + exception);
        }
    }

    public void sessionClosed(Object session, SyslogServerIF syslogServer,
            SocketAddress socketAddress, boolean timeout) {
        if (session != null) {
            int i = translate((String) session);

            if (i != -1) {
                closeCount[i]++;
            }
        }

        System.out.println("closed: " + id + "/" + session.toString());
    }

    public void destroy(SyslogServerIF syslogServer) {
        this.destroyed = true;
        System.out.println("destroyed " + syslogServer.getProtocol());
    }

    @Override
    public void event(SyslogServerIF syslogServer, SyslogServerEventIF event) {
        // TODO Auto-generated method stub

    }
}