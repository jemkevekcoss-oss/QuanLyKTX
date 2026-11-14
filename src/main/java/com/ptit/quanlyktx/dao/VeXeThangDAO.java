package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.VeXeThangRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Quan ly ve thang SQLite; giu nguyen API cua GuiXeController. */
public class VeXeThangDAO {
    public static final BigDecimal GIA_VE_THANG = new BigDecimal("100000.00");
    private static final DateTimeFormatter PREFIX_MONTH = DateTimeFormatter.ofPattern("yyMM");
    private static final String BASE = """
            SELECT v.ma_ve_xe, v.bien_so_xe, v.loai_xe, v.ma_sv,
                   s.ho_ten, s.lop, s.so_phong,
                   v.thang_dang_ky, v.ngay_dang_ky, v.phi_hang_thang
              FROM VE_XE_THANG v
              JOIN SINH_VIEN s ON s.ma_sv = v.ma_sv
            """;

    public record SinhVienRef(String maSV, String hoTen) {
        @Override public String toString() { return maSV + " – " + hoTen; }
    }

    private VeXeThangRow readRow(ResultSet rs) throws SQLException {
        String rawDate = rs.getString("ngay_dang_ky");
        LocalDate ngayDangKy = null;
        if (rawDate != null && !rawDate.isBlank()) {
            try {
                ngayDangKy = LocalDate.parse(rawDate.substring(0, 10));
            } catch (DateTimeParseException | IndexOutOfBoundsException ex) {
                throw new SQLException("Ngay dang ky ve khong hop le: " + rawDate, ex);
            }
        }
        return new VeXeThangRow(
                rs.getString("ma_ve_xe"), rs.getString("bien_so_xe"),
                rs.getString("loai_xe"), rs.getString("ma_sv"),
                rs.getString("ho_ten"), rs.getString("lop"), rs.getString("so_phong"),
                rs.getString("thang_dang_ky"), ngayDangKy,
                rs.getBigDecimal("phi_hang_thang"));
    }

