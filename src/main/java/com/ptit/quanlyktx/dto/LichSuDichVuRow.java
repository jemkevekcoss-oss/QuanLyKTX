package com.ptit.quanlyktx.dto;

import com.ptit.quanlyktx.model.SuDungDichVu;

/** Một lượt sử dụng kèm tên sinh viên/dịch vụ/đơn vị để hiển thị. */
public class LichSuDichVuRow {
    private final SuDungDichVu suDung;
    private final String tenSV;
    private final String tenDV;
    private final String donViTinh;

    public LichSuDichVuRow(SuDungDichVu suDung, String tenSV, String tenDV, String donViTinh) {
        this.suDung = suDung;
        this.tenSV = tenSV;
        this.tenDV = tenDV;
        this.donViTinh = donViTinh;
    }
    public SuDungDichVu getSuDung() { return suDung; }
    public String getTenSV() { return tenSV; }
    public String getTenDV() { return tenDV; }
    public String getDonViTinh() { return donViTinh; }
}
