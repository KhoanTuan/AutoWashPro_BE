package com.autowashpro.autowashpro_be.modules.customer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpStoreService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final long ttlSeconds;
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    public OtpStoreService(@Value("${app.otp.ttl-seconds:120}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public String generateAndStore(String phoneNumber) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        store.put(phoneNumber, new OtpEntry(otp, Instant.now().plusSeconds(ttlSeconds)));
        return otp;
    }

    public boolean verify(String phoneNumber, String otp) {
        OtpEntry entry = store.get(phoneNumber);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            store.remove(phoneNumber);
            return false;
        }
        if (!entry.otp().equals(otp)) {
            return false;
        }
        store.remove(phoneNumber);
        return true;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    private record OtpEntry(String otp, Instant expiresAt) {
    }
}
