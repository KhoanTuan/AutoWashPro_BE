package com.autowashpro.autowashpro_be.security;

import java.security.Principal;

/** Principal cho phiên WebSocket — name = "{STAFF|CUSTOMER}:{id}". */
public record WsPrincipal(String name) implements Principal {
    @Override
    public String getName() {
        return name;
    }
}
