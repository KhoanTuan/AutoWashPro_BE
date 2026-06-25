package com.autowashpro.autowashpro_be.modules.financial.service;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.financial.entity.CustomerPromotion;
import com.autowashpro.autowashpro_be.modules.financial.entity.CustomerPromotionStatus;
import com.autowashpro.autowashpro_be.modules.financial.entity.DiscountType;
import com.autowashpro.autowashpro_be.modules.financial.entity.Promotion;
import com.autowashpro.autowashpro_be.modules.financial.entity.PromotionStatus;
import com.autowashpro.autowashpro_be.modules.financial.repository.CustomerPromotionRepository;
import com.autowashpro.autowashpro_be.modules.financial.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * RFM (Recency, Frequency, Monetary) clustering trigger that issues rescue vouchers
 * to at-risk customer segments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRfmMarketingTrigger {

    private static final int RFM_RECENCY_DAYS_THRESHOLD = 90;
    private static final int RFM_FREQUENCY_THRESHOLD = 2;
    private static final BigDecimal RFM_MONETARY_THRESHOLD = new BigDecimal("500000");

    private final CustomerRepository customerRepository;
    private final PromotionRepository promotionRepository;
    private final CustomerPromotionRepository customerPromotionRepository;

    @Scheduled(cron = "0 0 2 * * MON")
    @Transactional
    public void runRfmRescueCampaign() {
        log.info("Running RFM rescue voucher clustering");
        List<Customer> customers = customerRepository.findAll();
        List<RfmScore> scores = customers.stream()
                .map(this::scoreCustomer)
                .sorted(Comparator.comparingInt(RfmScore::totalScore))
                .toList();

        int rescueCount = Math.max(1, scores.size() / 5);
        List<RfmScore> atRisk = scores.stream().limit(rescueCount).toList();

        for (RfmScore rfm : atRisk) {
            issueRescueVoucher(rfm.customer());
        }

        log.info("RFM campaign completed — {} rescue vouchers issued", atRisk.size());
    }

    private RfmScore scoreCustomer(Customer customer) {
        int recencyScore = scoreRecency(customer);
        int frequencyScore = scoreFrequency(customer);
        int monetaryScore = scoreMonetary(customer);
        return new RfmScore(customer, recencyScore, frequencyScore, monetaryScore);
    }

    private int scoreRecency(Customer customer) {
        if (customer.getLastCompletedBookingAt() == null) {
            return 1;
        }
        long daysSince = ChronoUnit.DAYS.between(customer.getLastCompletedBookingAt(), LocalDateTime.now());
        if (daysSince >= RFM_RECENCY_DAYS_THRESHOLD) {
            return 1;
        }
        if (daysSince >= 60) {
            return 2;
        }
        if (daysSince >= 30) {
            return 3;
        }
        return 5;
    }

    private int scoreFrequency(Customer customer) {
        int visits = customer.getVisitCount() != null ? customer.getVisitCount() : 0;
        if (visits <= RFM_FREQUENCY_THRESHOLD) {
            return 1;
        }
        if (visits <= 5) {
            return 3;
        }
        return 5;
    }

    private int scoreMonetary(Customer customer) {
        BigDecimal spending = customer.getTotalSpending() != null ? customer.getTotalSpending() : BigDecimal.ZERO;
        if (spending.compareTo(RFM_MONETARY_THRESHOLD) < 0) {
            return 1;
        }
        if (spending.compareTo(new BigDecimal("2000000")) < 0) {
            return 3;
        }
        return 5;
    }

    private void issueRescueVoucher(Customer customer) {
        boolean alreadyHasRescue = customerPromotionRepository
                .findByCustomerCustomerIdAndStatus(customer.getCustomerId(), CustomerPromotionStatus.AVAILABLE)
                .stream()
                .anyMatch(cp -> Boolean.TRUE.equals(cp.getPromotion().getRescueVoucher()));

        if (alreadyHasRescue) {
            return;
        }

        String code = "RESCUE-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        Promotion promotion = Promotion.builder()
                .promotionCode(code)
                .name("RFM Rescue Voucher — " + customer.getFullName())
                .description("Automated rescue voucher for at-risk customer segment")
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("15"))
                .validFrom(now)
                .validTo(now.plusDays(30))
                .status(PromotionStatus.ACTIVE)
                .rescueVoucher(true)
                .build();
        promotionRepository.save(promotion);

        CustomerPromotion customerPromotion = CustomerPromotion.builder()
                .customer(customer)
                .promotion(promotion)
                .status(CustomerPromotionStatus.AVAILABLE)
                .build();
        customerPromotionRepository.save(customerPromotion);
    }

    private record RfmScore(Customer customer, int recency, int frequency, int monetary) {
        int totalScore() {
            return recency + frequency + monetary;
        }
    }
}
