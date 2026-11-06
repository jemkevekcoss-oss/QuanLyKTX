package com.ptit.quanlyktx.model;

import java.math.BigDecimal;

public class Phong {

    private String soPhong;
    private String loaiPhong;
    private BigDecimal donGia;
    private int soNguoiToiDa;

    public Phong() {
    }

    public Phong(String soPhong, String loaiPhong, BigDecimal donGia, int soNguoiToiDa) {
        this.soPhong = soPhong;
        this.loaiPhong = loaiPhong;
        this.donGia = donGia;
        this.soNguoiToiDa = soNguoiToiDa;
    }

    public String getSoPhong() {
        return soPhong;
    }

    public void setSoPhong(String soPhong) {
        this.soPhong = soPhong;
    }

    public String getLoaiPhong() {
        return loaiPhong;
    }

    public void setLoaiPhong(String loaiPhong) {
        this.loaiPhong = loaiPhong;
    }

    public BigDecimal getDonGia() {
        return donGia;
    }

    public void setDonGia(BigDecimal donGia) {
        this.donGia = donGia;
    }

    public int getSoNguoiToiDa() {
        return soNguoiToiDa;
    }

    public void setSoNguoiToiDa(int soNguoiToiDa) {
        this.soNguoiToiDa = soNguoiToiDa;
    }

}
