package com.ptit.quanlyktx.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Kết nối trực tiếp MySQL bằng JDBC cho bài tập lớn. */
public final class DatabaseConnection {
    // TODO: Thay ten CSDL bang ten chinh xac trong MySQL Workbench cua ban.
    private static final String URL = "jdbc:mysql://localhost:3306/quanlyktx";
    private static final String USER = "root";
    // TODO: Nhap mat khau MySQL tren may cua ban. Khong commit mat khau that len GitHub.
    private static final String PASSWORD = "123456";

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}


