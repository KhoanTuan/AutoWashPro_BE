package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {
}
