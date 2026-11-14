package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.ThongKeKhachRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Thong ke so luot khach tham theo thang (moi dong: mot cap SV - khach). */
public class ThongKeKhachDAO {

    public List<ThongKeKhachRow> thongKeTheoThang(YearMonth thang) throws SQLException {
        String sql = """
            SELECT sv.ma_sv,
                   sv.ho_ten AS ten_sv,
                   sv.lop,
                   sv.so_phong,
                   k.id_khach,
                   k.so_cmt,
                   k.ho_ten AS ten_khach,
                   k.ngay_sinh,
                   COUNT(l.id_luot) AS so_lan_den,
                   MAX(l.ngay_den_choi) AS ngay_gan_nhat
            FROM LUOT_DEN_THAM l
            JOIN KHACH_THAM k ON k.id_khach = l.id_khach
            JOIN SINH_VIEN sv ON sv.ma_sv = l.ma_sv
            WHERE l.ngay_den_choi >= ? AND l.ngay_den_choi < ?
            GROUP BY sv.ma_sv, sv.ho_ten, sv.lop, sv.so_phong,
                     k.id_khach, k.so_cmt, k.ho_ten, k.ngay_sinh
            ORDER BY sv.ma_sv, so_lan_den DESC, k.ho_ten
            """;
        List<ThongKeKhachRow> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            // SQLite TEXT DATE: so sanh ISO YYYY-MM-DD duoc chinh xac.
            p.setString(1, thang.atDay(1).toString());
            p.setString(2, thang.plusMonths(1).atDay(1).toString());
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    String ns = r.getString("ngay_sinh");
                    String gan = r.getString("ngay_gan_nhat");
                    result.add(new ThongKeKhachRow(
                            r.getString("ma_sv"),
                            r.getString("ten_sv"),
                            r.getString("lop"),
                            r.getString("so_phong"),
                            r.getInt("id_khach"),
                            r.getString("so_cmt"),
                            r.getString("ten_khach"),
                            ns == null ? null : LocalDate.parse(ns),
                            r.getLong("so_lan_den"),
                            gan == null ? null : LocalDate.parse(gan)));
                }
            }
        }
        return result;
    }
}
