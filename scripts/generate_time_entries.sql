-- Erzeugt time_entries für alle Werktage (Mo-Fr) eines Datumsbereichs (MySQL 8+).
-- Tage, an denen der User bereits einen Eintrag oder eine Abwesenheit hat, werden übersprungen.

SET @user_id       = '00000000-0000-0000-0000-000000000000'; -- UUID des Users
SET @start_date    = '2026-01-01';                           -- inklusive
SET @end_date      = '2026-01-31';                           -- inklusive
SET @start_time    = '08:00:00';
SET @end_time      = '16:30:00';
SET @break_minutes = 30;

INSERT INTO time_entries (id, user_id, `date`, start_time, end_time, break_minutes, `STATUS`, created_at, completed_at)
WITH RECURSIVE days (d) AS (
    SELECT CAST(@start_date AS DATE)
    UNION ALL
    SELECT d + INTERVAL 1 DAY FROM days WHERE d < CAST(@end_date AS DATE)
)
SELECT UUID(),
       @user_id,
       days.d,
       @start_time,
       @end_time,
       @break_minutes,
       'COMPLETED',
       NOW(),
       NOW()
FROM days
WHERE WEEKDAY(days.d) < 5 -- 0 = Montag ... 4 = Freitag
  AND NOT EXISTS (SELECT 1 FROM time_entries te WHERE te.user_id = @user_id AND te.`date` = days.d)
  AND NOT EXISTS (SELECT 1 FROM absences a WHERE a.user_id = @user_id AND a.`date` = days.d);
