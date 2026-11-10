package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.KhachRow;
import java.sql.*;
import java.util.*;

/** Hồ sơ khách: chỉ làm việc với KHACH_THAM (không chứa ma_sv/ngay_den_choi). */
public class KhachThamDAO {
    public List<KhachRow> findAll() throws SQLException {
        String sql = "SELECT id_khach, so_cmt, ho_ten, ngay_sinh FROM KHACH_THAM ORDER BY id_khach";
        List<KhachRow> items = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                java.sql.Date ngay = r.getDate("ngay_sinh");
                items.add(new KhachRow(r.getInt("id_khach"), r.getString("so_cmt"),
                        r.getString("ho_ten"), ngay == null ? null : ngay.toLocalDate()));
            }
        }
        return items;
    }
    public void insert(KhachRow k) throws SQLException {
        String sql = "INSERT INTO KHACH_THAM(so_cmt,ho_ten,ngay_sinh) VALUES(?,?,?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            setFields(p, k); p.executeUpdate();
        }
    }
    public void update(KhachRow k) throws SQLException {
        String sql = "UPDATE KHACH_THAM SET so_cmt=?,ho_ten=?,ngay_sinh=? WHERE id_khach=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            setFields(p, k); p.setInt(4, k.idKhach());
            if (p.executeUpdate() != 1) throw new SQLException("Không tìm thấy khách cần sửa.");
        }
    }
    public void delete(int id) throws SQLException {
        // FK ON DELETE RESTRICT sẽ không cho xóa khách đã có lượt thăm.
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM KHACH_THAM WHERE id_khach=?")) {
            p.setInt(1, id);
            if (p.executeUpdate() != 1) throw new SQLException("Không tìm thấy khách cần xóa.");
        }
    }
    private static void setFields(PreparedStatement p, KhachRow k) throws SQLException {
        p.setString(1, k.soCmt()); p.setString(2, k.hoTen());
        if (k.ngaySinh() == null) p.setNull(3, Types.DATE);
        else p.setDate(3, java.sql.Date.valueOf(k.ngaySinh()));
    }
}
