package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.ChiPhiDichVuRow;
import com.ptit.quanlyktx.dto.LichSuDichVuRow;
import com.ptit.quanlyktx.model.SuDungDichVu;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Quan ly lich su su dung dich vu va thong ke chi phi theo khoang ngay. */
public class SuDungDichVuDAO {

    // SQLite luu thoi diem duoi dang TEXT: yyyy-MM-dd HH:mm:ss.
    private static final DateTimeFormatter DB_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<LichSuDichVuRow> findAll() throws SQLException {
        String sql = """
            SELECT sd.id_su_dung, sd.ma_sv, sd.ma_dv, sd.thoi_gian_su_dung,
                   sd.so_luong, sd.thanh_tien, sv.ho_ten, dv.ten_dv, dv.don_vi_tinh
            FROM SU_DUNG_DICH_VU sd
            JOIN SINH_VIEN sv ON sv.ma_sv = sd.ma_sv
            JOIN DICH_VU dv ON dv.ma_dv = sd.ma_dv
            ORDER BY sd.thoi_gian_su_dung ASC, sd.id_su_dung ASC
            """;
        List<LichSuDichVuRow> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String rawTime = rs.getString("thoi_gian_su_dung");
                LocalDateTime time = rawTime == null
                        ? null : LocalDateTime.parse(rawTime.trim().replace(' ', 'T'));
                SuDungDichVu sd = new SuDungDichVu(
                        rs.getInt("id_su_dung"),
                        rs.getString("ma_sv"),
                        rs.getString("ma_dv"),
                        time,
                        rs.getInt("so_luong"),
                        rs.getBigDecimal("thanh_tien"));
                result.add(new LichSuDichVuRow(sd,
                        rs.getString("ho_ten"),
                        rs.getString("ten_dv"),
                        rs.getString("don_vi_tinh")));
            }
        }
        return result;
    }

    public boolean insert(SuDungDichVu sd) throws SQLException {
        String sql = """
            INSERT INTO SU_DUNG_DICH_VU
                (ma_sv, ma_dv, thoi_gian_su_dung, so_luong, thanh_tien)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFields(ps, sd);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean update(SuDungDichVu sd) throws SQLException {
        String sql = """
            UPDATE SU_DUNG_DICH_VU
            SET ma_sv=?, ma_dv=?, thoi_gian_su_dung=?, so_luong=?, thanh_tien=?
            WHERE id_su_dung=?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFields(ps, sd);
            ps.setInt(6, sd.getIdSuDung());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM SU_DUNG_DICH_VU WHERE id_su_dung=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    /** Loc tu dau ngay bat dau den truoc 00:00 ngay sau ngay ket thuc. */
    public List<ChiPhiDichVuRow> thongKeSuDung(LocalDate from, LocalDate to)
            throws SQLException {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
        }
        String sql = """
            SELECT sv.ma_sv, sv.ho_ten, dv.ma_dv, dv.ten_dv, dv.don_vi_tinh,
                   SUM(sd.so_luong) AS tong_so_luong,
                   SUM(sd.thanh_tien) AS tong_tien
            FROM SU_DUNG_DICH_VU sd
            JOIN SINH_VIEN sv ON sv.ma_sv = sd.ma_sv
            JOIN DICH_VU dv ON dv.ma_dv = sd.ma_dv
            WHERE sd.thoi_gian_su_dung >= ? AND sd.thoi_gian_su_dung < ?
            GROUP BY sv.ma_sv, sv.ho_ten, dv.ma_dv, dv.ten_dv, dv.don_vi_tinh
            ORDER BY sv.ma_sv, dv.ten_dv, dv.ma_dv
            """;
        List<ChiPhiDichVuRow> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Bind cung dinh dang voi TEXT datetime da luu trong SQLite.
            ps.setString(1, from.atStartOfDay().format(DB_DATETIME));
            ps.setString(2, to.plusDays(1).atStartOfDay().format(DB_DATETIME));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new ChiPhiDichVuRow(
                            rs.getString("ma_sv"),
                            rs.getString("ho_ten"),
                            rs.getString("ten_dv"),
                            rs.getString("don_vi_tinh"),
                            rs.getLong("tong_so_luong"),
                            rs.getBigDecimal("tong_tien")));
                }
            }
        }
        return result;
    }

    private void bindFields(PreparedStatement ps, SuDungDichVu sd) throws SQLException {
        if (sd.getThoiGianSuDung() == null) {
            throw new SQLException("Thời gian sử dụng dịch vụ không được để trống.");
        }
        ps.setString(1, sd.getMaSV());
        ps.setString(2, sd.getMaDV());
        ps.setString(3, sd.getThoiGianSuDung().format(DB_DATETIME));
        ps.setInt(4, sd.getSoLuong());
        ps.setBigDecimal(5, sd.getThanhTien());
    }
}
