package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.HoaDonRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Truy vấn 1: tổng chi phí KTX theo tháng. Không ghi/sửa bảng dữ liệu. */
public class HoaDonDAO {

    private static final String SQL = """
        SELECT sv.ma_sv, sv.ho_ten, sv.lop, sv.so_phong,
               p.don_gia AS tien_phong,
               COALESCE(dv.tien_dich_vu, 0) AS tien_dich_vu,
               COALESCE(vx.tien_ve_xe_thang, 0) AS tien_ve_xe_thang,
               COALESCE(px.tien_gui_xe_phat_sinh, 0) AS tien_gui_xe_phat_sinh,
               (p.don_gia + COALESCE(dv.tien_dich_vu, 0)
                + COALESCE(vx.tien_ve_xe_thang, 0)
                + COALESCE(px.tien_gui_xe_phat_sinh, 0)) AS tong_phai_tra
        FROM SINH_VIEN sv
        JOIN PHONG p ON p.so_phong = sv.so_phong
        LEFT JOIN (
            SELECT ma_sv, SUM(thanh_tien) AS tien_dich_vu
            FROM SU_DUNG_DICH_VU
            WHERE thoi_gian_su_dung >= ? AND thoi_gian_su_dung < ?
            GROUP BY ma_sv
        ) dv ON dv.ma_sv = sv.ma_sv
        LEFT JOIN (
            SELECT ma_sv, SUM(phi_hang_thang) AS tien_ve_xe_thang
            FROM VE_XE_THANG
            WHERE thang_dang_ky = ?
            GROUP BY ma_sv
        ) vx ON vx.ma_sv = sv.ma_sv
        LEFT JOIN (
            SELECT v.ma_sv, SUM(l.tien_phat_sinh) AS tien_gui_xe_phat_sinh
            FROM LUOT_GUI_XE l
            JOIN VE_XE_THANG v ON v.ma_ve_xe = l.ma_ve_xe
            WHERE l.thoi_gian_lay >= ? AND l.thoi_gian_lay < ?
            GROUP BY v.ma_sv
        ) px ON px.ma_sv = sv.ma_sv
        WHERE sv.ngay_vao_o < ?
          AND (? IS NULL OR sv.ma_sv = ?)
        ORDER BY sv.ma_sv ASC
        """;

    /** maSV == null: tất cả SV; khác null: một SV. */
    public List<HoaDonRow> findByMonth(YearMonth month, String maSV) throws SQLException {
        if (month == null) throw new IllegalArgumentException("Thiếu tháng cần thống kê");
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime untilExclusive = month.plusMonths(1).atDay(1).atStartOfDay();

        List<HoaDonRow> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(untilExclusive));
            ps.setString(3, month.toString()); // YYYY-MM
            ps.setTimestamp(4, Timestamp.valueOf(from));
            ps.setTimestamp(5, Timestamp.valueOf(untilExclusive));
            ps.setTimestamp(6, Timestamp.valueOf(untilExclusive));
            if (maSV == null || maSV.isBlank()) {
                ps.setNull(7, Types.VARCHAR);
                ps.setNull(8, Types.VARCHAR);
            } else {
                ps.setString(7, maSV);
                ps.setString(8, maSV);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new HoaDonRow(
                        rs.getString("ma_sv"),
                        rs.getString("ho_ten"),
                        rs.getString("lop"),
                        rs.getString("so_phong"),
                        rs.getBigDecimal("tien_phong"),
                        rs.getBigDecimal("tien_dich_vu"),
                        rs.getBigDecimal("tien_ve_xe_thang"),
                        rs.getBigDecimal("tien_gui_xe_phat_sinh"),
                        rs.getBigDecimal("tong_phai_tra")
                    ));
                }
            }
        }
        return result;
    }
}
