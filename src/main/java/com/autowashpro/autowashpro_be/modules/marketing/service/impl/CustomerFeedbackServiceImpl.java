package com.autowashpro.autowashpro_be.modules.marketing.service.impl;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.CustomerFeedbackCreateRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.FeedbackResponse;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerFeedback;
import com.autowashpro.autowashpro_be.modules.marketing.entity.FeedbackStatus;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerFeedbackRepository;
import com.autowashpro.autowashpro_be.modules.marketing.service.CustomerFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;

import org.springframework.context.ApplicationEventPublisher;
import com.autowashpro.autowashpro_be.modules.marketing.event.FeedbackEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerFeedbackServiceImpl implements CustomerFeedbackService {

    private final CustomerFeedbackRepository customerFeedbackRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(Long customerId, CustomerFeedbackCreateRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại ID: " + customerId));

        String bookingCode = request.getBookingCode();
        if ((bookingCode == null || bookingCode.isBlank()) && request.getBookingId() != null) {
            Booking b = bookingRepository.findById(request.getBookingId()).orElse(null);
            if (b != null) {
                bookingCode = b.getBookingCode();
            }
        }
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new IllegalArgumentException("Mã đơn hàng không được để trống");
        }

        Integer stars = request.getRatingStars();
        if (stars == null) {
            stars = request.getRating();
        }
        if (stars == null) {
            throw new IllegalArgumentException("Số sao đánh giá không được để trống");
        }

        // 1. Kiểm tra đơn hàng có tồn tại không
        final String finalBookingCode = bookingCode;
        Booking booking = bookingRepository.findByBookingCode(finalBookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt lịch với mã: " + finalBookingCode));

        // 2. Ràng buộc: Đơn hàng phải thuộc về đúng khách hàng này
        if (!booking.getCustomer().getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Đơn đặt lịch này không thuộc về tài khoản của bạn.");
        }

        // 3. Ràng buộc: Chỉ những đơn đã hoàn thành mới được phép gửi đánh giá
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Bạn chỉ có thể đánh giá đơn đặt lịch đã hoàn thành (COMPLETED). Trạng thái hiện tại: " + booking.getStatus());
        }

        // 4. Ràng buộc: Tránh spam đánh giá (mỗi đơn đặt lịch chỉ được đánh giá 1 lần duy nhất)
        if (customerFeedbackRepository.existsByBookingId(finalBookingCode)) {
            throw new IllegalArgumentException("Đơn đặt lịch này đã được gửi đánh giá trước đó.");
        }

        // Tự động lấy tên dịch vụ từ đơn hàng nếu không truyền lên
        String serviceName = request.getServiceName();
        if (serviceName == null || serviceName.isBlank()) {
            serviceName = booking.getItems() != null && !booking.getItems().isEmpty()
                          ? booking.getItems().get(0).getServiceNameSnapshot()
                          : "Dịch vụ rửa xe và chăm sóc chi tiết";
        }

        CustomerFeedback feedback = CustomerFeedback.builder()
                .customer(customer)
                .bookingId(finalBookingCode)
                .serviceName(serviceName)
                .ratingStars(stars)
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .status(FeedbackStatus.NEW)
                .build();

        CustomerFeedback saved = customerFeedbackRepository.save(feedback);
        log.info("Customer {} created feedback for booking {}: {} stars", customer.getFullName(), request.getBookingCode(), request.getRatingStars());
        
        eventPublisher.publishEvent(new FeedbackEvent(this, saved, "CREATED"));

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getMyFeedbacks(Long customerId) {
        return customerFeedbackRepository.findByCustomerCustomerId(customerId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private FeedbackResponse mapToResponse(CustomerFeedback f) {
        Customer c = f.getCustomer();
        return FeedbackResponse.builder()
                .id(f.getId())
                .customerId(c != null ? c.getCustomerId() : null)
                .customerName(c != null ? c.getFullName() : "Khách ẩn danh")
                .customerPhone(c != null ? c.getPhoneNumber() : "")
                .customerAvatar(c != null ? "https://api.dicebear.com/7.x/avataaars/svg?seed=" + c.getPhoneNumber() : null)
                .bookingId(f.getBookingId())
                .serviceName(f.getServiceName())
                .ratingStars(f.getRatingStars())
                .comment(f.getComment())
                .createdAt(f.getCreatedAt())
                .status(f.getStatus())
                .resolutionNotes(f.getResolutionNotes())
                .compensationVoucherCode(f.getCompensationVoucherCode())
                .build();
    }
}
