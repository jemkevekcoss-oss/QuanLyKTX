package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.model.DichVu;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Truy cap bang DICH_VU qua SQLite JDBC; khong xu ly giao dien. */
public class DichVuDAO {

    public List<DichVu> findAll() throws SQLException {
        String sql = "SELECT ma_dv, ten_dv, don_gia, don_vi_tinh FROM DICH_VU ORDER BY ma_dv";
        List<DichVu> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new DichVu(
                        rs.getString("ma_dv"),
                        rs.getString("ten_dv"),
                        rs.getBigDecimal("don_gia"),
                        rs.getString("don_vi_tinh")));
            }
        }
        return result;
    }

    public boolean insert(DichVu dv) throws SQLException {
        String sql = "INSERT INTO DICH_VU (ma_dv, ten_dv, don_gia, don_vi_tinh) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            fill(ps, dv);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean update(DichVu dv) throws SQLException {
        String sql = "UPDATE DICH_VU SET ten_dv=?, don_gia=?, don_vi_tinh=? WHERE ma_dv=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dv.getTenDV());
            ps.setBigDecimal(2, dv.getDonGia());
            ps.setString(3, dv.getDonViTinh());
            ps.setString(4, dv.getMaDV());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(String maDV) throws SQLException {
        // SQLite khong co SELECT ... FOR UPDATE. Dung mot DELETE co dieu kien:
        // chi xoa dich vu neu chua tung phat sinh giao dich su dung.
        String deleteSql = """
            DELETE FROM DICH_VU
            WHERE ma_dv = ?
              AND NOT EXISTS (
                  SELECT 1 FROM SU_DUNG_DICH_VU WHERE ma_dv = ?
              )
            """;
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setString(1, maDV);
                ps.setString(2, maDV);
                if (ps.executeUpdate() == 1) {
                    return true;
                }
            }

            // Neu khong xoa duoc: phan biet ma khong ton tai va ma da co lich su.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM DICH_VU WHERE ma_dv=?")) {
                ps.setString(1, maDV);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                }
            }
            throw new SQLIntegrityConstraintViolationException(
                    "Dịch vụ đã có lịch sử sử dụng, không được xóa. "
                    + "Hãy giữ lại để bảo toàn dữ liệu hóa đơn.");
        }
    }

    private void fill(PreparedStatement ps, DichVu dv) throws SQLException {
        ps.setString(1, dv.getMaDV());
        ps.setString(2, dv.getTenDV());
        ps.setBigDecimal(3, dv.getDonGia());
        ps.setString(4, dv.getDonViTinh());
    }
}
