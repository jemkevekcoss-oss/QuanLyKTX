package com.ptit.quanlyktx.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LuotGuiXe {

    private int idLuot;
    private String maVeXe;
    private LocalDateTime thoiGianGui;
    private LocalDateTime thoiGianLay;
    private BigDecimal tienPhatSinh;

    public LuotGuiXe() {
    }

    public LuotGuiXe(int idLuot, String maVeXe, LocalDateTime thoiGianGui, LocalDateTime thoiGianLay, BigDecimal tienPhatSinh) {
        this.idLuot = idLuot;
        this.maVeXe = maVeXe;
        this.thoiGianGui = thoiGianGui;
        this.thoiGianLay = thoiGianLay;
        this.tienPhatSinh = tienPhatSinh;
    }

    public int getIdLuot() {
        return idLuot;
    }

    public void setIdLuot(int idLuot) {
        this.idLuot = idLuot;
    }

    public String getMaVeXe() {
        return maVeXe;
    }

    public void setMaVeXe(String maVeXe) {
        this.maVeXe = maVeXe;
    }

    public LocalDateTime getThoiGianGui() {
        return thoiGianGui;
    }

    public void setThoiGianGui(LocalDateTime thoiGianGui) {
        this.thoiGianGui = thoiGianGui;
    }

    public LocalDateTime getThoiGianLay() {
        return thoiGianLay;
    }

    public void setThoiGianLay(LocalDateTime thoiGianLay) {
        this.thoiGianLay = thoiGianLay;
    }

    public BigDecimal getTienPhatSinh() {
        return tienPhatSinh;
    }

    public void setTienPhatSinh(BigDecimal tienPhatSinh) {
        this.tienPhatSinh = tienPhatSinh;
    }

}
