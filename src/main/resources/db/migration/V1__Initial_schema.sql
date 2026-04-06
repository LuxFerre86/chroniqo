CREATE TABLE `users` (
    `id`                                char(36)     NOT NULL,
    `email`                             varchar(255) NOT NULL,
    `password_hash`                     varchar(255) NOT NULL,
    `first_name`                        varchar(255) NOT NULL DEFAULT 'User',
    `last_name`                         varchar(255) NOT NULL DEFAULT 'Name',
    `weekly_target_hours`               int          NOT NULL DEFAULT 0,
    `country_code`                      varchar(10)           DEFAULT NULL,
    `subdivision_code`                  varchar(20)           DEFAULT NULL,
    `enabled`                           tinyint(1)   NOT NULL DEFAULT 0,
    `locked_until`                      datetime              DEFAULT NULL,
    `created_at`                        timestamp             NULL DEFAULT NULL,
    `last_login_at`                     timestamp             NULL DEFAULT NULL,
    `reset_token`                       varchar(255)          DEFAULT NULL,
    `reset_token_expiry_date`           timestamp             NULL DEFAULT NULL,
    `verification_token`                varchar(255)          DEFAULT NULL,
    `verification_token_expiry_date`    timestamp             NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_workdays` (
    `user_id`   char(36)    NOT NULL,
    `workday`   varchar(20) NOT NULL,
    PRIMARY KEY (`user_id`, `workday`),
    CONSTRAINT `fk_user_workdays_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `absences` (
    `id`        char(36)                         NOT NULL,
    `user_id`   char(36)                         NOT NULL,
    `date`      date                             NOT NULL,
    `type`      enum('VACATION','SICK')          NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_absence_01` (`user_id`, `date`) USING BTREE,
    CONSTRAINT `fk_absences_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `time_entries` (
    `id`            char(36)    NOT NULL,
    `user_id`       char(36)    NOT NULL,
    `date`          date        NOT NULL,
    `start_time`    time        NOT NULL,
    `end_time`      time                 DEFAULT NULL,
    `break_minutes` int                  DEFAULT NULL,
    `STATUS`        varchar(20) NOT NULL DEFAULT 'COMPLETED',
    `created_at`    timestamp            NULL DEFAULT NULL,
    `completed_at`  timestamp            NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_time_entries_01` (`user_id`, `date`),
    KEY `idx_time_entries_status` (`STATUS`),
    KEY `idx_time_entries_user_status` (`user_id`, `STATUS`),
    CONSTRAINT `fk_time_entries_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;