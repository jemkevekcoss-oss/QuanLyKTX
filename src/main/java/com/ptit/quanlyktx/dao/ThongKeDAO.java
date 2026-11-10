package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.DoanhThuDichVuRow;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Báo cáo doanh thu phát sinh theo tháng. Chỉ SELECT, không thay đổi dữ liệu. */
public class ThongKeDAO {

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

    /** Cận trên loại trừ: lấy đủ giờ/phút/giây cuối tháng và không lấy tháng sau. */
    public List<DoanhThuDichVuRow> findDoanhThuTheoThang(YearMonth thang) throws SQLException {
        if (thang == null) throw new IllegalArgumentException("Tháng không được rỗng");
        LocalDate tuNgay = thang.atDay(1);
        LocalDate truocNgay = thang.plusMonths(1).atDay(1);
        List<DoanhThuDichVuRow> ketQua = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DOANH_THU)) {
            ps.setDate(1, Date.valueOf(tuNgay));
            ps.setDate(2, Date.valueOf(truocNgay));
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

    /** Để khi mở lần đầu hiện tháng gần nhất có giao dịch thay vì một bảng toàn 0. */
    public YearMonth findThangGanNhatCoSuDung() throws SQLException {
        String sql = "SELECT MAX(thoi_gian_su_dung) AS ngay_moi_nhat FROM SU_DUNG_DICH_VU";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getTimestamp("ngay_moi_nhat") != null) {
                return YearMonth.from(rs.getTimestamp("ngay_moi_nhat").toLocalDateTime());
            }
            return null;
        }
    }
}
