package com.luxferre.chroniqo.service.push;

import com.luxferre.chroniqo.config.AppProperties;
import com.luxferre.chroniqo.model.PushSubscription;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Security;

/**
 * {@link PushSender} backed by the {@code web-push} library (VAPID).
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@Slf4j
@Component
public class WebPushSender implements PushSender {

    private final PushService pushService;

    public WebPushSender(AppProperties appProperties) {
        AppProperties.PushProperties push = appProperties.getPush();
        PushService service = null;
        if (push.isConfigured()) {
            try {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                }
                service = new PushService(push.getVapidPublicKey(), push.getVapidPrivateKey(), push.getSubject());
            } catch (Exception e) {
                log.error("Invalid VAPID configuration - push reminders are disabled", e);
            }
        }
        this.pushService = service;
    }

    @Override
    public boolean isEnabled() {
        return pushService != null;
    }

    @Override
    public Result send(PushSubscription subscription, String jsonPayload) {
        if (!isEnabled()) {
            return Result.FAILED;
        }
        try {
            Notification notification = new Notification(
                    subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(),
                    jsonPayload.getBytes(StandardCharsets.UTF_8));
            HttpResponse response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status == 404 || status == 410) {
                return Result.GONE;
            }
            if (status >= 200 && status < 300) {
                return Result.SENT;
            }
            log.warn("Push service answered with status {}", status);
            return Result.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.FAILED;
        } catch (Exception e) {
            log.warn("Sending push notification failed: {}", e.getMessage());
            return Result.FAILED;
        }
    }
}