    public List<VeXeThangRow> findByMonth(YearMonth month) throws SQLException {
        String sql = BASE + " WHERE v.thang_dang_ky=? ORDER BY v.ma_ve_xe";
        List<VeXeThangRow> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(readRow(rs));
            }
        }
        return rows;
    }

    public VeXeThangRow findByPlate(String plate, YearMonth month) throws SQLException {
        if (plate == null || month == null) return null;
        String sql = BASE + " WHERE UPPER(v.bien_so_xe)=UPPER(?) "
                + "AND v.thang_dang_ky=? ORDER BY v.ma_ve_xe LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, plate.trim());
            ps.setString(2, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readRow(rs) : null;
            }
        }
    }

    public List<SinhVienRef> findStudents() throws SQLException {
        List<SinhVienRef> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT ma_sv, ho_ten FROM SINH_VIEN ORDER BY ma_sv");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(new SinhVienRef(rs.getString(1), rs.getString(2)));
        }
        return result;
    }

    public List<String> findAvailableMonths() throws SQLException {
        List<String> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT DISTINCT thang_dang_ky FROM VE_XE_THANG ORDER BY thang_dang_ky DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(rs.getString(1));
        }
        return result;
    }

    /** Tao ma VXyyMM_nn, kiem tra gioi han 2 ve/thang va trung bien so. */
    public String register(String plate, String type, String maSV, YearMonth month)
            throws SQLException {
        plate = normalizePlate(plate);
        type = type == null ? "" : type.trim();
        validate(plate, type, maSV, month);
        if (month.isBefore(YearMonth.now())) {
            throw new SQLException("Khong the dang ky ve cho thang da qua: " + month);
        }
        String prefix = "VX" + month.format(PREFIX_MONTH) + "_";
        try (Connection c = DatabaseConnection.getConnection()) {
            // SQLite khong co GET_LOCK/FOR UPDATE. BEGIN IMMEDIATE khoa ghi
            // TRUOC khi dem ve va tao ma, tranh hai lan dang ky trung nhau.
            beginImmediate(c);
            try {
                ensureStudentExists(c, maSV);
                ensureAllowed(c, plate, maSV, month, null);
                String code = nextCode(c, prefix);
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO VE_XE_THANG
                          (ma_ve_xe, bien_so_xe, loai_xe, ma_sv,
                           thang_dang_ky, ngay_dang_ky, phi_hang_thang)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    ps.setString(1, code);
                    ps.setString(2, plate);
                    ps.setString(3, type);
                    ps.setString(4, maSV);
                    ps.setString(5, month.toString());
                    ps.setString(6, LocalDate.now().toString());
                    ps.setBigDecimal(7, GIA_VE_THANG);
                    ps.executeUpdate();
                }
                commit(c);
                return code;
            } catch (SQLException | RuntimeException ex) {
                rollback(c, ex);
                throw ex;
            }
        }
    }

    /** Ve da co luot gui xe thi khong thay doi thong tin de giu lich su. */
    public void edit(String ticketId, String plate, String type, String maSV, YearMonth month)
            throws SQLException {
        plate = normalizePlate(plate);
        type = type == null ? "" : type.trim();
        validate(plate, type, maSV, month);
        try (Connection c = DatabaseConnection.getConnection()) {
            beginImmediate(c);
            try {
                if (hasTrips(c, ticketId))
                    throw new SQLException("Ve da phat sinh luot gui. Khong cho sua thong tin lich su.");
                ensureStudentExists(c, maSV);
                ensureAllowed(c, plate, maSV, month, ticketId);
                try (PreparedStatement ps = c.prepareStatement("""
                        UPDATE VE_XE_THANG
                           SET bien_so_xe=?, loai_xe=?, ma_sv=?
                         WHERE ma_ve_xe=? AND thang_dang_ky=?
                        """)) {
                    ps.setString(1, plate);
                    ps.setString(2, type);
                    ps.setString(3, maSV);
                    ps.setString(4, ticketId);
                    ps.setString(5, month.toString());
                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Khong tim thay ve hoac thang dang ky da thay doi.");
                }
                commit(c);
            } catch (SQLException | RuntimeException ex) {
                rollback(c, ex);
                throw ex;
            }
        }
    }

    /** Chan xoa ve co luot gui (SQLite schema co FK ON DELETE CASCADE). */
    public void delete(String ticketId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            beginImmediate(c);
            try {
                if (hasTrips(c, ticketId))
                    throw new SQLException("Ve da co luot gui/lay xe nen khong the xoa.");
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM VE_XE_THANG WHERE ma_ve_xe=?")) {
                    ps.setString(1, ticketId);
                    if (ps.executeUpdate() != 1)
                        throw new SQLException("Ve khong con ton tai.");
                }
                commit(c);
            } catch (SQLException | RuntimeException ex) {
                rollback(c, ex);
                throw ex;
            }
        }
    }

    /** Gia han bang ve MOI, khong ghi de ve cu va cac luot gui xe cu. */
    public String renew(VeXeThangRow old, YearMonth month) throws SQLException {
        if (old == null || month == null)
            throw new SQLException("Chua chon ve va thang can gia han.");
        if (!month.isAfter(YearMonth.parse(old.thangDangKy())))
            throw new SQLException("Thang gia han phai sau thang dang ky cua ve cu.");
        return register(old.bienSoXe(), old.loaiXe(), old.maSV(), month);
    }

    private String normalizePlate(String plate) {
        return plate == null ? "" : plate.trim().toUpperCase(Locale.ROOT);
    }

    private void validate(String plate, String type, String maSV, YearMonth month)
            throws SQLException {
        if (plate.isBlank() || type.isBlank() || maSV == null || maSV.isBlank() || month == null)
            throw new SQLException("Hay nhap du bien so, loai xe, sinh vien va thang.");
        if (plate.length() > 20 || type.length() > 50 || maSV.length() > 20)
            throw new SQLException("Du lieu vuot do dai quy dinh trong CSDL.");
    }

    private void ensureStudentExists(Connection c, String maSV) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM SINH_VIEN WHERE ma_sv=? LIMIT 1")) {
            ps.setString(1, maSV);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Khong tim thay sinh vien " + maSV);
            }
        }
    }

    private void ensureAllowed(Connection c, String plate, String maSV,
                               YearMonth month, String exceptId) throws SQLException {
        String except = exceptId == null ? "" : exceptId;
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COUNT(*) FROM VE_XE_THANG
                WHERE ma_sv=? AND thang_dang_ky=? AND ma_ve_xe<>?
                """)) {
            ps.setString(1, maSV);
            ps.setString(2, month.toString());
            ps.setString(3, except);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) >= 2)
                    throw new SQLException("Sinh vien da co 2 ve thang " + month + ".");
            }
        }
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM VE_XE_THANG
                WHERE UPPER(bien_so_xe)=UPPER(?) AND thang_dang_ky=?
                  AND ma_ve_xe<>? LIMIT 1
                """)) {
            ps.setString(1, plate);
            ps.setString(2, month.toString());
            ps.setString(3, except);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new SQLException("Bien so da co ve trong thang " + month + ".");
            }
        }
    }

    private String nextCode(Connection c, String prefix) throws SQLException {
        // VX2611_ la 7 ky tu; phan so bat dau tai vi tri 8.
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(MAX(CAST(SUBSTR(ma_ve_xe, 8) AS INTEGER)), 0)
                FROM VE_XE_THANG WHERE SUBSTR(ma_ve_xe, 1, 7)=?
                """)) {
            ps.setString(1, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int next = rs.getInt(1) + 1;
                String code = prefix + String.format(Locale.ROOT, "%02d", next);
                if (code.length() > 20)
                    throw new SQLException("Ma ve da vuot gioi han 20 ky tu.");
                return code;
            }
        }
    }

    private boolean hasTrips(Connection c, String ticketId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM LUOT_GUI_XE WHERE ma_ve_xe=? LIMIT 1")) {
            ps.setString(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // Transaction quan ly bang SQL: khong goi c.commit() khi JDBC autoCommit=true.
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
