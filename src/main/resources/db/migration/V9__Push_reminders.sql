CREATE TABLE `push_subscriptions` (
    `id`          char(36)      NOT NULL,
    `user_id`     char(36)      NOT NULL,
    `endpoint`    varchar(700)  NOT NULL,
    `p256dh`      varchar(255)  NOT NULL,
    `auth_secret` varchar(255)  NOT NULL,
    `created_at`  datetime(6)   NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_push_subscription_endpoint` (`endpoint`),
    KEY `idx_push_subscriptions_user` (`user_id`),
    CONSTRAINT `fk_push_subscriptions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `reminder_settings` (
    `id`                         char(36) NOT NULL,
    `user_id`                    char(36) NOT NULL,
    `start_enabled`              bit(1)   NOT NULL,
    `start_time`                 time     NOT NULL,
    `end_enabled`                bit(1)   NOT NULL,
    `end_after_hours`            int      NOT NULL,
    `last_start_reminder_date`   date     NULL,
    `last_end_reminder_entry_id` char(36) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reminder_settings_user` (`user_id`),
    CONSTRAINT `fk_reminder_settings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
