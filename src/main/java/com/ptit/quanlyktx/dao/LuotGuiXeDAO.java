package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.LuotGuiXeRow;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

/** Đọc/ghi LUOT_GUI_XE; một dòng = một lần gửi và lấy hoàn chỉnh. */
public class LuotGuiXeDAO {
    private static final BigDecimal PHI_VUOT_LUOT = new BigDecimal("3000.00");
    private static final String BASE = """
            SELECT l.id_luot, l.ma_ve_xe, v.bien_so_xe, v.ma_sv, s.ho_ten,
                   l.thoi_gian_gui, l.thoi_gian_lay, l.tien_phat_sinh
              FROM LUOT_GUI_XE l
              JOIN VE_XE_THANG v ON v.ma_ve_xe = l.ma_ve_xe
              JOIN SINH_VIEN s ON s.ma_sv = v.ma_sv
            """;

    private LuotGuiXeRow fromRow(ResultSet rs) throws SQLException {
        Timestamp send = rs.getTimestamp("thoi_gian_gui");
        Timestamp take = rs.getTimestamp("thoi_gian_lay");
        return new LuotGuiXeRow(
                rs.getInt("id_luot"), rs.getString("ma_ve_xe"),
                rs.getString("bien_so_xe"), rs.getString("ma_sv"), rs.getString("ho_ten"),
                send == null ? null : send.toLocalDateTime(),
                take == null ? null : take.toLocalDateTime(),
                rs.getBigDecimal("tien_phat_sinh"));
    }

    public List<LuotGuiXeRow> findOpen() throws SQLException {
        List<LuotGuiXeRow> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE +
                     " WHERE l.thoi_gian_lay IS NULL ORDER BY l.thoi_gian_gui" );
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(fromRow(rs));
        }
        return result;
    }

    public List<LuotGuiXeRow> findHistory(LocalDate from, LocalDate to) throws SQLException {
        if (from == null || to == null || from.isAfter(to))
            throw new SQLException("Khoảng ngày lọc lịch sử không hợp lệ.");
        List<LuotGuiXeRow> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + """
                     WHERE l.thoi_gian_gui >= ? AND l.thoi_gian_gui < ?
                     ORDER BY l.thoi_gian_gui ASC, l.id_luot ASC
                     """)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(fromRow(rs));
            }
        }
        return result;
    }

    public int countToday(String maVeXe) throws SQLException {
        LocalDate today = LocalDate.now();
        try (Connection c = DatabaseConnection.getConnection()) {
            return countDay(c, maVeXe, today);
        }
    }

    public BigDecimal todayFees() throws SQLException {
        LocalDate today = LocalDate.now();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT COALESCE(SUM(tien_phat_sinh), 0)
                     FROM LUOT_GUI_XE
                     WHERE thoi_gian_gui >= ? AND thoi_gian_gui < ?
                     """)) {
            ps.setTimestamp(1, Timestamp.valueOf(today.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(today.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next(); return rs.getBigDecimal(1);
            }
        }
    }

    /** Ghi nhận lượt gửi mới, hai lượt đầu/xe/ngày miễn phí, từ lượt thứ ba 3.000đ. */
    public BigDecimal checkIn(String maVeXe) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String month;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT thang_dang_ky FROM VE_XE_THANG WHERE ma_ve_xe=? FOR UPDATE")) {
                    ps.setString(1, maVeXe);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Không tìm thấy vé xe.");
                        month = rs.getString(1);
                    }
                }
                // Không cho gửi mới bằng vé đã hết hạn / chưa tới kỳ.
                if (!YearMonth.now().toString().equals(month))
                    throw new SQLException("Vé " + maVeXe + " không có hiệu lực trong tháng hiện tại.");
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT 1 FROM LUOT_GUI_XE WHERE ma_ve_xe=? AND thoi_gian_lay IS NULL LIMIT 1")) {
                    ps.setString(1, maVeXe);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw new SQLException("Xe đang trong bãi. Hãy xác nhận lấy trước khi gửi lần nữa.");
                    }
                }
                BigDecimal fee = countDay(c, maVeXe, LocalDate.now()) >= 2
                        ? PHI_VUOT_LUOT : BigDecimal.ZERO;
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO LUOT_GUI_XE
                        (ma_ve_xe, thoi_gian_gui, thoi_gian_lay, tien_phat_sinh)
                        VALUES (?, ?, NULL, ?)
                        """)) {
                    ps.setString(1, maVeXe);
                    ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setBigDecimal(3, fee);
                    ps.executeUpdate();
                }
                c.commit();
                return fee;
            } catch (SQLException | RuntimeException ex) { c.rollback(); throw ex; }
        }
    }

    /** Kết thúc đúng một lượt đang gửi. Cho lấy cả xe có vé tháng trước. */
    public void checkOut(int idLuot) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     UPDATE LUOT_GUI_XE SET thoi_gian_lay=NOW()
                     WHERE id_luot=? AND thoi_gian_lay IS NULL
                     """)) {
            ps.setInt(1, idLuot);
            if (ps.executeUpdate() != 1)
                throw new SQLException("Lượt gửi không tồn tại hoặc xe đã được lấy trước đó.");
        }
    }

    private int countDay(Connection c, String maVeXe, LocalDate date) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COUNT(*) FROM LUOT_GUI_XE
                WHERE ma_ve_xe=? AND thoi_gian_gui>=? AND thoi_gian_gui<?
                """)) {
            ps.setString(1, maVeXe);
            ps.setTimestamp(2, Timestamp.valueOf(date.atStartOfDay()));
            ps.setTimestamp(3, Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }
}
