-- --------------------------------------------------------
-- Host:                         mariadb.local.andreas-daehn.de
-- Server Version:               12.2.2-MariaDB-ubu2404 - mariadb.org binary distribution
-- Server Betriebssystem:        debian-linux-gnu
-- HeidiSQL Version:             12.1.0.6537
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- Exportiere Struktur von Tabelle chroniqo.absences
CREATE TABLE IF NOT EXISTS `absences` (
                                          `id` char(36) NOT NULL,
                                          `user_id` char(36) NOT NULL,
                                          `date` date NOT NULL,
                                          `type` enum('VACATION','SICK','OTHER') NOT NULL,
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_absence_01` (`user_id`,`date`) USING BTREE,
                                          CONSTRAINT `fk_absences_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Daten Export vom Benutzer nicht ausgewählt

-- Exportiere Struktur von Tabelle chroniqo.time_entries
CREATE TABLE IF NOT EXISTS `time_entries` (
                                              `id` char(36) NOT NULL,
                                              `user_id` char(36) NOT NULL,
                                              `date` date NOT NULL,
                                              `start_time` time NOT NULL,
                                              `end_time` time DEFAULT NULL,
                                              `break_minutes` int(11) DEFAULT NULL,
                                              `STATUS` varchar(20) NOT NULL DEFAULT 'COMPLETED',
                                              `created_at` timestamp NULL DEFAULT NULL,
                                              `completed_at` timestamp NULL DEFAULT NULL,
                                              PRIMARY KEY (`id`),
                                              UNIQUE KEY `uk_time_entries_01` (`user_id`,`date`),
                                              KEY `idx_time_entries_status` (`STATUS`),
                                              KEY `idx_time_entries_user_status` (`user_id`,`STATUS`),
                                              CONSTRAINT `fk_time_entries_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Daten Export vom Benutzer nicht ausgewählt

-- Exportiere Struktur von Tabelle chroniqo.users
CREATE TABLE IF NOT EXISTS `users` (
                                       `id` char(36) NOT NULL,
                                       `email` varchar(255) NOT NULL,
                                       `password_hash` varchar(255) NOT NULL,
                                       `weekly_target_hours` int(11) DEFAULT 0,
                                       `federal_state` varchar(255) DEFAULT NULL,
                                       `first_name` varchar(255) NOT NULL DEFAULT 'User',
                                       `last_name` varchar(255) NOT NULL DEFAULT 'Name',
                                       `enabled` tinyint(1) NOT NULL DEFAULT 0,
                                       `created_at` timestamp NULL DEFAULT NULL,
                                       `last_login_at` timestamp NULL DEFAULT NULL,
                                       `reset_token` varchar(255) DEFAULT NULL,
                                       `reset_token_expiry_date` timestamp NULL DEFAULT NULL,
                                       `verification_token` varchar(255) DEFAULT NULL,
                                       `verification_token_expiry_date` timestamp NULL DEFAULT NULL,
                                       `locked_until` datetime DEFAULT NULL,
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Daten Export vom Benutzer nicht ausgewählt

-- Exportiere Struktur von Tabelle chroniqo.user_workdays
CREATE TABLE IF NOT EXISTS `user_workdays` (
                                               `user_id` char(36) NOT NULL,
                                               `workday` varchar(20) NOT NULL,
                                               PRIMARY KEY (`user_id`,`workday`),
                                               CONSTRAINT `fk_user_workdays_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Daten Export vom Benutzer nicht ausgewählt

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
