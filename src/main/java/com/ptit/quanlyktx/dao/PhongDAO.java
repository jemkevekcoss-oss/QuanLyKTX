package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.PhongThongKe;
import com.ptit.quanlyktx.model.Phong;
import com.ptit.quanlyktx.model.SinhVien;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Truy vấn và CRUD bảng PHONG bằng SQLite JDBC. */
public class PhongDAO {

    /** Danh sách tất cả phòng, kể cả phòng chưa có sinh viên. */
    public List<PhongThongKe> findAll() throws SQLException {
        final String sql = """
            SELECT p.so_phong, p.loai_phong, p.don_gia, p.so_nguoi_toi_da,
                   COUNT(sv.ma_sv) AS so_nguoi_dang_o
            FROM PHONG p
            LEFT JOIN SINH_VIEN sv ON sv.so_phong = p.so_phong
            GROUP BY p.so_phong, p.loai_phong, p.don_gia, p.so_nguoi_toi_da
            ORDER BY p.so_phong
            """;

        List<PhongThongKe> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Phong phong = new Phong(
                    rs.getString("so_phong"),
                    rs.getString("loai_phong"),
                    rs.getBigDecimal("don_gia"),
                    rs.getInt("so_nguoi_toi_da")
                );
                result.add(new PhongThongKe(
                    phong, rs.getInt("so_nguoi_dang_o")
                ));
            }
        }
        return result;
    }

    /** Lấy danh sách sinh viên thuộc phòng đang chọn. */
    public List<SinhVien> findStudentsByRoom(String soPhong) throws SQLException {
        final String sql = """
            SELECT ma_sv, so_cmt, ho_ten, ngay_sinh,
                   lop, que_quan, so_phong, ngay_vao_o
            FROM SINH_VIEN
            WHERE so_phong = ?
            ORDER BY ma_sv
            """;

        List<SinhVien> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, soPhong);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new SinhVien(
                        rs.getString("ma_sv"),
                        rs.getString("so_cmt"),
                        rs.getString("ho_ten"),
                        asLocalDate(rs.getString("ngay_sinh")),
                        rs.getString("lop"),
                        rs.getString("que_quan"),
                        rs.getString("so_phong"),
                        asLocalDate(rs.getString("ngay_vao_o"))
                    ));
                }
            }
        }
        return result;
    }

    /** Thêm phòng mới. */
    public boolean insert(Phong phong) throws SQLException {
        final String sql = """
            INSERT INTO PHONG (so_phong, loai_phong, don_gia, so_nguoi_toi_da)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phong.getSoPhong());
            ps.setString(2, phong.getLoaiPhong());
            ps.setBigDecimal(3, phong.getDonGia());
            ps.setInt(4, phong.getSoNguoiToiDa());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Sửa phòng và ngăn giảm sức chứa thấp hơn số sinh viên đang ở.
     * SQLite không hỗ trợ SELECT ... FOR UPDATE như MySQL.
     * Điều kiện kiểm tra trong chính câu UPDATE giúp thao tác kiểm tra và ghi
     * được thực hiện nguyên tử, không phải dùng commit/rollback riêng.
     */
    public boolean update(Phong phong) throws SQLException {
        final String updateSql = """
            UPDATE PHONG
            SET loai_phong = ?, don_gia = ?, so_nguoi_toi_da = ?
            WHERE so_phong = ?
              AND (
                  SELECT COUNT(*)
                  FROM SINH_VIEN
                  WHERE SINH_VIEN.so_phong = PHONG.so_phong
              ) <= ?
            """;
        final String checkSql = """
            SELECT COUNT(*) AS dang_o
            FROM SINH_VIEN
            WHERE so_phong = ?
            """;
        final String existsSql = "SELECT 1 FROM PHONG WHERE so_phong = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, phong.getLoaiPhong());
                ps.setBigDecimal(2, phong.getDonGia());
                ps.setInt(3, phong.getSoNguoiToiDa());
                ps.setString(4, phong.getSoPhong());
                ps.setInt(5, phong.getSoNguoiToiDa());

                if (ps.executeUpdate() == 1) {
                    return true;
                }
            }

            // UPDATE không tác động dòng nào: phòng không tồn tại hoặc sức chứa quá nhỏ.
            try (PreparedStatement ps = conn.prepareStatement(existsSql)) {
                ps.setString(1, phong.getSoPhong());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, phong.getSoPhong());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int dangO = rs.getInt("dang_o");
                        if (phong.getSoNguoiToiDa() < dangO) {
                            throw new SQLException(
                                "Không thể giảm sức chứa xuống " + phong.getSoNguoiToiDa()
                                + ": phòng đang có " + dangO + " sinh viên.",
                                "45000"
                            );
                        }
                    }
                }
            }
            return false;
        }
    }

    /** FK từ SINH_VIEN sẽ chặn xóa phòng đang có người (foreign_keys = ON). */
    public boolean delete(String soPhong) throws SQLException {
        final String sql = "DELETE FROM PHONG WHERE so_phong = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, soPhong);
            return ps.executeUpdate() == 1;
        }
    }

    /** Ngày trong SQLite được lưu dưới dạng chuỗi YYYY-MM-DD. */
    private static LocalDate asLocalDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }
}
