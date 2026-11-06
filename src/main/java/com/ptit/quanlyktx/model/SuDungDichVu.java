package com.ptit.quanlyktx.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SuDungDichVu {

    private int idSuDung;
    private String maSV;
    private String maDV;
    private LocalDateTime thoiGianSuDung;
    private int soLuong;
    private BigDecimal thanhTien;

    public SuDungDichVu() {
    }

    public SuDungDichVu(int idSuDung, String maSV, String maDV, LocalDateTime thoiGianSuDung, int soLuong, BigDecimal thanhTien) {
        this.idSuDung = idSuDung;
        this.maSV = maSV;
        this.maDV = maDV;
        this.thoiGianSuDung = thoiGianSuDung;
        this.soLuong = soLuong;
        this.thanhTien = thanhTien;
    }

    public int getIdSuDung() {
        return idSuDung;
    }

    public void setIdSuDung(int idSuDung) {
        this.idSuDung = idSuDung;
    }

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
    }

    public String getMaDV() {
        return maDV;
    }

    public void setMaDV(String maDV) {
        this.maDV = maDV;
    }

    public LocalDateTime getThoiGianSuDung() {
        return thoiGianSuDung;
    }

    public void setThoiGianSuDung(LocalDateTime thoiGianSuDung) {
        this.thoiGianSuDung = thoiGianSuDung;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    public BigDecimal getThanhTien() {
        return thanhTien;
    }

    public void setThanhTien(BigDecimal thanhTien) {
        this.thanhTien = thanhTien;
    }

}
