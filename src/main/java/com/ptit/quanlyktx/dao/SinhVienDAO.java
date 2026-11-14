package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.model.SinhVien;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
                    docNgay(rs, "ngay_sinh"),
                    rs.getString("lop"),
                    rs.getString("que_quan"),
                    rs.getString("so_phong"),
                    docNgay(rs, "ngay_vao_o")
                ));
            }
        }
        return result;
    }
    
    //INSERT
    public boolean insert(SinhVien sv) throws SQLException {

        String sql = """
            INSERT INTO SINH_VIEN
            (ma_sv, so_cmt, ho_ten, ngay_sinh,
             lop, que_quan, so_phong, ngay_vao_o)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sv.getMaSV());
            ps.setString(2, sv.getSoCmt());
            ps.setString(3, sv.getHoTen());
            ps.setString(4, sv.getNgaySinh().toString());
            ps.setString(5, sv.getLop());
            ps.setString(6, sv.getQueQuan());
            ps.setString(7, sv.getSoPhong());
            ps.setString(8, sv.getNgayVaoO().toString());

            return ps.executeUpdate() == 1;
        }
        
    } 
    //UPDATE
    public boolean update(SinhVien sv) throws SQLException {
        String sql = """
            UPDATE SINH_VIEN
            SET so_cmt = ?,
                ho_ten = ?,
                ngay_sinh = ?,
                lop = ?,
                que_quan = ?,
                so_phong = ?,
                ngay_vao_o = ?
            WHERE ma_sv = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sv.getSoCmt());
            ps.setString(2, sv.getHoTen());
            ps.setString(3, sv.getNgaySinh().toString());
            ps.setString(4, sv.getLop());
            ps.setString(5, sv.getQueQuan());
            ps.setString(6, sv.getSoPhong());
            ps.setString(7, sv.getNgayVaoO().toString());
            ps.setString(8, sv.getMaSV());

            return ps.executeUpdate() == 1;
        }
    }   
    //DELETE
    public boolean delete(String maSV) throws SQLException {
        String sql = """
            DELETE FROM SINH_VIEN
            WHERE ma_sv = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, maSV);

            return ps.executeUpdate() == 1;
        }
    } 
    
    /** SQLite luu DATE theo chuoi ISO yyyy-MM-dd. */
    private static LocalDate docNgay(ResultSet rs, String cot) throws SQLException {
        String ngay = rs.getString(cot);
        if (ngay == null) {
            throw new SQLException("Thieu ngay tai cot " + cot + " trong SINH_VIEN");
        }
        try {
            return LocalDate.parse(ngay);
        } catch (DateTimeParseException e) {
            throw new SQLException("Ngay khong hop le tai cot " + cot + ": " + ngay, e);
        }
    }
    
}
