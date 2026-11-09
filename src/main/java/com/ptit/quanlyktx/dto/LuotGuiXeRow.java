package com.ptit.quanlyktx.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Kết quả đọc LUOT_GUI_XE cùng thông tin xe/sinh viên. */
public record LuotGuiXeRow(
        int idLuot, String maVeXe, String bienSoXe,
        String maSV, String hoTen,
        LocalDateTime thoiGianGui, LocalDateTime thoiGianLay,
        BigDecimal tienPhatSinh) {

    public boolean dangGui() { return thoiGianLay == null; }
}
