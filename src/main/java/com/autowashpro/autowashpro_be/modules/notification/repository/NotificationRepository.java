package com.autowashpro.autowashpro_be.modules.notification.repository;

import com.autowashpro.autowashpro_be.modules.notification.entity.Notification;
import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationRecipientType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.recipientType = 'CUSTOMER' AND n.recipientId = :customerId ORDER BY n.createdAt DESC")
    List<Notification> findForCustomer(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientType = 'CUSTOMER' AND n.recipientId = :customerId AND n.isRead = false")
    long countUnreadForCustomer(@Param("customerId") Long customerId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientType = 'CUSTOMER' AND n.recipientId = :customerId AND n.isRead = false")
    void markAllAsReadForCustomer(@Param("customerId") Long customerId);

    @Query("SELECT n FROM Notification n WHERE n.recipientType = 'ALL_STAFF' OR (n.recipientType = 'STAFF' AND n.recipientId = :staffId) ORDER BY n.createdAt DESC")
    List<Notification> findForStaff(@Param("staffId") Long staffId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.recipientType = 'ALL_STAFF' OR (n.recipientType = 'STAFF' AND n.recipientId = :staffId)) AND n.isRead = false")
    long countUnreadForStaff(@Param("staffId") Long staffId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE (n.recipientType = 'ALL_STAFF' OR (n.recipientType = 'STAFF' AND n.recipientId = :staffId)) AND n.isRead = false")
    void markAllAsReadForStaff(@Param("staffId") Long staffId);
}
