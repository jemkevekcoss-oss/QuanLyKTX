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
    /**
     * Kiểm tra phòng đích khi chuyển sinh viên. Trả null nếu hợp lệ,
     * ngược lại trả về thông báo để Controller hiển thị ngay trong form.
     */
    public String validateRoomChange(String maSV, String soPhongMoi) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return validateRoomChange(conn, maSV, soPhongMoi);
        }
    }

    private String validateRoomChange(Connection conn, String maSV, String soPhongMoi)
            throws SQLException {
        final String sql = """
            SELECT sv.so_phong AS phong_hien_tai,
                   p.so_nguoi_toi_da AS suc_chua,
                   (SELECT COUNT(*) FROM SINH_VIEN s
                    WHERE s.so_phong = p.so_phong) AS dang_o
            FROM SINH_VIEN sv
            LEFT JOIN PHONG p ON p.so_phong = ?
            WHERE sv.ma_sv = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, soPhongMoi);
            ps.setString(2, maSV);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "Không tìm thấy hồ sơ sinh viên cần cập nhật.";
                }
                if (soPhongMoi.equals(rs.getString("phong_hien_tai"))) {
                    return null; // Sửa thông tin nhưng không chuyển phòng.
                }
                if (rs.getObject("suc_chua") == null) {
                    return "Phòng " + soPhongMoi + " không tồn tại. Vui lòng chọn phòng hợp lệ.";
                }
                int dangO = rs.getInt("dang_o");
                int sucChua = rs.getInt("suc_chua");
                if (dangO >= sucChua) {
                    return "Phòng " + soPhongMoi + " đã đủ sức chứa ("
                            + dangO + "/" + sucChua
                            + " sinh viên). Vui lòng chọn phòng khác.";
                }
            }
        }
        return null;
    }

    // UPDATE: điều kiện sức chứa nằm trong chính câu SQL để không ghi sai
    // ngay cả khi dữ liệu thay đổi sau bước kiểm tra trên giao diện.
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
              AND (
                  so_phong = ?
                  OR EXISTS (
                      SELECT 1 FROM PHONG p
                      WHERE p.so_phong = ?
                        AND (SELECT COUNT(*) FROM SINH_VIEN s
                             WHERE s.so_phong = p.so_phong) < p.so_nguoi_toi_da
                  )
              )
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
            ps.setString(9, sv.getSoPhong());
            ps.setString(10, sv.getSoPhong());

            if (ps.executeUpdate() == 1) {
                return true;
            }
            // Phân biệt phòng đầy với trường hợp hồ sơ không còn tồn tại.
            String reason = validateRoomChange(conn, sv.getMaSV(), sv.getSoPhong());
            if (reason != null) {
                throw new SQLException(reason, "45000");
            }
            return false;
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
