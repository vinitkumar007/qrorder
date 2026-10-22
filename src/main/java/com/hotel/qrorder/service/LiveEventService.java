package com.hotel.qrorder.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Kitchen screens ko live events (SSE) bhejta hai. */
@Service
public class LiveEventService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void publish(String type, Object data) {
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().name(type).data(data));
            } catch (Exception ex) {
                emitters.remove(e);
            }
        }
    }

    @Scheduled(fixedRate = 25000)
    public void heartbeat() {
        publish("PING", "ok");
    }
}
