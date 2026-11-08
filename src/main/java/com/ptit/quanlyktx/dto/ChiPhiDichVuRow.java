package com.ptit.quanlyktx.dto;

import java.math.BigDecimal;

/** Kết quả truy vấn 2 - gộp theo sinh viên và dịch vụ trong khoảng ngày. */
public class ChiPhiDichVuRow {
    private final String maSV, hoTen, tenDV, donViTinh;
    private final long tongSoLuong;
    private final BigDecimal tongTien;

    public ChiPhiDichVuRow(String maSV, String hoTen, String tenDV, String donViTinh,
                           long tongSoLuong, BigDecimal tongTien) {
        this.maSV = maSV;
        this.hoTen = hoTen;
        this.tenDV = tenDV;
        this.donViTinh = donViTinh;
        this.tongSoLuong = tongSoLuong;
        this.tongTien = tongTien;
    }
    public String getMaSV() { return maSV; }
    public String getHoTen() { return hoTen; }
    public String getTenDV() { return tenDV; }
    public String getDonViTinh() { return donViTinh; }
    public long getTongSoLuong() { return tongSoLuong; }
    public BigDecimal getTongTien() { return tongTien; }
}
