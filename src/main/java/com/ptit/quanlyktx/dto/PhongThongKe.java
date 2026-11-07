package com.ptit.quanlyktx.dto;

import com.ptit.quanlyktx.model.Phong;
import java.math.BigDecimal;

/**
 * Dữ liệu hiển thị của một phòng. Không phải bảng mới trong MySQL:
 * các giá trị số người/chỗ trống được tính từ PHONG và SINH_VIEN.
 */
public final class PhongThongKe {
    private final Phong phong;
    private final int soNguoiDangO;

    public PhongThongKe(Phong phong, int soNguoiDangO) {
        this.phong = phong;
        this.soNguoiDangO = soNguoiDangO;
    }

    public Phong getPhong() { return phong; }
    public String getSoPhong() { return phong.getSoPhong(); }
    public String getLoaiPhong() { return phong.getLoaiPhong(); }
    public BigDecimal getDonGia() { return phong.getDonGia(); }
    public int getSoNguoiToiDa() { return phong.getSoNguoiToiDa(); }
    public int getSoNguoiDangO() { return soNguoiDangO; }
    public int getSoChoConTrong() { return Math.max(0, getSoNguoiToiDa() - soNguoiDangO); }

    public String getTrangThai() {
        if (soNguoiDangO == 0) return "Trống";
        if (soNguoiDangO >= getSoNguoiToiDa()) return "Đầy";
        return "Còn chỗ";
    }
}
