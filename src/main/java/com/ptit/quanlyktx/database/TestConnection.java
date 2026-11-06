package com.ptit.quanlyktx.database;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConnection {

    public static void main(String[] args) {

        try (Connection conn =
                DatabaseConnection.getConnection()) {

            System.out.println("Kết nối MySQL thành công!");

            System.out.println(
                "Database: " + conn.getCatalog()
            );

        } catch (SQLException e) {

            System.out.println("Kết nối thất bại!");
            System.out.println(e.getMessage());
        }
    }
}



/*
Nếu thành công, kết quả in ra là:
Kết nối MySQL thành công!
Database: quanlyktx

*/