package com.ptit.quanlyktx.model;

import java.time.LocalDate;

public class KhachTham {

    private int idKhach;
    private String soCmt;
    private String hoTen;
    private LocalDate ngaySinh;
    private LocalDate ngayDenChoi;
    private String maSV;

    public KhachTham() {
    }

    public KhachTham(int idKhach, String soCmt, String hoTen, LocalDate ngaySinh, LocalDate ngayDenChoi, String maSV) {
        this.idKhach = idKhach;
        this.soCmt = soCmt;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.ngayDenChoi = ngayDenChoi;
        this.maSV = maSV;
    }

    public int getIdKhach() {
        return idKhach;
    }

    public void setIdKhach(int idKhach) {
        this.idKhach = idKhach;
    }

    public String getSoCmt() {
        return soCmt;
    }

    public void setSoCmt(String soCmt) {
        this.soCmt = soCmt;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public LocalDate getNgayDenChoi() {
        return ngayDenChoi;
    }

    public void setNgayDenChoi(LocalDate ngayDenChoi) {
        this.ngayDenChoi = ngayDenChoi;
    }

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
    }

}
