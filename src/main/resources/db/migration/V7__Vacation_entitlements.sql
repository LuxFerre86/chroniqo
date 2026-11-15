CREATE TABLE `vacation_entitlements` (
    `id`      char(36) NOT NULL,
    `user_id` char(36) NOT NULL,
    `entitlement_year` int      NOT NULL,
    `days`    int      NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vacation_entitlement_01` (`user_id`, `entitlement_year`),
    CONSTRAINT `fk_vacation_entitlements_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
