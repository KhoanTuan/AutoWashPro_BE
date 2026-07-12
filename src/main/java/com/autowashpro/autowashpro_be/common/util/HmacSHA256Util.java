package com.autowashpro.autowashpro_be.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for HMAC-SHA256 signature generation used in MoMo payment gateway integration.
 * This utility generates cryptographic signatures to ensure payment request authenticity and integrity.
 */
public class HmacSHA256Util {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Generates an HMAC-SHA256 signature for the given message using the provided secret key.
     *
     * @param message   The message/payload to be signed
     * @param secretKey The secret key for HMAC signing
     * @return The Base64-encoded HMAC-SHA256 signature
     * @throws RuntimeException if the signing process fails
     */
    public static String generateSignature(String message, String secretKey) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    0,
                    secretKey.getBytes(StandardCharsets.UTF_8).length,
                    ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating HMAC-SHA256 signature: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies if the provided signature matches the expected signature for the given message and secret key.
     *
     * @param message        The message/payload that was signed
     * @param secretKey      The secret key used for signing
     * @param providedSignature The signature to verify (Base64-encoded)
     * @return true if the signature is valid, false otherwise
     */
    public static boolean verifySignature(String message, String secretKey, String providedSignature) {
        try {
            String expectedSignature = generateSignature(message, secretKey);
            return expectedSignature.equals(providedSignature);
        } catch (Exception e) {
            return false;
        }
    }
}
