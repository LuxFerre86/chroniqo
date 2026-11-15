CREATE TABLE `custom_holidays` (
    `id`         char(36)     NOT NULL,
    `user_id`    char(36)     NOT NULL,
    `name`       varchar(100) NOT NULL,
    `start_date` date         NOT NULL,
    `end_date`   date         NOT NULL,
    `recurring`  bit(1)       NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_custom_holidays_user` (`user_id`),
    CONSTRAINT `fk_custom_holidays_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
