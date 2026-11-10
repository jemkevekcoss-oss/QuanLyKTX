package com.ptit.quanlyktx.dao;

import com.ptit.quanlyktx.database.DatabaseConnection;
import com.ptit.quanlyktx.dto.TinhTrangPhongRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Chỉ đọc MySQL; không phụ thuộc các DAO nghiệp vụ khác. */
public class TongQuanDAO {
    public record SoLieu(int tongSinhVien, int tongPhong, int tongSucChua,
                         int tongChoTrong, int phongDay, int phongConCho,
                         int phongTrong, int tongDichVu,
                         List<TinhTrangPhongRow> topPhong) {}

    private static final String SQL_PHONG = """
            SELECT p.so_phong, p.so_nguoi_toi_da AS suc_chua,
                   COUNT(sv.ma_sv) AS dang_o
            FROM PHONG p
            LEFT JOIN SINH_VIEN sv ON sv.so_phong = p.so_phong
            GROUP BY p.so_phong, p.so_nguoi_toi_da
            ORDER BY (COUNT(sv.ma_sv) / NULLIF(p.so_nguoi_toi_da, 0)) ASC,
                     (p.so_nguoi_toi_da - COUNT(sv.ma_sv)) DESC,
                     p.so_phong ASC
            """;

    public SoLieu taiSoLieu() throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            // Đọc chung trong một transaction để hạn chế KPI và phòng bị lệch do dữ liệu thay đổi giữa chừng.
            boolean oldAutoCommit = c.getAutoCommit();
            boolean oldReadOnly = c.isReadOnly();
            try {
                c.setReadOnly(true);
                c.setAutoCommit(false);
                List<TinhTrangPhongRow> phong = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(SQL_PHONG);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        phong.add(new TinhTrangPhongRow(
                                rs.getString("so_phong"),
                                rs.getInt("suc_chua"),
                                rs.getInt("dang_o")));
                    }
                }
                int students = dem(c, "SELECT COUNT(*) FROM SINH_VIEN");
                int services = dem(c, "SELECT COUNT(*) FROM DICH_VU");
                int capacity = phong.stream().mapToInt(TinhTrangPhongRow::sucChua).sum();
                int full = 0, partial = 0, empty = 0, vacancies = 0;
                for (TinhTrangPhongRow p : phong) {
                    vacancies += p.conTrong();
                    if (p.dangO() == 0) empty++;
                    else if (p.sucChua() > 0 && p.dangO() >= p.sucChua()) full++;
                    else partial++;
                }
                c.commit();
                return new SoLieu(students, phong.size(), capacity, vacancies,
                        full, partial, empty, services,
                        List.copyOf(phong.subList(0, Math.min(4, phong.size()))));
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(oldAutoCommit);
                c.setReadOnly(oldReadOnly);
            }
        }
    }

    private int dem(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
