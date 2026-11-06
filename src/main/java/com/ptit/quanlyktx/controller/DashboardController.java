package com.ptit.quanlyktx.controller;

import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Chỉ quản lý nội dung Dashboard; sẽ nối với ThongKeDAO sau. */
public class DashboardController {

    @FXML private Label studentCount;
    @FXML private Label roomCount;
    @FXML private Label serviceCount;
    @FXML private Label parkingCount;

    private Consumer<String> onNavigate;

    @FXML
    private void initialize() {
        // Chưa truy vấn MySQL: hiển thị dấu —, KHÔNG hiển thị số giả.
        studentCount.setText("—");
        roomCount.setText("—");
        serviceCount.setText("—");
        parkingCount.setText("—");
    }

    public void setOnNavigate(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
    }

    @FXML private void handleBills() {
        if (onNavigate != null) onNavigate.accept("bills");
    }

    @FXML private void handleReports() {
        if (onNavigate != null) onNavigate.accept("reports");
    }

    // Về sau ThongKeDAO có thể gọi lại phương thức này để cập nhật số thật.
    public void updateStatistics(int students, int rooms, int services, int parking) {
        studentCount.setText(Integer.toString(students));
        roomCount.setText(Integer.toString(rooms));
        serviceCount.setText(Integer.toString(services));
        parkingCount.setText(Integer.toString(parking));
    }
}
