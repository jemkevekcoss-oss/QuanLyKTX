package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Mỗi record LUOT_DEN_THAM là một lần khách đến, có thể nhiều lần trong ngày. */
public class LuotDenThamDAO {
    public List<LuotDenThamRow> findAll() throws SQLException {
        String sql = """
            SELECT l.id_luot,l.id_khach,k.ho_ten AS ten_khach,k.so_cmt,
                   l.ma_sv,s.ho_ten AS ten_sv,l.ngay_den_choi,l.thoi_gian_den,l.thoi_gian_di
            FROM LUOT_DEN_THAM l
            JOIN KHACH_THAM k ON k.id_khach=l.id_khach
            JOIN SINH_VIEN s ON s.ma_sv=l.ma_sv
            ORDER BY l.ngay_den_choi DESC,l.thoi_gian_den DESC,l.id_luot DESC
            """;
        List<LuotDenThamRow> items = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                java.sql.Date d = r.getDate("ngay_den_choi");
                Timestamp den = r.getTimestamp("thoi_gian_den"), di = r.getTimestamp("thoi_gian_di");
                items.add(new LuotDenThamRow(r.getInt("id_luot"),r.getInt("id_khach"),
                    r.getString("ten_khach"),r.getString("so_cmt"),r.getString("ma_sv"),
                    r.getString("ten_sv"),d.toLocalDate(),
                    den == null ? null : den.toLocalDateTime(),di == null ? null : di.toLocalDateTime()));
            }
        }
        return items;
    }
    public List<SinhVienOption> findSinhVien() throws SQLException {
        List<SinhVienOption> items = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT ma_sv,ho_ten FROM SINH_VIEN ORDER BY ma_sv");
             ResultSet r = p.executeQuery()) {
            while (r.next()) items.add(new SinhVienOption(r.getString(1),r.getString(2)));
        }
        return items;
    }
    public void checkIn(int idKhach, String maSv) throws SQLException {
        // Lấy thời gian từ MySQL Server để thống nhất với hệ thống lưu trữ.
        String sql = "INSERT INTO LUOT_DEN_THAM(id_khach,ma_sv,ngay_den_choi,thoi_gian_den,thoi_gian_di) " +
                "VALUES(?,?,CURRENT_DATE(),CURRENT_TIMESTAMP(),NULL)";
        try (Connection c = DatabaseConnection.getConnection()) {
            // Kiểm tra lượt chưa đóng; đây là kiểm tra ứng dụng, nếu có nhiều máy đồng thời
            // cần cơ chế transaction/khóa bổ sung để ngăn race condition hoàn toàn.
            try (PreparedStatement q = c.prepareStatement(
                    "SELECT COUNT(*) FROM LUOT_DEN_THAM WHERE id_khach=? AND thoi_gian_den IS NOT NULL AND thoi_gian_di IS NULL")) {
                q.setInt(1, idKhach);
                try (ResultSet r = q.executeQuery()) {
                    if (r.next() && r.getInt(1) > 0)
                        throw new SQLException("Khách đang có lượt thăm chưa check-out.");
                }
            }
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setInt(1,idKhach); p.setString(2,maSv); p.executeUpdate();
            }
        }
    }
    public void checkOut(int idLuot) throws SQLException {
        String sql = "UPDATE LUOT_DEN_THAM SET thoi_gian_di=CURRENT_TIMESTAMP() " +
                "WHERE id_luot=? AND thoi_gian_den IS NOT NULL AND thoi_gian_di IS NULL " +
                "AND thoi_gian_den<=CURRENT_TIMESTAMP()";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1,idLuot);
            if (p.executeUpdate() != 1) throw new SQLException("Lượt này đã kết thúc hoặc thiếu giờ check-in.");
        }
    }
    public void updateVisit(int idLuot, int idKhach, String maSv,
                            LocalDate ngay, LocalDateTime den, LocalDateTime di) throws SQLException {
        String sql = "UPDATE LUOT_DEN_THAM SET id_khach=?,ma_sv=?,ngay_den_choi=?,thoi_gian_den=?,thoi_gian_di=? WHERE id_luot=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1,idKhach); p.setString(2,maSv); p.setDate(3,java.sql.Date.valueOf(ngay));
            if (den == null) p.setNull(4,Types.TIMESTAMP); else p.setTimestamp(4,Timestamp.valueOf(den));
            if (di == null) p.setNull(5,Types.TIMESTAMP); else p.setTimestamp(5,Timestamp.valueOf(di));
            p.setInt(6,idLuot);
            if (p.executeUpdate() != 1) throw new SQLException("Không tìm thấy lượt cần sửa.");
        }
    }
    public void deleteVisit(int idLuot) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM LUOT_DEN_THAM WHERE id_luot=?")) {
            p.setInt(1,idLuot);
            if (p.executeUpdate() != 1) throw new SQLException("Không tìm thấy lượt cần xóa.");
        }
    }
}
