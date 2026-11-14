package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.dao.TongQuanDAO;
import com.ptit.quanlyktx.dto.TinhTrangPhongRow;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;

/** Trang Tổng quan. Callback được MainController gán qua setOnNavigate(). */
public class DashboardController {
    @FXML private Label lblTongSinhVien, lblTongPhong, lblChoTrong, lblTongDichVu;
    @FXML private Label lblTyLe, lblSucChua, lblPhongDay, lblPhongConCho, lblPhongTrong, lblChoConLai;
    @FXML private Label lblCapNhat;
    @FXML private VBox danhSachPhong;
    @FXML private Arc arcDaO;

    private final TongQuanDAO dao = new TongQuanDAO();
    private Consumer<String> onNavigate;

    @FXML private void initialize() { lamMoi(); }

    public void setOnNavigate(Consumer<String> callback) { this.onNavigate = callback; }
    private void navigate(String key) {
        if (onNavigate != null) onNavigate.accept(key);
        else new Alert(Alert.AlertType.WARNING, "Chưa gán điều hướng từ MainController.").showAndWait();
    }

    @FXML private void moSinhVien() { navigate("students"); }
    @FXML private void moPhong() { navigate("rooms"); }
    @FXML private void moHoaDon() { navigate("bills"); }
    @FXML private void moGuiXe() { navigate("parking"); }

    @FXML public void lamMoi() {
        try {
            TongQuanDAO.SoLieu x = dao.taiSoLieu();
            lblTongSinhVien.setText(Integer.toString(x.tongSinhVien()));
            lblTongPhong.setText(Integer.toString(x.tongPhong()));
            lblChoTrong.setText(Integer.toString(x.tongChoTrong()));
            lblTongDichVu.setText(Integer.toString(x.tongDichVu()));
            lblPhongDay.setText(Integer.toString(x.phongDay()));
            lblPhongConCho.setText(Integer.toString(x.phongConCho()));
            lblPhongTrong.setText(Integer.toString(x.phongTrong()));
            lblChoConLai.setText(Integer.toString(x.tongChoTrong()));
            lblSucChua.setText(x.tongSinhVien() + " / " + x.tongSucChua() + " chỗ");
            double ratio = x.tongSucChua() == 0 ? 0 : (double) x.tongSinhVien() / x.tongSucChua();
            lblTyLe.setText(String.format(java.util.Locale.forLanguageTag("vi-VN"), "%.1f%%", 100 * ratio));
            arcDaO.setLength(-360.0 * Math.min(1, Math.max(0, ratio)));
            hienThiPhong(x.topPhong());
            //lblCapNhat.setText("Đã cập nhật lúc " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (SQLException | RuntimeException e) {
            //lblCapNhat.setText("Không tải được dữ liệu từ CSDL");
            new Alert(Alert.AlertType.ERROR, "Không thể tải dữ liệu Tổng quan từ cơ sở dữ liệu:\n" + e.getMessage()).showAndWait();
        }
    }

    private void hienThiPhong(java.util.List<TinhTrangPhongRow> list) {
        danhSachPhong.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("Chưa có phòng trong cơ sở dữ liệu.");
            empty.getStyleClass().add("dash-muted");
            danhSachPhong.getChildren().add(empty);
            return;
        }
        for (TinhTrangPhongRow p : list) {
            Label room = new Label(p.soPhong());
            room.getStyleClass().add("dash-room-code");
            room.setMinWidth(64);
            Label detail = new Label(p.dangO() + "/" + p.sucChua() + " sinh viên");
            detail.getStyleClass().add("dash-room-detail");
            detail.setMinWidth(112);
            detail.setAlignment(Pos.CENTER_RIGHT);
            ProgressBar progress = new ProgressBar(p.tyLeLapDay());
            progress.setMaxWidth(Double.MAX_VALUE);
            progress.setPrefHeight(13);
            progress.getStyleClass().add("dash-room-progress");
            HBox.setHgrow(progress, Priority.ALWAYS);
            HBox row = new HBox(13, room, progress, detail);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-room-row");
            danhSachPhong.getChildren().add(row);
        }
    }
}
