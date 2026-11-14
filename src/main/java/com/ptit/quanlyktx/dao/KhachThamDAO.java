package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.KhachRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Ho so khach: chi lam viec voi KHACH_THAM. */
public class KhachThamDAO {

    public List<KhachRow> findAll() throws SQLException {
        String sql = "SELECT id_khach, so_cmt, ho_ten, ngay_sinh "
                   + "FROM KHACH_THAM ORDER BY id_khach";
        List<KhachRow> items = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            while (r.next()) {
                // SQLite luu DATE duoi dang chuoi YYYY-MM-DD.
                String ngay = r.getString("ngay_sinh");
                LocalDate ngaySinh = (ngay == null || ngay.isBlank())
                        ? null : LocalDate.parse(ngay);
                items.add(new KhachRow(
                        r.getInt("id_khach"),
                        r.getString("so_cmt"),
                        r.getString("ho_ten"),
                        ngaySinh));
            }
        }
        return items;
    }

    public void insert(KhachRow k) throws SQLException {
        String sql = "INSERT INTO KHACH_THAM(so_cmt, ho_ten, ngay_sinh) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            setFields(p, k);
            p.executeUpdate();
        }
    }

    public void update(KhachRow k) throws SQLException {
        String sql = "UPDATE KHACH_THAM SET so_cmt=?, ho_ten=?, ngay_sinh=? WHERE id_khach=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            setFields(p, k);
            p.setInt(4, k.idKhach());
            if (p.executeUpdate() != 1) {
                throw new SQLException("Khong tim thay khach can sua.");
            }
        }
    }

    public void delete(int id) throws SQLException {
        // FK ON DELETE RESTRICT chan xoa khach da co luot tham.
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "DELETE FROM KHACH_THAM WHERE id_khach=?")) {
            p.setInt(1, id);
            if (p.executeUpdate() != 1) {
                throw new SQLException("Khong tim thay khach can xoa.");
            }
        }
    }

    private static void setFields(PreparedStatement p, KhachRow k) throws SQLException {
        p.setString(1, k.soCmt());
        p.setString(2, k.hoTen());
        if (k.ngaySinh() == null) {
            p.setNull(3, Types.VARCHAR);
        } else {
            p.setString(3, k.ngaySinh().toString());
        }
    }
}
