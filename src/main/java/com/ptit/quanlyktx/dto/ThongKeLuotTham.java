package com.ptit.quanlyktx.dto;

/** Kết quả thống kê số lần một khách đến thăm một SV trong khoảng ngày. */
public record ThongKeLuotTham(
        String maSinhVien,
        String tenSinhVien,
        String soCmtKhach,
        String tenKhach,
        long soLanDen
) {}
