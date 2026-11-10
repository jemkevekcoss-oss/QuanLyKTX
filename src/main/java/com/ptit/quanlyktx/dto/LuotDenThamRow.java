package com.ptit.quanlyktx.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LuotDenThamRow(
        int idLuot, int idKhach, String tenKhach, String soCmt,
        String maSv, String tenSv, LocalDate ngayDen,
        LocalDateTime thoiGianDen, LocalDateTime thoiGianDi) {

    public String trangThai() {
        if (thoiGianDen == null) return "Thiếu giờ (lịch sử cũ)";
        return thoiGianDi == null ? "Đang thăm" : "Đã rời KTX";
    }
}
