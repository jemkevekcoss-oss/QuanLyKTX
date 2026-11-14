
package com.ptit.quanlyktx.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Ket noi SQLite cho ung dung Quan ly Ky tuc xa. */
public final class DatabaseConnection {

    private static final String DB_NAME = "quanlyktx.db";

    private DatabaseConnection() {
    }

    public static Path getDatabasePath() {

        // Duong dan tuy chinh neu duoc cung cap
        String customPath = System.getProperty("ktx.db.path");

        if (customPath != null && !customPath.isBlank()) {
            return Path.of(customPath)
                    .toAbsolutePath().normalize();
        }

        // Khi chay bang file .exe do jpackage tao ra
        String launcherPath = System.getProperty("jpackage.app-path");

        if (launcherPath != null && !launcherPath.isBlank()) {
            Path launcherDir = Path.of(launcherPath)
                    .toAbsolutePath().getParent();

            if (launcherDir != null) {
                return launcherDir.resolve("database")
                        .resolve(DB_NAME).normalize();
            }
        }

        // Khi chay truc tiep trong NetBeans
        return Path.of(
                System.getProperty("user.dir"),
                "database",
                DB_NAME
        ).toAbsolutePath().normalize();
    }

    public static Connection getConnection() throws SQLException {

        Path dbPath = getDatabasePath();

        // Ngan SQLite tu tao file DB rong khi sai duong dan
        if (!Files.isRegularFile(dbPath)) {
            throw new SQLException(
                    "Khong tim thay CSDL SQLite: " + dbPath
            );
        }

        Connection conn = DriverManager.getConnection(
                "jdbc:sqlite:" + dbPath
        );

        try (Statement stmt = conn.createStatement()) {

            // Bat rang buoc khoa ngoai
            stmt.execute("PRAGMA foreign_keys = ON");

            // Cho toi da 5 giay neu CSDL dang bi khoa
            stmt.execute("PRAGMA busy_timeout = 5000");

        } catch (SQLException e) {
            try {
                conn.close();
            } catch (SQLException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }

        return conn;
    }
}
