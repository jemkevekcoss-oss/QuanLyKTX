package com.ptit.quanlyktx.dto;

import java.math.BigDecimal;

/** Dữ liệu chỉ đọc cho bảng xếp hạng SV sử dụng DV trong một tháng. */
public record TopSinhVienDichVuRow(
        int thuHang, String maSv, String hoTen,
        int soLuot, BigDecimal tongTien) {
}
