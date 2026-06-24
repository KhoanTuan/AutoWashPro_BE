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

    @Column(name = "permission_code", nullable = false, unique = true, length = 100)
    private String permissionCode;

    @Column(length = 255)
    private String description;

    @Column(name = "module_group", length = 50)
    private String moduleGroup;

    @Column(nullable = false)
    @Builder.Default
    private Integer phase = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}
