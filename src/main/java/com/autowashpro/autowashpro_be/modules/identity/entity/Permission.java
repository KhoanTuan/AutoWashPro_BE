package com.autowashpro.autowashpro_be.modules.identity.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Integer permissionId;

    @Column(name = "permission_code", nullable = false, unique = true, length = 50)
    private String permissionCode;
}
