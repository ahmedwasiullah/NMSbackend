package com.dashboard.NMS.Repository;

import com.dashboard.NMS.DTO.DeviceHistory;
import com.dashboard.NMS.DTO.Logs;
import com.dashboard.NMS.DTO.NetworkData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public class DeviceStatusRepository {

    private final JdbcTemplate jdbcTemplate;

    public DeviceStatusRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public NetworkData getDeviceStatusSummary(String tableName) {
        String sql = String.format("""
            WITH latest_status AS (
                SELECT
                    ip,
                    status,
                    ROW_NUMBER() OVER (PARTITION BY ip ORDER BY timestamp DESC) AS rn
                FROM
                    %s
                WHERE
                    timestamp >= CURRENT_DATE
                    AND timestamp < CURRENT_DATE + INTERVAL '1 day'
            )
            SELECT 
                COUNT(CASE WHEN status = 'UP' THEN 1 END) AS active_devices,
                COUNT(CASE WHEN status = 'DOWN' THEN 1 END) AS inactive_devices,
                COUNT(CASE WHEN status IS NULL AND rn IS NULL THEN 1 END) AS no_status_devices
            FROM 
                (
                    SELECT 
                        ip,
                        status,
                        rn
                    FROM 
                        latest_status
                    WHERE rn = 1
                    UNION ALL
                    SELECT 
                        ip,
                        NULL AS status,
                        NULL AS rn
                    FROM 
                        (SELECT DISTINCT ip FROM %s) AS all_devices
                    WHERE 
                        ip NOT IN (SELECT ip FROM latest_status)
                ) AS combined_status
            """, tableName, tableName);

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new NetworkData(
                        rs.getInt("active_devices"),
                        rs.getInt("inactive_devices"),
                        rs.getInt("no_status_devices")
                )
        );
    }

    public List<Logs> getDeviceLogs(String tableName) {
        String sql = String.format("""
                SELECT DISTINCT ON (ip) ip, name, status, timestamp
                FROM %s
                ORDER BY ip, timestamp DESC;
       
                """, tableName);

        return jdbcTemplate.query(sql, new LogsRowMapper());
    }

    public DeviceHistory getDeviceData(String tableName, String name) {
        // SQL to get total count of UP and DOWN and their durations
        String sql = String.format("""
        WITH time_intervals AS (
            SELECT 
                status,
                EXTRACT(EPOCH FROM (LEAD(timestamp) OVER (ORDER BY timestamp) - timestamp)) AS duration
            FROM %s
            WHERE name = ?
        )
        SELECT 
            COUNT(*) FILTER (WHERE status = 'UP') AS NoOfUp,
            COUNT(*) FILTER (WHERE status = 'DOWN') AS NoOfDown,
            COALESCE(SUM(duration) FILTER (WHERE status = 'UP'), 0) AS TotalUpTime,
            COALESCE(SUM(duration) FILTER (WHERE status = 'DOWN'), 0) AS TotalDownTime
        FROM time_intervals
    """, tableName);

        // SQL to get the most recent status and its duration
        String sql2 = String.format("""
        WITH cte AS (
            SELECT status, timestamp
            FROM %s
            WHERE name= ?
            ORDER BY timestamp DESC
            LIMIT 1
        )
        SELECT
            status, 
            EXTRACT(EPOCH FROM (NOW() - timestamp))  AS duration_minutes -- Duration in minutes
        FROM cte;
    """, tableName);

        // Execute sql to get the main stats (NoOfUp, NoOfDown, TotalUpTime, TotalDownTime)
        DeviceHistory deviceHistory = jdbcTemplate.queryForObject(sql, new Object[]{name}, (rs, rowNum) -> {
            DeviceHistory history = new DeviceHistory();
            history.setNoOfUp(rs.getInt("NoOfUp"));
            history.setNoOfDown(rs.getInt("NoOfDown"));
            history.setTotalUpTime(rs.getDouble("TotalUpTime"));
            history.setTotalDownTime(rs.getDouble("TotalDownTime"));
            return history;
        });

        // Execute sql2 to get the most recent status and its duration
          Map<String, Object> result= jdbcTemplate.queryForMap(sql2, new Object[]{name});

        String mostRecentStatus = (String) result.get("status");
        BigDecimal mostRecentDuration = (BigDecimal) result.get("duration_minutes");
        Double doubleValue = mostRecentDuration.doubleValue();

        // Add the duration from sql2 to the appropriate total time (TotalUpTime or TotalDownTime)
        if ("UP".equals(mostRecentStatus)) {
            deviceHistory.setTotalUpTime(deviceHistory.getTotalUpTime() + doubleValue);
        } else if ("DOWN".equals(mostRecentStatus)) {
            deviceHistory.setTotalDownTime(deviceHistory.getTotalDownTime() + doubleValue);
        }

        return deviceHistory;
    }

    public DeviceHistory getDeviceData(String tableName, String name,int days) {
        // SQL to get total count of UP and DOWN and their durations
        String sql = String.format("""
        WITH time_intervals AS (
            SELECT 
                status,
                EXTRACT(EPOCH FROM (LEAD(timestamp) OVER (ORDER BY timestamp) - timestamp)) AS duration
            FROM %s
             WHERE name = ?
            AND timestamp <= CURRENT_DATE AND timestamp >= CURRENT_DATE - INTERVAL '%d day'
        )
        SELECT 
            COUNT(*) FILTER (WHERE status = 'UP') AS NoOfUp,
            COUNT(*) FILTER (WHERE status = 'DOWN') AS NoOfDown,
            COALESCE(SUM(duration) FILTER (WHERE status = 'UP'), 0) AS TotalUpTime,
            COALESCE(SUM(duration) FILTER (WHERE status = 'DOWN'), 0) AS TotalDownTime
        FROM time_intervals
    """, tableName,days);

        // SQL to get the most recent status and its duration
        String sql2 = String.format("""
        WITH cte AS (
            SELECT status, timestamp
            FROM %s
            WHERE name= ?
            ORDER BY timestamp DESC
            LIMIT 1
        )
        SELECT
            status, 
            EXTRACT(EPOCH FROM (NOW() - timestamp))  AS duration_minutes -- Duration in minutes
        FROM cte;
    """, tableName);

        // Execute sql to get the main stats (NoOfUp, NoOfDown, TotalUpTime, TotalDownTime)
        DeviceHistory deviceHistory = jdbcTemplate.queryForObject(sql, new Object[]{name}, (rs, rowNum) -> {
            DeviceHistory history = new DeviceHistory();
            history.setNoOfUp(rs.getInt("NoOfUp"));
            history.setNoOfDown(rs.getInt("NoOfDown"));
            history.setTotalUpTime(rs.getDouble("TotalUpTime"));
            history.setTotalDownTime(rs.getDouble("TotalDownTime"));
            return history;
        });

        // Execute sql2 to get the most recent status and its duration
        Map<String, Object> result= jdbcTemplate.queryForMap(sql2, new Object[]{name});

        String mostRecentStatus = (String) result.get("status");
        BigDecimal mostRecentDuration = (BigDecimal) result.get("duration_minutes");
        Double doubleValue = mostRecentDuration.doubleValue();

        // Add the duration from sql2 to the appropriate total time (TotalUpTime or TotalDownTime)
        if ("UP".equals(mostRecentStatus)) {
            deviceHistory.setTotalUpTime(deviceHistory.getTotalUpTime() + doubleValue);
        } else if ("DOWN".equals(mostRecentStatus)) {
            deviceHistory.setTotalDownTime(deviceHistory.getTotalDownTime() + doubleValue);
        }

        return deviceHistory;
    }


