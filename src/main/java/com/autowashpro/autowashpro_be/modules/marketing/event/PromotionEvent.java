package com.autowashpro.autowashpro_be.modules.marketing.event;

import com.autowashpro.autowashpro_be.modules.marketing.entity.Promotion;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
@JsonIgnoreProperties({"source"})
public class PromotionEvent extends ApplicationEvent {
    @JsonIgnore
    private final Promotion promotion;
    private final Long promotionId;
    private final String promoCode;
    private final String action;

    public PromotionEvent(Object source, Promotion promotion, String action) {
        super(source);
        this.promotion = promotion;
        this.promotionId = promotion != null ? promotion.getId() : null;
        this.promoCode = promotion != null ? promotion.getCode() : null;
        this.action = action;
    }
}
