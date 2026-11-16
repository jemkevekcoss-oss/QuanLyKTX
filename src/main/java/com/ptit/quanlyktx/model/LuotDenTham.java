package com.ptit.quanlyktx.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

//-------------------- NOT USED --------------------
public class LuotDenTham {

    private int idLuot;
    private int idKhach;
    private String tenKhach;
    private String soCmt;
    private String maSv;
    private String tenSv;
    private LocalDate ngayDenChoi;
    private LocalDateTime thoiGianDen;
    private LocalDateTime thoiGianDi;
    
    public LuotDenTham() {
    }

    public LuotDenTham(int idLuot, int idKhach, String tenKhach, String soCmt, String maSv, String tenSv, LocalDate ngayDenChoi, LocalDateTime thoiGianDen, LocalDateTime thoiGianDi) {
        this.idLuot = idLuot;
        this.idKhach = idKhach;
        this.tenKhach = tenKhach;
        this.soCmt = soCmt;
        this.maSv = maSv;
        this.tenSv = tenSv;
        this.ngayDenChoi = ngayDenChoi;
        this.thoiGianDen = thoiGianDen;
        this.thoiGianDi = thoiGianDi;
    }

    
    

    public int getIdLuot() {
        return idLuot;
    }
    public int getIdKhach() {
        return idKhach;
    }
    public String getTenKhach() {
        return tenKhach;
    }
    public String getSoCmt() {
        return soCmt;
    }
    public String getMaSv() {
        return maSv;
    }
    public String getTenSv() {
        return tenSv;
    }
    public LocalDate getNgayDenChoi() {
        return ngayDenChoi;
    }
    public LocalDateTime getThoiGianDen() {
        return thoiGianDen;
    }
    public LocalDateTime getThoiGianDi() {
        return thoiGianDi;
    }

    public void setIdLuot(int idLuot) {
        this.idLuot = idLuot;
    }

    public void setIdKhach(int idKhach) {
        this.idKhach = idKhach;
    }
    public void setTenKhach(String tenKhach) {
        this.tenKhach = tenKhach;
    }
    public void setSoCmt(String soCmt) {
        this.soCmt = soCmt;
    }
    public void setMaSV(String maSv) {
        this.maSv = maSv;
    }
    public void setTenSV(String tenSv) {
        this.tenSv = tenSv;
    }
    public void setNgayDenChoi(LocalDate ngayDenChoi) {
        this.ngayDenChoi = ngayDenChoi;
    }
    public void setThoiGianDen(LocalDateTime thoiGianDen) {
        this.thoiGianDen = thoiGianDen;
    }
    public void setThoiGianDi(LocalDateTime thoiGianDi) {
        this.thoiGianDi = thoiGianDi;
    }
    
    
    
}
