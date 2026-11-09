package com.ptit.quanlyktx.controller;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Điều hướng giữa các màn hình. Không chứa nghiệp vụ Dashboard/SQL. */
public class MainController {

    // fx:id phải khớp MainView.fxml hiện tại.
    @FXML private Label pageTitle;
    @FXML private VBox contentArea;

    @FXML
    private void initialize() {
        showDashboard();
    }

    private void loadPage(String fxmlFile, String title) {
        String path = "/com/ptit/quanlyktx/fxml/" + fxmlFile;
        URL resource = getClass().getResource(path);
        if (resource == null) {
            showError("Không tìm thấy file giao diện: " + path);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
               
            if (loader.getController() instanceof DichVuController dv) {
            dv.setOnNavigate(page -> {
                if ("reports".equals(page)) {
                    showReports();
                }
            });
}
            
            // Cấp callback để nút truy cập nhanh ở Dashboard đổi màn hình.
            if (loader.getController() instanceof DashboardController dashboard) {
                dashboard.setOnNavigate(this::navigateTo);
            }

            contentArea.getChildren().setAll(view);
            pageTitle.setText(title);
        } catch (IOException | RuntimeException ex) {

            ex.printStackTrace();

            // Tìm nguyên nhân sâu nhất
            Throwable rootCause = ex;

            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }

            String errorMessage =
                    "Không tải được: " + fxmlFile
                    + "\n\nLoại lỗi: "
                    + rootCause.getClass().getSimpleName()
                    + "\n\nNguyên nhân: "
                    + rootCause.getMessage();

            showError(errorMessage);
}
    }

    private void navigateTo(String page) {
        switch (page) {
            case "bills" -> showBills();
            case "reports" -> showReports();
            case "students" -> showStudents();
            default -> showDashboard();
        }
    }

    // Giữ tạm màn hình chờ để menu cũ không bị lỗi khi các view chưa được tạo.
    private void showPlaceholder(String title, String message) {
        pageTitle.setText(title);
        contentArea.getChildren().clear();
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        Label description = new Label(message);
        description.setWrapText(true);
        description.getStyleClass().add("placeholder-text");
        VBox panel = new VBox(15, heading, description);
        panel.getStyleClass().add("content-panel");
        contentArea.getChildren().add(panel);
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    @FXML private void showDashboard() {
        loadPage("DashboardView.fxml", "Tổng quan ký túc xá");
    }

    @FXML private void showStudents() {
        loadPage("SinhVienView.fxml", "Quản lý sinh viên");
    }

//    @FXML private void showRooms() {
//        showPlaceholder("Quản lý phòng ở", "Danh sách phòng, sức chứa, đơn giá và kiểm tra số chỗ còn trống.");
//    }
    @FXML
    private void showRooms() {
        loadPage("PhongView.fxml", "Quản lý phòng ở");
    }

//    @FXML private void showVisitors() {
//        showPlaceholder("Quản lý khách thăm", "Ghi nhận khách đến và thống kê theo tuần hoặc tháng.");
//    }
    @FXML
    private void showVisitors() {
        loadPage("KhachThamView.fxml", "Quản lý khách thăm");
    }

//    @FXML private void showServices() {
//        showPlaceholder("Quản lý dịch vụ", "Danh mục dịch vụ và lịch sử sử dụng của sinh viên.");
//    }
    @FXML
    private void showServices() {
        loadPage("DichVuView.fxml", "Quản lý dịch vụ");
    }

//    @FXML private void showParking() {
//        showPlaceholder("Quản lý gửi xe", "Đăng ký vé xe tháng, ghi nhận lượt gửi/lấy và tính phí.");
//    }
    @FXML
    private void showParking() {
        loadPage("GuiXeView.fxml", "Quản lý gửi xe");
    }

//    @FXML private void showBills() {
//        showPlaceholder("Hóa đơn thanh toán", "Tính tiền phòng và dịch vụ theo tháng cho từng sinh viên.");
//    }
    @FXML 
    private void showBills() {
       loadPage("HoaDonView.fxml", "Hóa đơn thanh toán");
    }

    @FXML private void showReports() {
        showPlaceholder("Thống kê và báo cáo", "Thống kê doanh thu dịch vụ, khách thăm và các khoản phải thu.");
    }
}
