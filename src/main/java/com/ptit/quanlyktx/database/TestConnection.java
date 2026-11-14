package com.ptit.quanlyktx.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConnection {

    public static void main(String[] args) {

        System.out.println("Duong dan CSDL: "
                + DatabaseConnection.getDatabasePath());

        try (
            Connection conn = DatabaseConnection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM SINH_VIEN"
            )
        ) {

            System.out.println("Ket noi SQLite thanh cong!");

            if (rs.next()) {
                System.out.println(
                    "Tong so sinh vien: " + rs.getInt(1)
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
