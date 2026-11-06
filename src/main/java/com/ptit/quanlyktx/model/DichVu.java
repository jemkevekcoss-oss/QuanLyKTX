package com.ptit.quanlyktx.model;

import java.math.BigDecimal;

public class DichVu {

    private String maDV;
    private String tenDV;
    private BigDecimal donGia;
    private String donViTinh;

    public DichVu() {
    }

    public DichVu(String maDV, String tenDV, BigDecimal donGia, String donViTinh) {
        this.maDV = maDV;
        this.tenDV = tenDV;
        this.donGia = donGia;
        this.donViTinh = donViTinh;
    }

    public String getMaDV() {
        return maDV;
    }

    public void setMaDV(String maDV) {
        this.maDV = maDV;
    }

    public String getTenDV() {
        return tenDV;
    }

    public void setTenDV(String tenDV) {
        this.tenDV = tenDV;
    }

    public BigDecimal getDonGia() {
        return donGia;
    }

    public void setDonGia(BigDecimal donGia) {
        this.donGia = donGia;
    }

    public String getDonViTinh() {
        return donViTinh;
    }

    public void setDonViTinh(String donViTinh) {
        this.donViTinh = donViTinh;
    }

}
