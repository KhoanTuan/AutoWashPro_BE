package com.autowashpro.autowashpro_be.modules.customer.dto;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;

/**
 * Kết quả resolve khách cho booking Nhánh B/C.
 *
 * @param customer             khách đã resolve (luôn != null)
 * @param created              true nếu vừa tạo mới hồ sơ khách
 * @param activationEmailSent  true nếu đã lên lịch gửi email kích hoạt (claim) sau khi commit
 */
public record ResolvedCustomer(Customer customer, boolean created, boolean activationEmailSent) {
}
