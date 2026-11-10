package com.ptit.quanlyktx.dto;

import java.time.LocalDate;

public record ThongKeKhachRow(String maSv, String tenSv, String lop, String soPhong,
        int idKhach, String soCmt, String tenKhach, LocalDate ngaySinh,
        long soLanDen, LocalDate ngayDenGanNhat) { }
