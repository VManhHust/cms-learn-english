package com.example.cmslearnenglish.service;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class SepayWebhookVerifier {

    private static final long MAX_TIMESTAMP_DRIFT_SECONDS = 300;
    private static final String HMAC_SHA_256 = "HmacSHA256";

    private final Clock clock;

    public SepayWebhookVerifier() {
        this(Clock.systemUTC());
    }

    SepayWebhookVerifier(Clock clock) {
        this.clock = clock;
    }

    public boolean isValid(String rawBody, String signature, String timestampHeader, String secret) {
        if (isBlank(rawBody) || isBlank(signature) || isBlank(timestampHeader) || isBlank(secret)) {
            return false;
        }

        try {
            long timestamp = Long.parseLong(timestampHeader);
            long now = Instant.now(clock).getEpochSecond();
            if (Math.abs(now - timestamp) > MAX_TIMESTAMP_DRIFT_SECONDS) {
                return false;
            }

            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            byte[] hash = mac.doFinal((timestampHeader + "." + rawBody).getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
