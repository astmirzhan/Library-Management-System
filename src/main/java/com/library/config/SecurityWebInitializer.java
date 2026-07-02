package com.library.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * Registers the {@code springSecurityFilterChain} filter with the servlet container
 * (the non-Spring-Boot equivalent of Boot's auto-registration).
 */
public class SecurityWebInitializer extends AbstractSecurityWebApplicationInitializer {
}
