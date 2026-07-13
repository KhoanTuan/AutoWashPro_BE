package com.autowashpro.autowashpro_be.modules.marketing.event;

import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotion;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
@JsonIgnoreProperties({"source"})
public class VoucherRedemptionEvent extends ApplicationEvent {
    @JsonIgnore
    private final CustomerPromotion customerPromotion;
    private final Long customerPromotionId;
    private final String voucherCode;
    private final String action;

    public VoucherRedemptionEvent(Object source, CustomerPromotion customerPromotion, String action) {
        super(source);
        this.customerPromotion = customerPromotion;
        this.customerPromotionId = customerPromotion != null ? customerPromotion.getId() : null;
        this.voucherCode = customerPromotion != null ? customerPromotion.getVoucherCode() : null;
        this.action = action;
    }
}
