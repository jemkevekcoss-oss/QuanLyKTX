package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.model.SinhVien;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Đọc bảng SINH_VIEN và ánh xạ mỗi hàng thành một đối tượng SinhVien. */
public class SinhVienDAO {

    public List<SinhVien> findAll() throws SQLException {
        final String sql = """
                SELECT ma_sv, so_cmt, ho_ten, ngay_sinh,
                       lop, que_quan, so_phong, ngay_vao_o
                FROM SINH_VIEN
                ORDER BY ma_sv
                """;

        List<SinhVien> result = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                result.add(new SinhVien(
                    rs.getString("ma_sv"),
                    rs.getString("so_cmt"),
                    rs.getString("ho_ten"),
                    asLocalDate(rs.getDate("ngay_sinh")),
                    rs.getString("lop"),
                    rs.getString("que_quan"),
                    rs.getString("so_phong"),
                    asLocalDate(rs.getDate("ngay_vao_o"))
                ));
            }
        }
        return result;
    }

    private static LocalDate asLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }
}
