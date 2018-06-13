package io.elastest.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import io.elastest.model.Trace;

@Service
public class QueueService {
    private final SimpMessagingTemplate messagingTemplate;

    QueueService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendTrace(Trace trace) {
        String queue = extractQueue(trace);
        this.messagingTemplate.convertAndSend("/exchange/amq.topic/" + queue,
                trace);
    }

    public String extractQueue(Trace trace) {
        return trace.getComponent() + "." + trace.getStream() + "."
                + trace.getExec() + "." + trace.getStreamType();

        // e.g.: sut_fullteaching.default_log.34.log
    }
}
