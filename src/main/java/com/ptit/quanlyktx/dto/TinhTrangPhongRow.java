package com.ptit.quanlyktx.dto;

/** Một phòng và số sinh viên hiện đang ở; phục vụ Dashboard. */
public record TinhTrangPhongRow(String soPhong, int sucChua, int dangO) {
    public int conTrong() { return Math.max(0, sucChua - dangO); }
    public double tyLeLapDay() {
        return sucChua > 0 ? Math.min(1.0, (double) dangO / sucChua) : 0.0;
    }
}
