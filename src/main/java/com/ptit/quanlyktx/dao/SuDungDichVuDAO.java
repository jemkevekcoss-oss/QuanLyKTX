package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.ChiPhiDichVuRow;
import com.ptit.quanlyktx.dto.LichSuDichVuRow;
import com.ptit.quanlyktx.model.SuDungDichVu;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Quản lý các giao dịch và truy vấn 2: tổng hợp tiền từng dịch vụ theo SV. */
public class SuDungDichVuDAO {
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
                Timestamp ts = rs.getTimestamp("thoi_gian_su_dung");
                SuDungDichVu sd = new SuDungDichVu(
                    rs.getInt("id_su_dung"), rs.getString("ma_sv"), rs.getString("ma_dv"),
                    ts == null ? null : ts.toLocalDateTime(), rs.getInt("so_luong"),
                    rs.getBigDecimal("thanh_tien"));
                result.add(new LichSuDichVuRow(sd, rs.getString("ho_ten"),
                        rs.getString("ten_dv"), rs.getString("don_vi_tinh")));
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

    /** Lọc từ 00:00 ngày đầu đến trước 00:00 ngày sau ngày cuối. */
    public List<ChiPhiDichVuRow> thongKeSuDung(LocalDate from, LocalDate to) throws SQLException {
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
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new ChiPhiDichVuRow(rs.getString("ma_sv"), rs.getString("ho_ten"),
                        rs.getString("ten_dv"), rs.getString("don_vi_tinh"),
                        rs.getLong("tong_so_luong"), rs.getBigDecimal("tong_tien")));
                }
            }
        }
        return result;
    }

    private void bindFields(PreparedStatement ps, SuDungDichVu sd) throws SQLException {
        ps.setString(1, sd.getMaSV());
        ps.setString(2, sd.getMaDV());
        ps.setTimestamp(3, Timestamp.valueOf(sd.getThoiGianSuDung()));
        ps.setInt(4, sd.getSoLuong());
        ps.setBigDecimal(5, sd.getThanhTien());
    }
}
