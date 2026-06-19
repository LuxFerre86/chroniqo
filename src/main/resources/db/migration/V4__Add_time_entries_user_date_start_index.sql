CREATE INDEX `idx_time_entries_user_date_start`
    ON `time_entries` (`user_id`, `date`, `start_time`);

