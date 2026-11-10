package com.ptit.quanlyktx.dto;

public record SinhVienOption(String maSv, String hoTen) {
    @Override public String toString() { return maSv + " — " + hoTen; }
}
