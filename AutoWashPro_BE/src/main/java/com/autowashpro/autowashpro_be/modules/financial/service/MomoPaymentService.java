package com.autowashpro.autowashpro_be.modules.financial.service;

import com.autowashpro.autowashpro_be.common.BadRequestException;
import com.autowashpro.autowashpro_be.modules.financial.config.MomoProperties;
import com.autowashpro.autowashpro_be.modules.financial.dto.MomoCreatePaymentApiResponse;
import com.autowashpro.autowashpro_be.modules.financial.dto.MomoPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomoPaymentService {

    private final MomoProperties momoProperties;
    private final RestTemplate restTemplate;

    public MomoPaymentResponse createPayment(String orderId, BigDecimal amount, String orderInfo) {
        String requestId = UUID.randomUUID().toString();
        String extraData = "";
        long amountLong = amount.longValue();

        String rawSignature = buildRawSignature(
                momoProperties.getAccessKey(),
                String.valueOf(amountLong),
                extraData,
                momoProperties.getIpnUrl(),
                orderId,
                orderInfo,
                momoProperties.getPartnerCode(),
                momoProperties.getRedirectUrl(),
                requestId,
                momoProperties.getRequestType()
        );

        String signature = signHmacSha256(rawSignature, momoProperties.getSecretKey());

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", momoProperties.getPartnerCode());
        body.put("accessKey", momoProperties.getAccessKey());
        body.put("requestId", requestId);
        body.put("amount", String.valueOf(amountLong));
        body.put("orderId", orderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", momoProperties.getRedirectUrl());
        body.put("ipnUrl", momoProperties.getIpnUrl());
        body.put("extraData", extraData);
        body.put("requestType", momoProperties.getRequestType());
        body.put("signature", signature);
        body.put("lang", "vi");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.info("Invoking MoMo sandbox payment for orderId={}", orderId);
        MomoCreatePaymentApiResponse apiResponse = restTemplate.postForObject(
                momoProperties.getEndpoint(),
                entity,
                MomoCreatePaymentApiResponse.class
        );

        if (apiResponse == null) {
            throw new BadRequestException("MoMo payment gateway returned empty response");
        }

        if (apiResponse.getResultCode() != null && apiResponse.getResultCode() != 0) {
            throw new BadRequestException("MoMo payment failed: " + apiResponse.getMessage());
        }

        return MomoPaymentResponse.builder()
                .partnerCode(apiResponse.getPartnerCode())
                .requestId(apiResponse.getRequestId())
                .orderId(apiResponse.getOrderId())
                .resultCode(apiResponse.getResultCode())
                .message(apiResponse.getMessage())
                .payUrl(apiResponse.getPayUrl())
                .deeplink(apiResponse.getDeeplink())
                .qrCodeUrl(apiResponse.getQrCodeUrl())
                .build();
    }

    /**
     * Builds the raw signature string per MoMo Developer Sandbox guidelines.
     */
    public String buildRawSignature(String accessKey, String amount, String extraData,
                                    String ipnUrl, String orderId, String orderInfo,
                                    String partnerCode, String redirectUrl,
                                    String requestId, String requestType) {
        return "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
    }

    public String signHmacSha256(String rawData, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(rawData.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new BadRequestException("Failed to generate MoMo HMAC-SHA256 signature");
        }
    }
}