//    public DeviceHistory getDeviceData(String tableName, String name) {
//        String sql = String.format("""
//        WITH time_intervals AS (
//            SELECT
//                status,
//                EXTRACT(EPOCH FROM (LEAD(timestamp) OVER (ORDER BY timestamp) - timestamp)) AS duration
//            FROM %s
//            WHERE name = ?
//        )
//        SELECT
//            COUNT(*) FILTER (WHERE status = 'UP') AS NoOfUp,
//            COUNT(*) FILTER (WHERE status = 'DOWN') AS NoOfDown,
//            COALESCE(SUM(duration) FILTER (WHERE status = 'UP'), 0) AS TotalUpTime,
//            COALESCE(SUM(duration) FILTER (WHERE status = 'DOWN'), 0) AS TotalDownTime
//        FROM time_intervals
//        """, tableName);
//
//
//        String sql2 = String.format("""
//    WITH cte AS (
//        SELECT status, timestamp
//        FROM %s
//        Where name= ?
//        ORDER BY timestamp DESC
//        LIMIT 1
//    )
//    SELECT
//        status,
//        EXTRACT(EPOCH FROM (NOW() - timestamp))  AS duration -- Duration in seconds
//    FROM cte;
//""", tableName);
//
//        return jdbcTemplate.queryForObject(sql, new Object[]{name}, (rs, rowNum) -> {
//            DeviceHistory deviceHistory = new DeviceHistory();
//            deviceHistory.setNoOfUp(rs.getInt("NoOfUp"));
//            deviceHistory.setNoOfDown(rs.getInt("NoOfDown"));
//            deviceHistory.setTotalUpTime(rs.getDouble("TotalUpTime"));
//            deviceHistory.setTotalDownTime(rs.getDouble("TotalDownTime"));
//            return deviceHistory;
//        });
//    }

