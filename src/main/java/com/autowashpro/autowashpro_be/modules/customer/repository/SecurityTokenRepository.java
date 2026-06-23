package com.autowashpro.autowashpro_be.modules.customer.repository;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.SecurityToken;
import com.autowashpro.autowashpro_be.modules.customer.entity.SecurityTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SecurityTokenRepository extends JpaRepository<SecurityToken, Long> {

    Optional<SecurityToken> findByToken(String token);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE SecurityToken t SET t.used = true
            WHERE t.customer = :customer AND t.tokenType = :type AND t.used = false
            """)
    void invalidateActiveTokens(@Param("customer") Customer customer,
                                @Param("type") SecurityTokenType type);
}
