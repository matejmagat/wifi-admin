package com.ht_rnd.wifi_admin_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduling support for the application.
 * This configuration allows Spring to detect and execute methods annotated
 * with scheduling annotations such as {@code @Scheduled}.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}