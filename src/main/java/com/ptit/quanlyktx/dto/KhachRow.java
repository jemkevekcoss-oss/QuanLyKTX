package com.ptit.quanlyktx.dto;

import java.time.LocalDate;

public record KhachRow(int idKhach, String soCmt, String hoTen, LocalDate ngaySinh) {
    @Override public String toString() { return hoTen + " (" + soCmt + ")"; }
}
