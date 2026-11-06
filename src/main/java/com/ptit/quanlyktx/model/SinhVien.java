package com.ptit.quanlyktx.model;

import java.time.LocalDate;

public class SinhVien {

    private String maSV;
    private String soCmt;
    private String hoTen;
    private LocalDate ngaySinh;
    private String lop;
    private String queQuan;
    private String soPhong;
    private LocalDate ngayVaoO;

    public SinhVien() {
    }

    public SinhVien(String maSV, String soCmt, String hoTen, LocalDate ngaySinh, String lop, String queQuan, String soPhong, LocalDate ngayVaoO) {
        this.maSV = maSV;
        this.soCmt = soCmt;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.lop = lop;
        this.queQuan = queQuan;
        this.soPhong = soPhong;
        this.ngayVaoO = ngayVaoO;
    }

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
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

    public String getLop() {
        return lop;
    }

    public void setLop(String lop) {
        this.lop = lop;
    }

    public String getQueQuan() {
        return queQuan;
    }

    public void setQueQuan(String queQuan) {
        this.queQuan = queQuan;
    }

    public String getSoPhong() {
        return soPhong;
    }

    public void setSoPhong(String soPhong) {
        this.soPhong = soPhong;
    }

    public LocalDate getNgayVaoO() {
        return ngayVaoO;
    }

    public void setNgayVaoO(LocalDate ngayVaoO) {
        this.ngayVaoO = ngayVaoO;
    }

}
