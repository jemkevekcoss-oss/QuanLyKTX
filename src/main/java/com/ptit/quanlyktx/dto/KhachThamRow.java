package com.ptit.quanlyktx.dto;

import com.ptit.quanlyktx.model.KhachTham;

/** Một lượt đến thăm, kèm thông tin SV từ JOIN để hiển thị. Không phải bảng mới. */
public class KhachThamRow {
    private final KhachTham khach;
    private final String tenSinhVien;
    private final String soPhong;

    public KhachThamRow(KhachTham khach, String tenSinhVien, String soPhong) {
        this.khach = khach;
        this.tenSinhVien = tenSinhVien;
        this.soPhong = soPhong;
    }

    public KhachTham getKhach() { return khach; }
    public String getTenSinhVien() { return tenSinhVien; }
    public String getSoPhong() { return soPhong; }
}
