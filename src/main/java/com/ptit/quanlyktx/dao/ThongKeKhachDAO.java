package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.ThongKeKhachRow;
import java.sql.*;
import java.time.YearMonth;
import java.util.*;

public class ThongKeKhachDAO {
    public List<ThongKeKhachRow> thongKeTheoThang(YearMonth thang) throws SQLException {
        String sql = """
            SELECT sv.ma_sv, sv.ho_ten AS ten_sv, sv.lop, sv.so_phong,
                   k.id_khach, k.so_cmt, k.ho_ten AS ten_khach, k.ngay_sinh,
                   COUNT(l.id_luot) AS so_lan_den,
                   MAX(l.ngay_den_choi) AS ngay_gan_nhat
            FROM LUOT_DEN_THAM l
            JOIN KHACH_THAM k ON k.id_khach=l.id_khach
            JOIN SINH_VIEN sv ON sv.ma_sv=l.ma_sv
            WHERE l.ngay_den_choi >= ? AND l.ngay_den_choi < ?
            GROUP BY sv.ma_sv,sv.ho_ten,sv.lop,sv.so_phong,
                     k.id_khach,k.so_cmt,k.ho_ten,k.ngay_sinh
            ORDER BY sv.ma_sv,so_lan_den DESC,k.ho_ten
            """;
        List<ThongKeKhachRow> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setDate(1,java.sql.Date.valueOf(thang.atDay(1)));
            p.setDate(2,java.sql.Date.valueOf(thang.plusMonths(1).atDay(1)));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    java.sql.Date ns = r.getDate("ngay_sinh"), gan = r.getDate("ngay_gan_nhat");
                    result.add(new ThongKeKhachRow(r.getString("ma_sv"),r.getString("ten_sv"),
                        r.getString("lop"),r.getString("so_phong"),r.getInt("id_khach"),
                        r.getString("so_cmt"),r.getString("ten_khach"),
                        ns==null?null:ns.toLocalDate(),r.getLong("so_lan_den"),
                        gan==null?null:gan.toLocalDate()));
                }
            }
        }
        return result;
    }
}
