package com.autowashpro.autowashpro_be.modules.identity.repository;

import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    @Query("SELECT s FROM Staff s WHERE s.deletedAt IS NULL AND s.username = :username")
    Optional<Staff> findByUsername(@Param("username") String username);

    @Query("SELECT s FROM Staff s WHERE s.deletedAt IS NULL AND (s.username = :loginId OR LOWER(s.email) = LOWER(:loginId))")
    Optional<Staff> findByLoginId(@Param("loginId") String loginId);

    @Query("SELECT s FROM Staff s WHERE s.deletedAt IS NULL AND s.phoneNumber = :phoneNumber")
    Optional<Staff> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Staff s WHERE s.deletedAt IS NULL AND s.username = :username")
    boolean existsByUsername(@Param("username") String username);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Staff s WHERE s.deletedAt IS NULL AND LOWER(s.email) = LOWER(:email)")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Staff s WHERE s.deletedAt IS NULL AND s.phoneNumber = :phoneNumber")
    boolean existsByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT s FROM Staff s WHERE s.deletedAt IS NULL AND s.status = :status")
    List<Staff> findByStatus(@Param("status") StaffStatus status);

    @Query("SELECT s FROM Staff s WHERE s.staffId = :id AND s.deletedAt IS NULL")
    Optional<Staff> findActiveById(@Param("id") Long id);

    @Query("""
            SELECT s FROM Staff s
            WHERE (:includeDeleted = true OR s.deletedAt IS NULL)
            AND (:status IS NULL OR s.status = :status)
            AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(s.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR s.phoneNumber LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Staff> search(@Param("status") StaffStatus status,
                       @Param("keyword") String keyword,
                       @Param("includeDeleted") boolean includeDeleted,
                       Pageable pageable);

    @Query("SELECT COUNT(DISTINCT s) FROM Staff s JOIN s.roles r WHERE s.deletedAt IS NULL AND r.roleName = :roleName")
    long countActiveByRoleName(@Param("roleName") String roleName);

    @Query("""
            SELECT COUNT(DISTINCT s) FROM Staff s JOIN s.roles r
            WHERE s.deletedAt IS NULL AND r.roleName = :roleName AND s.status = :status
            """)
    long countByRoleNameAndStatus(@Param("roleName") String roleName,
                                  @Param("status") StaffStatus status);

    @Query("""
            SELECT DISTINCT s FROM Staff s JOIN s.roles r
            WHERE s.deletedAt IS NULL
            AND r.roleName = :roleName
            AND (:status IS NULL OR s.status = :status)
            ORDER BY s.fullName ASC
            """)
    List<Staff> findByRoleNameAndStatus(@Param("roleName") String roleName,
                                        @Param("status") StaffStatus status);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Staff s JOIN s.roles r
            WHERE s.deletedAt IS NULL AND s.staffId = :staffId AND r.roleName = :roleName
            """)
    boolean hasRoleName(@Param("staffId") Long staffId, @Param("roleName") String roleName);

    @Query("SELECT COUNT(s) FROM Staff s JOIN s.roles r WHERE r.roleId = :roleId AND s.deletedAt IS NULL")
    long countByRoleId(@Param("roleId") Integer roleId);
}
