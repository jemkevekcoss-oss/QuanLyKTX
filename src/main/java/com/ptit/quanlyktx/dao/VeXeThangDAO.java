package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.VeXeThangRow;

import java.math.BigDecimal;
import java.sql.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Quản lý VE_XE_THANG, dùng bảng SINH_VIEN để hiển thị tên. */
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
        // MySQL DATE -> LocalDate; vé cũ vẫn có thể có giá trị NULL.
        java.sql.Date sqlDate = rs.getDate("ngay_dang_ky");
        return new VeXeThangRow(
                rs.getString("ma_ve_xe"), rs.getString("bien_so_xe"),
                rs.getString("loai_xe"), rs.getString("ma_sv"),
                rs.getString("ho_ten"), rs.getString("lop"), rs.getString("so_phong"),
                rs.getString("thang_dang_ky"),
                sqlDate == null ? null : sqlDate.toLocalDate(),
                rs.getBigDecimal("phi_hang_thang"));
    }

    public List<VeXeThangRow> findByMonth(YearMonth month) throws SQLException {
        String sql = BASE + " WHERE v.thang_dang_ky = ? ORDER BY v.ma_ve_xe";
        List<VeXeThangRow> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(readRow(rs));
            }
        }
        return result;
    }

    public VeXeThangRow findByPlate(String plate, YearMonth month) throws SQLException {
        String sql = BASE + " WHERE UPPER(v.bien_so_xe) = UPPER(?) " +
                "AND v.thang_dang_ky = ? ORDER BY v.ma_ve_xe LIMIT 1";
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
             PreparedStatement ps = c.prepareStatement("SELECT ma_sv, ho_ten FROM SINH_VIEN ORDER BY ma_sv");
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

    /**
     * Tự tạo mã VXyyMM_nn. GET_LOCK tuần tự hóa đăng ký tháng trên những kết nối
     * cùng dùng ứng dụng; để bảo vệ tuyệt đối, DB vẫn cần UNIQUE theo (tháng, biển).
     */
    public String register(String plate, String type, String maSV, YearMonth month) throws SQLException {
        plate = plate.trim().toUpperCase(Locale.ROOT);
        type = type.trim();
        validate(plate, type, maSV, month);
        String prefix = "VX" + month.format(PREFIX_MONTH) + "_";
        String lockName = "ktx_ve_" + month.toString();
        try (Connection c = DatabaseConnection.getConnection()) {
            if (!acquireLock(c, lockName)) throw new SQLException("Hệ thống đang tạo mã vé. Hãy thử lại.");
            try {
                c.setAutoCommit(false);
                try {
                    lockStudent(c, maSV);
                    ensureAllowed(c, plate, maSV, month, null);
                    String code = nextCode(c, prefix);
                    try (PreparedStatement ps = c.prepareStatement("""
                            INSERT INTO VE_XE_THANG
                            (ma_ve_xe, bien_so_xe, loai_xe, ma_sv, thang_dang_ky, ngay_dang_ky, phi_hang_thang)
                            VALUES (?, ?, ?, ?, ?, CURRENT_DATE(), ?)
                            """)) {
                        ps.setString(1, code);
                        ps.setString(2, plate);
                        ps.setString(3, type);
                        ps.setString(4, maSV);
                        ps.setString(5, month.toString());
                        ps.setBigDecimal(6, GIA_VE_THANG);
                        ps.executeUpdate();
                    }
                    c.commit();
                    return code;
                } catch (SQLException | RuntimeException ex) {
                    c.rollback();
                    throw ex;
                }
            } finally { releaseLock(c, lockName); }
        }
    }

    public void edit(String ticketId, String plate, String type, String maSV, YearMonth month)
            throws SQLException {
        plate = plate.trim().toUpperCase(Locale.ROOT);
        type = type.trim();
        validate(plate, type, maSV, month);
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Đã có lượt gửi: không đổi chủ, biển số hay tháng để giữ lịch sử hóa đơn.
                if (hasTrips(c, ticketId))
                    throw new SQLException("Vé đã phát sinh lượt gửi. Không cho sửa thông tin lịch sử.");
                lockStudent(c, maSV);
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
                        throw new SQLException("Không tìm thấy vé hoặc tháng đăng ký đã thay đổi.");
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) { c.rollback(); throw ex; }
        }
    }

    public void delete(String ticketId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (hasTrips(c, ticketId))
                throw new SQLException("Vé đã có lượt gửi/lấy xe nên không thể xóa.");
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM VE_XE_THANG WHERE ma_ve_xe=?")) {
                ps.setString(1, ticketId);
                if (ps.executeUpdate() != 1) throw new SQLException("Vé không còn tồn tại.");
            }
        }
    }

    public String renew(VeXeThangRow old, YearMonth month) throws SQLException {
        if (!month.isAfter(YearMonth.parse(old.thangDangKy())))
            throw new SQLException("Tháng gia hạn phải sau tháng đăng ký của vé cũ.");
        return register(old.bienSoXe(), old.loaiXe(), old.maSV(), month);
    }

    private void validate(String plate, String type, String maSV, YearMonth month) throws SQLException {
        if (plate.isBlank() || type.isBlank() || maSV == null || maSV.isBlank() || month == null)
            throw new SQLException("Hãy nhập đủ biển số, loại xe, sinh viên và tháng.");
        if (plate.length() > 20 || type.length() > 50 || maSV.length() > 20)
            throw new SQLException("Dữ liệu vượt độ dài quy định trong CSDL.");
    }

    private void lockStudent(Connection c, String maSV) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT ma_sv FROM SINH_VIEN WHERE ma_sv=? FOR UPDATE")) {
            ps.setString(1, maSV);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Không tìm thấy sinh viên " + maSV);
            }
        }
    }

    private void ensureAllowed(Connection c, String plate, String maSV,
                               YearMonth month, String exceptId) throws SQLException {
        String except = exceptId == null ? "" : exceptId;
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COUNT(*) AS n FROM VE_XE_THANG
                WHERE ma_sv=? AND thang_dang_ky=? AND ma_ve_xe<>?
                """)) {
            ps.setString(1, maSV);
            ps.setString(2, month.toString());
            ps.setString(3, except);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) >= 2)
                    throw new SQLException("Sinh viên đã có 2 vé tháng " + month + ". Không được đăng ký thêm.");
            }
        }
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COUNT(*) FROM VE_XE_THANG
                WHERE UPPER(bien_so_xe)=UPPER(?) AND thang_dang_ky=? AND ma_ve_xe<>?
                """)) {
            ps.setString(1, plate);
            ps.setString(2, month.toString());
            ps.setString(3, except);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0)
                    throw new SQLException("Biển số đã có vé trong tháng " + month + ".");
            }
        }
    }

    private String nextCode(Connection c, String prefix) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(MAX(CAST(SUBSTRING(ma_ve_xe, 8) AS UNSIGNED)), 0)
                FROM VE_XE_THANG WHERE LEFT(ma_ve_xe, 7)=?
                """)) {
            ps.setString(1, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int next = rs.getInt(1) + 1;
                String code = prefix + String.format(Locale.ROOT, "%02d", next);
                if (code.length() > 20) throw new SQLException("Mã vé đã vượt giới hạn VARCHAR(20).");
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

    private boolean acquireLock(Connection c, String key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT GET_LOCK(?, 5)")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) == 1; }
        }
    }

    private void releaseLock(Connection c, String key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            ps.setString(1, key);
            ps.executeQuery().close();
        }
    }
}
