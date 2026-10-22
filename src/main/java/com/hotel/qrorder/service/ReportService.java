package com.hotel.qrorder.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final JdbcTemplate jdbc;

    @Value("${app.timezone:Asia/Kolkata}")
    private String tz;

    public Map<String, Object> summary(int days) {
        ZoneId zone = ZoneId.of(tz);                       // invalid ho to start pe hi error
        int d = Math.max(1, Math.min(days, 90));
        LocalDate today = LocalDate.now(zone);
        OffsetDateTime todayStart = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime from = today.minusDays(d - 1L).atStartOfDay(zone).toOffsetDateTime();
        String z = zone.getId();                           // config se aaya hai, user input nahi

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("today", jdbc.queryForMap(
                "select count(*) as orders, coalesce(sum(total),0) as sales from orders "
              + "where status <> 'CANCELLED' and created_at >= ?", todayStart));
        out.put("paidToday", jdbc.queryForMap(
                "select count(*) as bills, coalesce(sum(total),0) as amount from payment where paid_at >= ?", todayStart));
        out.put("byMethod", jdbc.queryForList(
                "select method, count(*) as bills, coalesce(sum(total),0) as amount from payment "
              + "where paid_at >= ? group by method order by method", todayStart));
        out.put("daily", jdbc.queryForList(
                "select to_char(created_at at time zone '" + z + "', 'YYYY-MM-DD') as d, count(*) as orders, "
              + "coalesce(sum(total),0) as sales from orders where status <> 'CANCELLED' and created_at >= ? "
              + "group by 1 order by 1", from));
        out.put("topItems", jdbc.queryForList(
                "select mi.name as name, sum(oi.quantity) as qty, sum(oi.quantity * oi.unit_price) as revenue "
              + "from order_item oi join orders o on o.id = oi.order_id join menu_item mi on mi.id = oi.menu_item_id "
              + "where o.status <> 'CANCELLED' and o.created_at >= ? group by mi.name order by qty desc limit 8", from));
        out.put("hourly", jdbc.queryForList(
                "select to_char(created_at at time zone '" + z + "', 'HH24') as hr, count(*) as orders from orders "
              + "where status <> 'CANCELLED' and created_at >= ? group by 1 order by 1", todayStart));
        return out;
    }
}
