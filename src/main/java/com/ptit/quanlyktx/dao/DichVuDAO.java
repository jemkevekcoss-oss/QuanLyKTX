package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.model.DichVu;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO cho bảng DICH_VU. Không chứa xử lý JavaFX. */
public class DichVuDAO {
    public List<DichVu> findAll() throws SQLException {
        String sql = "SELECT ma_dv, ten_dv, don_gia, don_vi_tinh FROM DICH_VU ORDER BY ma_dv";
        List<DichVu> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new DichVu(rs.getString("ma_dv"), rs.getString("ten_dv"),
                        rs.getBigDecimal("don_gia"), rs.getString("don_vi_tinh")));
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
        // Không xóa dịch vụ đã phát sinh giao dịch, kể cả khi FK bị khai báo CASCADE.
        // Khóa bản ghi cha trong transaction để tránh chèn đồng thời khi đang xóa.
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement lock = conn.prepareStatement(
                        "SELECT ma_dv FROM DICH_VU WHERE ma_dv=? FOR UPDATE")) {
                    lock.setString(1, maDV);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) { conn.rollback(); return false; }
                    }
                }
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT 1 FROM SU_DUNG_DICH_VU WHERE ma_dv=? LIMIT 1")) {
                    check.setString(1, maDV);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            throw new SQLIntegrityConstraintViolationException(
                                "Dịch vụ đã có lịch sử sử dụng, không được xóa. "
                                + "Hãy giữ lại để bảo toàn dữ liệu hóa đơn.");
                        }
                    }
                }
                int changed;
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM DICH_VU WHERE ma_dv=?")) {
                    ps.setString(1, maDV);
                    changed = ps.executeUpdate();
                }
                conn.commit();
                return changed == 1;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void fill(PreparedStatement ps, DichVu dv) throws SQLException {
        ps.setString(1, dv.getMaDV());
        ps.setString(2, dv.getTenDV());
        ps.setBigDecimal(3, dv.getDonGia());
        ps.setString(4, dv.getDonViTinh());
    }
}
