package com.ptit.quanlyktx.dto;

import java.math.BigDecimal;

/** Một dịch vụ trong báo cáo doanh thu của một tháng (không tạo bảng mới). */
public record DoanhThuDichVuRow(
        String maDv,
        String tenDv,
        BigDecimal donGia,
        String donViTinh,
        int soLuotSuDung,
        BigDecimal tongSoLuong,
        BigDecimal tongDoanhThu
) {}
