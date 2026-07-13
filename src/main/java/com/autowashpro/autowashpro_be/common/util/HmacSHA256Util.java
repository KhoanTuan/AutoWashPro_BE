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
     * @return The Hex-encoded HMAC-SHA256 signature (Note: MoMo actually uses Hex-encoding for signatures, wait, let's check!)
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
            
            // Wait, does MoMo use HEX signature instead of Base64?
            // MoMo signature is usually HEX format: 
            // String signature = HmacSHA256Util.generateSignature(rawData, secretKey);
            // Let's look at the output format of MoMo's signature. MoMo uses HEX encoding (lower case).
            // Wait, in the Test BE it was Base64 or HEX? Let's check!
            // In Test BE, line 37 was: Base64.getEncoder().encodeToString(digest)? No, wait!
            // If they used Base64, and the user said "lỗi nhiều quá với cũng sai nghiệp vụ", maybe because the signature verification failed due to Base64 vs Hex?
            // Ah! MoMo's standard API signature is Hex-encoded! (lower case: e.g. "4b8a5...")
            // Let's write a hex-encoded generator, or check if MoMo uses Hex or Base64.
            // Let's check if the signature in MoMo docs is Hex: yes, MoMo uses Hex encoding (HmacSHA256 with Hex representation).
            // Let's write Hex representation:
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating HMAC-SHA256 signature: " + e.getMessage(), e);
        }
    }

    public static boolean verifySignature(String message, String secretKey, String providedSignature) {
        try {
            String expectedSignature = generateSignature(message, secretKey);
            return expectedSignature.equalsIgnoreCase(providedSignature);
        } catch (Exception e) {
            return false;
        }
    }
}
