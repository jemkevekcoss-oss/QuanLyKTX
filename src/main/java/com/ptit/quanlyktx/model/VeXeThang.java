package com.ptit.quanlyktx.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VeXeThang {

    private String maVeXe;
    private String bienSoXe;
    private String loaiXe;
    private String maSV;
    private String thangDangKy;
    private LocalDate ngayDangKy;
    private BigDecimal phiHangThang;
    
    public VeXeThang() {
    }
    //constructor01
    public VeXeThang(String maVeXe, String bienSoXe, String loaiXe, String maSV, String thangDangKy, BigDecimal phiHangThang) {
        this.maVeXe = maVeXe;
        this.bienSoXe = bienSoXe;
        this.loaiXe = loaiXe;
        this.maSV = maSV;
        this.thangDangKy = thangDangKy;
        this.phiHangThang = phiHangThang;
    }
    //constructor02
    public VeXeThang(String maVeXe, String bienSoXe,
                     String loaiXe, String maSV,
                     String thangDangKy, LocalDate ngayDangKy,
                     BigDecimal phiHangThang) {
        this(maVeXe, bienSoXe, loaiXe, maSV,
             thangDangKy, phiHangThang);
        this.ngayDangKy = ngayDangKy;
    }
    

    public String getMaVeXe() {
        return maVeXe;
    }

    public void setMaVeXe(String maVeXe) {
        this.maVeXe = maVeXe;
    }

    public String getBienSoXe() {
        return bienSoXe;
    }

    public void setBienSoXe(String bienSoXe) {
        this.bienSoXe = bienSoXe;
    }

    public String getLoaiXe() {
        return loaiXe;
    }

    public void setLoaiXe(String loaiXe) {
        this.loaiXe = loaiXe;
    }

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
    }

    public String getThangDangKy() {
        return thangDangKy;
    }

    public void setThangDangKy(String thangDangKy) {
        this.thangDangKy = thangDangKy;
    }
    
    public LocalDate getNgayDangKy() {
        return ngayDangKy;
    }

    public void setNgayDangKy(LocalDate ngayDangKy) {
        this.ngayDangKy = ngayDangKy;
    }
    
    public BigDecimal getPhiHangThang() {
        return phiHangThang;
    }

    public void setPhiHangThang(BigDecimal phiHangThang) {
        this.phiHangThang = phiHangThang;
    }

}