//    public DeviceHistory getDeviceData(String tableName, String name,int days) {
//        String sql = String.format("""
//        WITH time_intervals AS (
//            SELECT
//                status,
//                EXTRACT(EPOCH FROM (LEAD(timestamp) OVER (ORDER BY timestamp) - timestamp)) AS duration
//            FROM %s
//            WHERE name = ?
//            AND timestamp <= CURRENT_DATE AND timestamp >= CURRENT_DATE - INTERVAL '%d day'
//        )
//        SELECT
//            COUNT(*) FILTER (WHERE status = 'UP') AS NoOfUp,
//            COUNT(*) FILTER (WHERE status = 'DOWN') AS NoOfDown,
//            COALESCE(SUM(duration) FILTER (WHERE status = 'UP'), 0) AS TotalUpTime,
//            COALESCE(SUM(duration) FILTER (WHERE status = 'DOWN'), 0) AS TotalDownTime
//        FROM time_intervals
//        """, tableName,days);
//
//        return jdbcTemplate.queryForObject(sql, new Object[]{name}, (rs, rowNum) -> {
//            DeviceHistory deviceHistory = new DeviceHistory();
//            deviceHistory.setNoOfUp(rs.getInt("NoOfUp"));
//            deviceHistory.setNoOfDown(rs.getInt("NoOfDown"));
//            deviceHistory.setTotalUpTime(rs.getDouble("TotalUpTime"));
//            deviceHistory.setTotalDownTime(rs.getDouble("TotalDownTime"));
//            return deviceHistory;
//        });
//    }


    public List<Logs> getSpecificDeviceLogs(String tableName, String name) {
        String sql = String.format("SELECT ip, name, status, timestamp FROM %s WHERE name = ?", tableName);
        return jdbcTemplate.query(sql, new Object[]{name}, new LogsRowMapper());
    }

    public List<Logs> getSpecificDeviceLogs(String tableName, String name, int days) {
        String sql = String.format("SELECT ip, name, status, timestamp FROM %s WHERE name = ? AND timestamp <= CURRENT_DATE " +
                "AND timestamp >= CURRENT_DATE - INTERVAL '%d days'", tableName, days);

        // Use jdbcTemplate to execute the query
        return jdbcTemplate.query(sql, new Object[]{name}, new LogsRowMapper());
    }


    // RowMapper for Logs DTO
    private static class LogsRowMapper implements RowMapper<Logs> {
        @Override
        public Logs mapRow(ResultSet rs, int rowNum) throws SQLException {
            // no need to log if name is null
            Logs logEntry = new Logs();
            logEntry.setIp(rs.getString("ip"));
            logEntry.setName(rs.getString("name"));
            logEntry.setStatus(rs.getString("status"));
            logEntry.setTimeStamp(rs.getTimestamp("timestamp"));
            return logEntry;
        }
    }
}
