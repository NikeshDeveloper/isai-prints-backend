package com.isaiprints.service;

import com.isaiprints.config.AppProperties;
import com.isaiprints.domain.entity.PushSubscription;
import com.isaiprints.domain.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class NotificationService {
    private final PushSubscriptionRepository subscriptionRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final List<SseEmitter> adminSseEmitters = new CopyOnWriteArrayList<>();
    private PushService pushService;

    public NotificationService(PushSubscriptionRepository subscriptionRepository,
                                AppProperties appProperties,
                                ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        initPushService();
    }

    private void initPushService() {
        AppProperties.Vapid vapid = appProperties.getVapid();
        if (vapid.getPublicKey() != null && !vapid.getPublicKey().isBlank()
                && vapid.getPrivateKey() != null && !vapid.getPrivateKey().isBlank()) {
            try {
                pushService = new PushService(vapid.getPublicKey(), vapid.getPrivateKey(), vapid.getSubject());
                log.info("[WebPush] Service initialized.");
            } catch (Exception e) {
                log.warn("[WebPush] Init failed: {}", e.getMessage());
            }
        }
    }

    public String getVapidPublicKey() {
        return appProperties.getVapid().getPublicKey();
    }

    public SseEmitter createAdminSseEmitter() {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout
        adminSseEmitters.add(emitter);
        emitter.onCompletion(() -> adminSseEmitters.remove(emitter));
        emitter.onTimeout(() -> adminSseEmitters.remove(emitter));
        emitter.onError(e -> adminSseEmitters.remove(emitter));
        return emitter;
    }

    public void notifyAdmin(Map<String, Object> payload) {
        // SSE broadcast
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : adminSseEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(objectMapper.writeValueAsString(payload)));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        adminSseEmitters.removeAll(dead);

        // WebPush
        if (pushService == null) return;
        List<PushSubscription> subs = subscriptionRepository.findAll();
        List<PushSubscription> toDelete = new ArrayList<>();
        for (PushSubscription sub : subs) {
            try {
                Subscription s = new Subscription(
                    sub.getEndpoint(),
                    new Subscription.Keys(sub.getP256dh(), sub.getAuth())
                );
                Notification notification = new Notification(s, objectMapper.writeValueAsString(payload));
                pushService.send(notification);
            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("410") || e.getMessage().contains("404"))) {
                    toDelete.add(sub);
                } else {
                    log.warn("[WebPush] Send error: {}", e.getMessage());
                }
            }
        }
        subscriptionRepository.deleteAll(toDelete);
    }
}
