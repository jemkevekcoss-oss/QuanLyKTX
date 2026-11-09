package com.ptit.quanlyktx.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Một dòng vé tháng đã JOIN với bảng SINH_VIEN.
 */
public record VeXeThangRow(
        String maVeXe,
        String bienSoXe,
        String loaiXe,
        String maSV,
        String hoTen,
        String lop,
        String soPhong,
        String thangDangKy,
        LocalDate ngayDangKy,
        BigDecimal phiHangThang
) {

    // Constructor cũ: giữ lại để DAO hiện tại chưa bị lỗi
    public VeXeThangRow(
            String maVeXe,
            String bienSoXe,
            String loaiXe,
            String maSV,
            String hoTen,
            String lop,
            String soPhong,
            String thangDangKy,
            BigDecimal phiHangThang
    ) {
        this(maVeXe, bienSoXe, loaiXe, maSV,
             hoTen, lop, soPhong, thangDangKy,
             null, phiHangThang);
    }

    // Kiểm tra vé có hiệu lực ở thời điểm hiện tại
    public boolean conHieuLuc() {
        try {
            YearMonth kyVe = YearMonth.parse(thangDangKy);
            LocalDate homNay = LocalDate.now();

            // Vé phải thuộc tháng hiện tại
            if (!kyVe.equals(YearMonth.from(homNay))) {
                return false;
            }

            // Nếu biết ngày đăng ký, không dùng vé trước ngày đó
            if (ngayDangKy != null
                    && homNay.isBefore(ngayDangKy)) {
                return false;
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
