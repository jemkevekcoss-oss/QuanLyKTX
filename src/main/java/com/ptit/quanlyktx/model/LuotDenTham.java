package com.ptit.quanlyktx.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

//-------------------- NOT USED --------------------
public class LuotDenTham {

    private int idLuot;
    private int idKhach;
    private String maSV;
    private LocalDate ngayDenChoi;
    private LocalDateTime thoiGianDen;
    private LocalDateTime thoiGianDi;

    public LuotDenTham() {
    }

    public LuotDenTham(int idLuot, int idKhach, String maSV,
                       LocalDate ngayDenChoi, LocalDateTime thoiGianDen,
                       LocalDateTime thoiGianDi) {
        this.idLuot = idLuot;
        this.idKhach = idKhach;
        this.maSV = maSV;
        this.ngayDenChoi = ngayDenChoi;
        this.thoiGianDen = thoiGianDen;
        this.thoiGianDi = thoiGianDi;
    }

    public int getIdLuot() {
        return idLuot;
    }

    public void setIdLuot(int idLuot) {
        this.idLuot = idLuot;
    }

    public int getIdKhach() {
        return idKhach;
    }

    public void setIdKhach(int idKhach) {
        this.idKhach = idKhach;
    }

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
    }

    public LocalDate getNgayDenChoi() {
        return ngayDenChoi;
    }

    public void setNgayDenChoi(LocalDate ngayDenChoi) {
        this.ngayDenChoi = ngayDenChoi;
    }

    public LocalDateTime getThoiGianDen() {
        return thoiGianDen;
    }

    public void setThoiGianDen(LocalDateTime thoiGianDen) {
        this.thoiGianDen = thoiGianDen;
    }

    public LocalDateTime getThoiGianDi() {
        return thoiGianDi;
    }

    public void setThoiGianDi(LocalDateTime thoiGianDi) {
        this.thoiGianDi = thoiGianDi;
    }
}
