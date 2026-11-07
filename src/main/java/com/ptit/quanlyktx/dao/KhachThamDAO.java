package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.KhachThamRow;
import com.ptit.quanlyktx.dto.ThongKeLuotTham;
import com.ptit.quanlyktx.model.KhachTham;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** CRUD KHACH_THAM và thống kê số lượt thăm từ MySQL. */
public class KhachThamDAO {

    public List<KhachThamRow> findAll() throws SQLException {
        final String sql = """
            SELECT k.id_khach, k.so_cmt, k.ho_ten, k.ngay_sinh,
                   k.ngay_den_choi, k.ma_sv,
                   s.ho_ten AS ten_sinh_vien, s.so_phong
            FROM KHACH_THAM k
            LEFT JOIN SINH_VIEN s ON s.ma_sv = k.ma_sv
            ORDER BY k.ngay_den_choi DESC, k.id_khach DESC
            """;
        List<KhachThamRow> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                KhachTham guest = new KhachTham(
                    rs.getInt("id_khach"),
                    rs.getString("so_cmt"),
                    rs.getString("ho_ten"),
                    toLocalDate(rs.getDate("ngay_sinh")),
                    toLocalDate(rs.getDate("ngay_den_choi")),
                    rs.getString("ma_sv")
                );
                result.add(new KhachThamRow(
                    guest, rs.getString("ten_sinh_vien"), rs.getString("so_phong")));
            }
        }
        return result;
    }

    public boolean insert(KhachTham guest) throws SQLException {
        final String sql = """
            INSERT INTO KHACH_THAM
                (so_cmt, ho_ten, ngay_sinh, ngay_den_choi, ma_sv)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            fillValues(ps, guest);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean update(KhachTham guest) throws SQLException {
        final String sql = """
            UPDATE KHACH_THAM
            SET so_cmt = ?, ho_ten = ?, ngay_sinh = ?,
                ngay_den_choi = ?, ma_sv = ?
            WHERE id_khach = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            fillValues(ps, guest);
            ps.setInt(6, guest.getIdKhach());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(int idKhach) throws SQLException {
        final String sql = "DELETE FROM KHACH_THAM WHERE id_khach = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idKhach);
            return ps.executeUpdate() == 1;
        }
    }

    private static void fillValues(PreparedStatement ps, KhachTham guest)
            throws SQLException {
        ps.setString(1, guest.getSoCmt());
        ps.setString(2, guest.getHoTen());
        if (guest.getNgaySinh() == null) {
            ps.setNull(3, Types.DATE);
        } else {
            ps.setDate(3, Date.valueOf(guest.getNgaySinh()));
        }
        ps.setDate(4, Date.valueOf(guest.getNgayDenChoi()));
        ps.setString(5, guest.getMaSV());
    }

    /** Nguồn dữ liệu cho ComboBox sinh viên trong form thêm/sửa. */
    public List<SinhVienOption> findStudents() throws SQLException {
        final String sql = """
            SELECT ma_sv, ho_ten, so_phong FROM SINH_VIEN ORDER BY ma_sv
            """;
        List<SinhVienOption> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new SinhVienOption(rs.getString("ma_sv"),
                        rs.getString("ho_ten"), rs.getString("so_phong")));
            }
        }
        return result;
    }

    /** YC truy vấn 3: số lần mỗi khách (theo CCCD) đến thăm từng SV. */
    public List<ThongKeLuotTham> thongKeSoLanDen(
            LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        if (tuNgay == null || denNgay == null || tuNgay.isAfter(denNgay)) {
            throw new IllegalArgumentException("Khoảng ngày thống kê không hợp lệ.");
        }
        final String sql = """
            SELECT k.ma_sv, s.ho_ten AS ten_sv, k.so_cmt,
                   MAX(k.ho_ten) AS ten_khach, COUNT(*) AS so_lan
            FROM KHACH_THAM k
            JOIN SINH_VIEN s ON s.ma_sv = k.ma_sv
            WHERE k.ngay_den_choi BETWEEN ? AND ?
            GROUP BY k.ma_sv, s.ho_ten, k.so_cmt
            ORDER BY s.ho_ten, k.so_cmt
            """;
        List<ThongKeLuotTham> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(tuNgay));
            ps.setDate(2, Date.valueOf(denNgay));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new ThongKeLuotTham(
                        rs.getString("ma_sv"), rs.getString("ten_sv"),
                        rs.getString("so_cmt"), rs.getString("ten_khach"),
                        rs.getLong("so_lan")));
                }
            }
        }
        return result;
    }

    private static LocalDate toLocalDate(Date sqlDate) {
        return sqlDate == null ? null : sqlDate.toLocalDate();
    }

    /** Mỗi mục là một SV có thể được chọn trong giao diện, không phải bảng mới. */
    public record SinhVienOption(String maSV, String hoTen, String soPhong) {
        @Override
        public String toString() {
            return maSV + " - " + hoTen + " (" + soPhong + ")";
        }
    }
}
