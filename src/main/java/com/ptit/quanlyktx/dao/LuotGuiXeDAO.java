package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.LuotGuiXeRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Lich su gui/lay xe SQLite; giu nguyen API cua GuiXeController. */
public class LuotGuiXeDAO {
    private static final BigDecimal PHI_VUOT_LUOT = new BigDecimal("3000.00");
    private static final DateTimeFormatter SQLITE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String BASE = """
            SELECT l.id_luot, l.ma_ve_xe, v.bien_so_xe, v.ma_sv, s.ho_ten,
                   l.thoi_gian_gui, l.thoi_gian_lay, l.tien_phat_sinh
              FROM LUOT_GUI_XE l
              JOIN VE_XE_THANG v ON v.ma_ve_xe = l.ma_ve_xe
              JOIN SINH_VIEN s ON s.ma_sv = v.ma_sv
            """;

    private LocalDateTime parseTime(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDateTime.parse(raw.replace(' ', 'T'));
        } catch (DateTimeParseException ex) {
            throw new SQLException("Thoi gian khong hop le tai cot " + column + ": " + raw, ex);
        }
    }

    private LuotGuiXeRow fromRow(ResultSet rs) throws SQLException {
        return new LuotGuiXeRow(
                rs.getInt("id_luot"), rs.getString("ma_ve_xe"),
                rs.getString("bien_so_xe"), rs.getString("ma_sv"), rs.getString("ho_ten"),
                parseTime(rs, "thoi_gian_gui"), parseTime(rs, "thoi_gian_lay"),
                rs.getBigDecimal("tien_phat_sinh"));
    }

    public List<LuotGuiXeRow> findOpen() throws SQLException {
        List<LuotGuiXeRow> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE +
                     " WHERE l.thoi_gian_lay IS NULL ORDER BY l.thoi_gian_gui" );
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(fromRow(rs));
        }
        return rows;
    }

    public List<LuotGuiXeRow> findHistory(LocalDate from, LocalDate to) throws SQLException {
        if (from == null || to == null || from.isAfter(to))
            throw new SQLException("Khoang ngay loc lich su khong hop le.");
        List<LuotGuiXeRow> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + """
                     WHERE l.thoi_gian_gui >= ? AND l.thoi_gian_gui < ?
                     ORDER BY l.thoi_gian_gui ASC, l.id_luot ASC
                     """)) {
            ps.setString(1, startOfDay(from));
            ps.setString(2, startOfDay(to.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(fromRow(rs));
            }
        }
        return result;
    }

    public int countToday(String maVeXe) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            return countDay(c, maVeXe, LocalDate.now());
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
            ps.setString(1, startOfDay(today));
            ps.setString(2, startOfDay(today.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    /** Hai luot dau/xe/ngay mien phi, tu luot thu 3 phi 3.000d. */
    public BigDecimal checkIn(String maVeXe) throws SQLException {
        if (maVeXe == null || maVeXe.isBlank())
            throw new SQLException("Chua chon ve xe.");
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        try (Connection c = DatabaseConnection.getConnection()) {
            // Khoa ghi truoc khi kiem tra ve, xe dang o bai va dem luot.
            beginImmediate(c);
            try {
                String month;
                String registrationDate;
                try (PreparedStatement ps = c.prepareStatement("""
                        SELECT thang_dang_ky, ngay_dang_ky
                        FROM VE_XE_THANG WHERE ma_ve_xe=?
                        """)) {
                    ps.setString(1, maVeXe);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Khong tim thay ve xe.");
                        month = rs.getString("thang_dang_ky");
                        registrationDate = rs.getString("ngay_dang_ky");
                    }
                }
                if (!YearMonth.from(today).toString().equals(month))
                    throw new SQLException("Ve " + maVeXe + " khong co hieu luc trong thang hien tai.");
                if (registrationDate != null && !registrationDate.isBlank()) {
                    LocalDate regDate;
                    try {
                        regDate = LocalDate.parse(registrationDate.substring(0, 10));
                    } catch (DateTimeParseException | IndexOutOfBoundsException ex) {
                        throw new SQLException("Ngay dang ky tren ve khong hop le: " + registrationDate, ex);
                    }
                    if (today.isBefore(regDate))
                        throw new SQLException("Ve chua co hieu luc truoc ngay dang ky " + regDate + ".");
                }
                try (PreparedStatement ps = c.prepareStatement("""
                        SELECT 1 FROM LUOT_GUI_XE
                        WHERE ma_ve_xe=? AND thoi_gian_lay IS NULL LIMIT 1
                        """)) {
                    ps.setString(1, maVeXe);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next())
                            throw new SQLException("Xe dang trong bai. Hay xac nhan lay truoc khi gui tiep.");
                    }
                }

                BigDecimal expected = countDay(c, maVeXe, today) >= 2
                        ? PHI_VUOT_LUOT : BigDecimal.ZERO;
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO LUOT_GUI_XE
                            (ma_ve_xe, thoi_gian_gui, thoi_gian_lay, tien_phat_sinh)
                        VALUES (?, ?, NULL, 0)
                        """)) {
                    ps.setString(1, maVeXe);
                    ps.setString(2, now.format(SQLITE_TIME));
                    ps.executeUpdate();
                }

                // Trigger SQLite AFTER INSERT tinh phi: phai doc lai gia tri that
                // sau khi trigger da chay, khong tra ve gia tri gia dinh.
                BigDecimal actual;
                try (Statement s = c.createStatement();
                     ResultSet rs = s.executeQuery("""
                             SELECT tien_phat_sinh FROM LUOT_GUI_XE
                             WHERE id_luot=last_insert_rowid()
                             """)) {
                    if (!rs.next()) throw new SQLException("Khong doc duoc phi cua luot gui moi.");
                    actual = rs.getBigDecimal(1);
                }
                if (actual == null || actual.compareTo(expected) != 0)
                    throw new SQLException("Phi gui xe khong khop quy tac 2 luot mien phi. Kiem tra trigger SQLite.");
                commit(c);
                return actual;
            } catch (SQLException | RuntimeException ex) {
                rollback(c, ex);
                throw ex;
            }
        }
    }

    /** Cho lay xe ke ca neu ve thang cu da het han. */
    public void checkOut(int idLuot) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     UPDATE LUOT_GUI_XE SET thoi_gian_lay=?
                     WHERE id_luot=? AND thoi_gian_lay IS NULL
                     """)) {
            ps.setString(1, LocalDateTime.now().format(SQLITE_TIME));
            ps.setInt(2, idLuot);
            if (ps.executeUpdate() != 1)
                throw new SQLException("Luot gui khong ton tai hoac xe da duoc lay truoc do.");
        }
    }

    private int countDay(Connection c, String maVeXe, LocalDate date) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COUNT(*) FROM LUOT_GUI_XE
                WHERE ma_ve_xe=? AND thoi_gian_gui>=? AND thoi_gian_gui<?
                """)) {
            ps.setString(1, maVeXe);
            ps.setString(2, startOfDay(date));
            ps.setString(3, startOfDay(date.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static String startOfDay(LocalDate date) {
        return date.atStartOfDay().format(SQLITE_TIME);
    }

    // SQL transaction tranh c.commit()/c.rollback() khi JDBC autoCommit=true.
    private static void beginImmediate(Connection c) throws SQLException { executeTx(c, "BEGIN IMMEDIATE"); }
    private static void commit(Connection c) throws SQLException { executeTx(c, "COMMIT"); }
    private static void executeTx(Connection c, String command) throws SQLException {
        try (Statement s = c.createStatement()) { s.execute(command); }
    }
    private static void rollback(Connection c, Throwable original) {
        try { executeTx(c, "ROLLBACK"); }
        catch (SQLException ex) { original.addSuppressed(ex); }
    }
}
