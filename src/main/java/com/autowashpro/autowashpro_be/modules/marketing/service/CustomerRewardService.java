package com.autowashpro.autowashpro_be.modules.marketing.service;

import com.autowashpro.autowashpro_be.modules.marketing.dto.response.CustomerRewardShopResponse;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.CustomerVoucherResponse;

import java.util.List;

public interface CustomerRewardService {
    List<CustomerRewardShopResponse> getRewardShop(Long customerId);
    CustomerVoucherResponse claimFreeVoucher(Long customerId, Long promotionId);
    CustomerVoucherResponse exchangePoints(Long customerId, Long promotionId);
    List<CustomerVoucherResponse> getMyVouchers(Long customerId, String status);
}
