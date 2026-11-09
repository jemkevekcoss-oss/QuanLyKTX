package com.ptit.quanlyktx.dto;

import java.math.BigDecimal;

/** DTO (không phải bảng MySQL): một dòng tổng hợp phải trả của một sinh viên. */
public class HoaDonRow {
    private final String maSV;
    private final String hoTen;
    private final String lop;
    private final String soPhong;
    private final BigDecimal tienPhong;
    private final BigDecimal tienDichVu;
    private final BigDecimal tienVeXeThang;
    private final BigDecimal tienGuiXePhatSinh;
    private final BigDecimal tongPhaiTra;

    public HoaDonRow(String maSV, String hoTen, String lop, String soPhong,
                     BigDecimal tienPhong, BigDecimal tienDichVu,
                     BigDecimal tienVeXeThang, BigDecimal tienGuiXePhatSinh,
                     BigDecimal tongPhaiTra) {
        this.maSV = maSV;
        this.hoTen = hoTen;
        this.lop = lop;
        this.soPhong = soPhong;
        this.tienPhong = tienPhong;
        this.tienDichVu = tienDichVu;
        this.tienVeXeThang = tienVeXeThang;
        this.tienGuiXePhatSinh = tienGuiXePhatSinh;
        this.tongPhaiTra = tongPhaiTra;
    }

    public String getMaSV() { return maSV; }
    public String getHoTen() { return hoTen; }
    public String getLop() { return lop; }
    public String getSoPhong() { return soPhong; }
    public BigDecimal getTienPhong() { return tienPhong; }
    public BigDecimal getTienDichVu() { return tienDichVu; }
    public BigDecimal getTienVeXeThang() { return tienVeXeThang; }
    public BigDecimal getTienGuiXePhatSinh() { return tienGuiXePhatSinh; }
    public BigDecimal getTongPhaiTra() { return tongPhaiTra; }
}
