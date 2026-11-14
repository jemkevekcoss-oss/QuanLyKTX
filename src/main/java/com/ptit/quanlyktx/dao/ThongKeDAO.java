package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.DoanhThuDichVuRow;
import com.ptit.quanlyktx.dto.TopSinhVienDichVuRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Báo cáo doanh thu dịch vụ theo tháng (SQLite).
 * Chỉ đọc dữ liệu; bảng và biểu đồ dùng chung kết quả truy vấn này.
 */
public class ThongKeDAO {

    private static final DateTimeFormatter SQLITE_DATE_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    // Lọc kỳ trong điều kiện LEFT JOIN để vẫn hiển thị dịch vụ chưa phát sinh.
    // Cận trên loại trừ: không bỏ sót giao dịch ngày cuối tháng.
    private static final String SQL_DOANH_THU = """
            SELECT dv.ma_dv, dv.ten_dv, dv.don_gia, dv.don_vi_tinh,
                   COUNT(sd.id_su_dung) AS so_luot_su_dung,
                   COALESCE(SUM(sd.so_luong), 0) AS tong_so_luong,
                   COALESCE(SUM(sd.thanh_tien), 0) AS tong_doanh_thu
            FROM DICH_VU dv
            LEFT JOIN SU_DUNG_DICH_VU sd
                   ON sd.ma_dv = dv.ma_dv
                  AND sd.thoi_gian_su_dung >= ?
                  AND sd.thoi_gian_su_dung < ?
            GROUP BY dv.ma_dv, dv.ten_dv, dv.don_gia, dv.don_vi_tinh
            ORDER BY tong_doanh_thu DESC, dv.ma_dv ASC
            """;

    /** Trả về toàn bộ dịch vụ, kể cả dịch vụ không có doanh thu trong tháng. */
    public List<DoanhThuDichVuRow> findDoanhThuTheoThang(YearMonth thang) throws SQLException {
        if (thang == null) {
            throw new IllegalArgumentException("Tháng không được rỗng");
        }

        String tuNgay = thang.atDay(1).atStartOfDay().format(SQLITE_DATE_TIME);
        String denTruoc = thang.plusMonths(1).atDay(1).atStartOfDay()
                .format(SQLITE_DATE_TIME);

        List<DoanhThuDichVuRow> ketQua = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DOANH_THU)) {

            // CSDL SQLite của dự án lưu DATETIME dạng TEXT: yyyy-MM-dd HH:mm:ss.
            ps.setString(1, tuNgay);
            ps.setString(2, denTruoc);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ketQua.add(new DoanhThuDichVuRow(
                            rs.getString("ma_dv"),
                            rs.getString("ten_dv"),
                            rs.getBigDecimal("don_gia"),
                            rs.getString("don_vi_tinh"),
                            rs.getInt("so_luot_su_dung"),
                            rs.getBigDecimal("tong_so_luong"),
                            rs.getBigDecimal("tong_doanh_thu")
                    ));
                }
            }
        }
        return ketQua;
    }

    /**
     * Truy vấn bổ sung: xếp hạng người sử dụng DV trong một tháng.
     * Chỉ tính SV đã phát sinh giao dịch; doanh thu dùng giá trị thanh_tien
     * đã lưu ở từng giao dịch (không nhân với đơn giá hiện tại).
     */
    public List<TopSinhVienDichVuRow> findTopSinhVienTheoThang(
            YearMonth thang, int gioiHan, boolean theoTongTien) throws SQLException {
        if (thang == null) throw new IllegalArgumentException("Tháng không được rỗng");
        if (gioiHan != 5 && gioiHan != 10) {
            throw new IllegalArgumentException("Chỉ hỗ trợ Top 5 hoặc Top 10");
        }
        // Chuỗi ORDER BY chỉ lấy từ hai hằng do lập trình viên kiểm soát.
        String sapXep = theoTongTien
                ? "tong_tien DESC, so_luot DESC, sv.ma_sv ASC"
                : "so_luot DESC, tong_tien DESC, sv.ma_sv ASC";
        String sql = """
                SELECT sv.ma_sv, sv.ho_ten,
                       COUNT(sd.id_su_dung) AS so_luot,
                       COALESCE(SUM(sd.thanh_tien), 0) AS tong_tien
                FROM SU_DUNG_DICH_VU sd
                INNER JOIN SINH_VIEN sv ON sv.ma_sv = sd.ma_sv
                WHERE sd.thoi_gian_su_dung >= ?
                  AND sd.thoi_gian_su_dung < ?
                GROUP BY sv.ma_sv, sv.ho_ten
                """ + " ORDER BY " + sapXep + " LIMIT ?";
        String tuNgay = thang.atDay(1).atStartOfDay().format(SQLITE_DATE_TIME);
        String denTruoc = thang.plusMonths(1).atDay(1).atStartOfDay().format(SQLITE_DATE_TIME);
        List<TopSinhVienDichVuRow> ketQua = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tuNgay);
            ps.setString(2, denTruoc);
            ps.setInt(3, gioiHan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ketQua.add(new TopSinhVienDichVuRow(
                            ketQua.size() + 1,
                            rs.getString("ma_sv"),
                            rs.getString("ho_ten"),
                            rs.getInt("so_luot"),
                            rs.getBigDecimal("tong_tien")));
                }
            }
        }
        return ketQua;
    }

    /** Tháng gần nhất đã có giao dịch, dùng để chọn tháng ban đầu của màn hình. */
    public YearMonth findThangGanNhatCoSuDung() throws SQLException {
        String sql = """
                SELECT MAX(substr(thoi_gian_su_dung, 1, 7)) AS thang_gan_nhat
                FROM SU_DUNG_DICH_VU
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                return null;
            }
            String thang = rs.getString("thang_gan_nhat");
            if (thang == null || thang.isBlank()) {
                return null;
            }
            try {
                return YearMonth.parse(thang);
            } catch (DateTimeParseException ex) {
                throw new SQLException("Tháng giao dịch trong SQLite không hợp lệ: " + thang, ex);
            }
        }
    }
}
