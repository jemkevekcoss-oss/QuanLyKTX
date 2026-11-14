package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.LuotDenThamRow;
import com.ptit.quanlyktx.dto.SinhVienOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Moi dong LUOT_DEN_THAM la mot luot tham, ke ca nhieu luot trong mot ngay. */
public class LuotDenThamDAO {

    private static final DateTimeFormatter SQL_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<LuotDenThamRow> findAll() throws SQLException {
        String sql = """
            SELECT l.id_luot, l.id_khach, k.ho_ten AS ten_khach, k.so_cmt,
                   l.ma_sv, s.ho_ten AS ten_sv, l.ngay_den_choi,
                   l.thoi_gian_den, l.thoi_gian_di
            FROM LUOT_DEN_THAM l
            JOIN KHACH_THAM k ON k.id_khach = l.id_khach
            JOIN SINH_VIEN s ON s.ma_sv = l.ma_sv
            ORDER BY l.ngay_den_choi DESC, l.thoi_gian_den DESC, l.id_luot DESC
            """;
        List<LuotDenThamRow> items = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            while (r.next()) {
                items.add(new LuotDenThamRow(
                        r.getInt("id_luot"),
                        r.getInt("id_khach"),
                        r.getString("ten_khach"),
                        r.getString("so_cmt"),
                        r.getString("ma_sv"),
                        r.getString("ten_sv"),
                        LocalDate.parse(r.getString("ngay_den_choi")),
                        readDateTime(r.getString("thoi_gian_den")),
                        readDateTime(r.getString("thoi_gian_di"))));
            }
        }
        return items;
    }

    public List<SinhVienOption> findSinhVien() throws SQLException {
        List<SinhVienOption> items = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT ma_sv, ho_ten FROM SINH_VIEN ORDER BY ma_sv");
             ResultSet r = p.executeQuery()) {
            while (r.next()) {
                items.add(new SinhVienOption(r.getString(1), r.getString(2)));
            }
        }
        return items;
    }

    public void checkIn(int idKhach, String maSv) throws SQLException {
        // Lay ngay/gio dia phuong tu SQLite, khong dung UTC mac dinh.
        // INSERT ... WHERE NOT EXISTS gom kiem tra va ghi thanh mot lenh.
        String sql = """
            INSERT INTO LUOT_DEN_THAM
                (id_khach, ma_sv, ngay_den_choi, thoi_gian_den, thoi_gian_di)
            SELECT ?, ?, date('now','localtime'), datetime('now','localtime'), NULL
            WHERE NOT EXISTS (
                SELECT 1 FROM LUOT_DEN_THAM
                WHERE id_khach = ?
                  AND thoi_gian_den IS NOT NULL
                  AND thoi_gian_di IS NULL
            )
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, idKhach);
            p.setString(2, maSv);
            p.setInt(3, idKhach);
            if (p.executeUpdate() != 1) {
                throw new SQLException("Khach dang co luot tham chua check-out.");
            }
        }
    }

    public void checkOut(int idLuot) throws SQLException {
        String sql = """
            UPDATE LUOT_DEN_THAM
            SET thoi_gian_di = datetime('now','localtime')
            WHERE id_luot = ?
              AND thoi_gian_den IS NOT NULL
              AND thoi_gian_di IS NULL
              AND thoi_gian_den <= datetime('now','localtime')
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, idLuot);
            if (p.executeUpdate() != 1) {
                throw new SQLException("Luot nay da ket thuc hoac thieu gio check-in.");
            }
        }
    }

    public void updateVisit(int idLuot, int idKhach, String maSv,
                            LocalDate ngay, LocalDateTime den,
                            LocalDateTime di) throws SQLException {
        String sql = """
            UPDATE LUOT_DEN_THAM
            SET id_khach=?, ma_sv=?, ngay_den_choi=?, thoi_gian_den=?, thoi_gian_di=?
            WHERE id_luot=?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, idKhach);
            p.setString(2, maSv);
            p.setString(3, ngay.toString());
            setDateTime(p, 4, den);
            setDateTime(p, 5, di);
            p.setInt(6, idLuot);
            if (p.executeUpdate() != 1) {
                throw new SQLException("Khong tim thay luot can sua.");
            }
        }
    }

    public void deleteVisit(int idLuot) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "DELETE FROM LUOT_DEN_THAM WHERE id_luot=?")) {
            p.setInt(1, idLuot);
            if (p.executeUpdate() != 1) {
                throw new SQLException("Khong tim thay luot can xoa.");
            }
        }
    }

    private static LocalDateTime readDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        // SQLite TEXT: 'yyyy-MM-dd HH:mm:ss' (hoac 'yyyy-MM-ddTHH:mm:ss').
        return LocalDateTime.parse(value.trim().replace(' ', 'T'));
    }

    private static void setDateTime(PreparedStatement p, int index,
                                    LocalDateTime value) throws SQLException {
        if (value == null) {
            p.setNull(index, Types.VARCHAR);
        } else {
            p.setString(index, value.format(SQL_DATETIME));
        }
    }
}
