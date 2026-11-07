package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.model.SinhVien;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;


import com.ptit.quanlyktx.dao.SinhVienDAO;
import java.sql.SQLException;

/**
 * Bước 2: Quản lý sinh viên bằng dữ liệu mẫu trong bộ nhớ.
 * Sau này thay các thao tác trên danhSach bằng SinhVienService/DAO.
 */
public class SinhVienController {
    
    private final SinhVienDAO sinhVienDAO = new SinhVienDAO();
        
    
    @FXML private TextField txtSearch;
    @FXML private TableView<SinhVien> tableStudents;
    @FXML private TableColumn<SinhVien, String> colMaSV, colHoTen, colSoCmt,
            colLop, colQueQuan, colSoPhong, colNgayVaoO;
    @FXML private Label lblCount;

    private final ObservableList<SinhVien> danhSach = FXCollections.observableArrayList();
    private FilteredList<SinhVien> filtered;
    
    @FXML
    private void initialize() {
        colMaSV.setCellValueFactory(c -> str(c.getValue().getMaSV()));
        colHoTen.setCellValueFactory(c -> str(c.getValue().getHoTen()));
        colSoCmt.setCellValueFactory(c -> str(c.getValue().getSoCmt()));
        colLop.setCellValueFactory(c -> str(c.getValue().getLop()));
        colQueQuan.setCellValueFactory(c -> str(c.getValue().getQueQuan()));
        colSoPhong.setCellValueFactory(c -> str(c.getValue().getSoPhong()));
        colNgayVaoO.setCellValueFactory(c -> str(
                c.getValue().getNgayVaoO() == null ? "" : c.getValue().getNgayVaoO().toString()));

        
       
        filtered = new FilteredList<>(danhSach, sv -> true);
        tableStudents.setItems(filtered);
        txtSearch.textProperty().addListener(
                (obs, oldValue, newValue) -> applySearch(newValue)
        );
        filtered.addListener(
                (javafx.collections.ListChangeListener<SinhVien>)
                change -> updateCount()
        );

        // Lấy dữ liệu thật từ MySQL
        loadFromDatabase();
    }
    
    private boolean loadFromDatabase() {
    try {
        // Đọc dữ liệu mới nhất từ MySQL
        danhSach.setAll(sinhVienDAO.findAll());

        // Cập nhật số lượng sinh viên
        updateCount();

        // Tải thành công
        return true;

    } catch (SQLException e) {

        // Thông báo lỗi, giữ nguyên danh sách đang hiển thị
        showDatabaseError("Không thể tải danh sách sinh viên", e);

        // Tải thất bại
        return false;
    }
}
    
    private ReadOnlyStringWrapper str(String text) {
        return new ReadOnlyStringWrapper(text == null ? "" : text);
    }

    private void applySearch(String text) {
        String term = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(sv -> term.isEmpty() || (
                (safe(sv.getMaSV()) + " " + safe(sv.getHoTen()) + " " + safe(sv.getLop())
                + " " + safe(sv.getQueQuan()) + " " + safe(sv.getSoPhong()))
                    .toLowerCase(Locale.ROOT).contains(term)));
        updateCount();
    }

    private String safe(String value) { return value == null ? "" : value; }

    private void updateCount() {
        lblCount.setText(
                "Hiển thị: " + filtered.size()
                + " / " + danhSach.size()
                + " sinh viên"
        );
    }

    @FXML
    private void handleAdd() {
        showForm(null).ifPresent(sv -> {
            try {
                // Thêm sinh viên vào MySQL
                boolean success = sinhVienDAO.insert(sv);

                if (success) {
                    // Tải lại danh sách từ MySQL
                    if (loadFromDatabase()) {
                        // Chọn sinh viên vừa thêm
                        selectStudent(sv.getMaSV());
                    }

                    info("Thêm sinh viên thành công!");
                } else {
                    warn("Không thể thêm sinh viên.");
                }

            } catch (SQLException e) {
                showDatabaseError("Không thể thêm sinh viên", e);
            }
        });  
    }

    @FXML
    private void handleEdit() {
        SinhVien current = tableStudents.getSelectionModel().getSelectedItem();

        if (current == null) {
            warn("Hãy chọn một sinh viên trong bảng để sửa.");
            return;
        }

        showForm(current).ifPresent(updated -> {
            try {
                // Cập nhật thông tin sinh viên trong MySQL
                boolean success = sinhVienDAO.update(updated);

                if (success) {
                    // Tải lại danh sách từ MySQL
                    if (loadFromDatabase()) {
                        // Chọn lại sinh viên vừa sửa
                        selectStudent(updated.getMaSV());
                    }

                    info("Cập nhật thông tin sinh viên thành công!");
                } else {
                    warn("Không tìm thấy sinh viên cần cập nhật trong MySQL.");
                }

            } catch (SQLException e) {
                showDatabaseError("Không thể cập nhật sinh viên", e);
            }
        });
    }

    @FXML
    private void handleDelete() {
        SinhVien current = tableStudents.getSelectionModel().getSelectedItem();

        if (current == null) {
            warn("Hãy chọn một sinh viên trong bảng để xóa.");
            return;
        }

        // Yêu cầu xác nhận trước khi xóa
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Bạn có chắc chắn muốn xóa sinh viên "
                        + current.getHoTen()
                        + " (" + current.getMaSV() + ") khỏi cơ sở dữ liệu?",
                ButtonType.YES,
                ButtonType.NO
        );

        confirm.setTitle("Xác nhận xóa");

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            // Xóa sinh viên trong MySQL theo mã sinh viên
            boolean success = sinhVienDAO.delete(current.getMaSV());

            if (success) {
                // Tải lại danh sách sau khi xóa
                loadFromDatabase();

                info("Xóa sinh viên thành công!");
            } else {
                warn("Không tìm thấy sinh viên cần xóa trong MySQL.");
            }

        } catch (SQLException e) {
            showDatabaseError("Không thể xóa sinh viên", e);
        }
    }

    @FXML
    private void handleRefresh() {
        txtSearch.clear();
        loadFromDatabase();
    }

    private Optional<SinhVien> showForm(SinhVien current) {
        Dialog<SinhVien> dialog = new Dialog<>();
        boolean editing = current != null;
        dialog.setTitle(editing ? "Sửa thông tin sinh viên" : "Thêm sinh viên");
        dialog.setHeaderText("Nhập thông tin theo 8 thuộc tính của bảng SINH_VIEN");
        ButtonType saveType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField ma = new TextField(editing ? safe(current.getMaSV()) : "");
        TextField cmt = new TextField(editing ? safe(current.getSoCmt()) : "");
        TextField ten = new TextField(editing ? safe(current.getHoTen()) : "");
        TextField lop = new TextField(editing ? safe(current.getLop()) : "");
        TextField que = new TextField(editing ? safe(current.getQueQuan()) : "");
        TextField phong = new TextField(editing ? safe(current.getSoPhong()) : "");
        DatePicker sinh = new DatePicker(editing ? current.getNgaySinh() : null);
        DatePicker vao = new DatePicker(editing ? current.getNgayVaoO() : LocalDate.now());
        ma.setPromptText("Tối đa 20 ký tự");
        cmt.setPromptText("Số CMT/CCCD duy nhất");
        phong.setPromptText("Ví dụ P101");
        // Không cho thay khóa chính khi sửa: bản demo tránh đổi FK liên quan.
        ma.setDisable(editing);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(12));
        Object[][] fields = {
            {"Mã sinh viên", ma}, {"Số CMT/CCCD", cmt}, {"Họ và tên", ten},
            {"Ngày sinh", sinh}, {"Lớp", lop}, {"Quê quán", que},
            {"Số phòng", phong}, {"Ngày vào ở", vao}
        };
        for (int i = 0; i < fields.length; i++) {
            grid.add(new Label((String) fields[i][0]), 0, i);
            grid.add((javafx.scene.Node) fields[i][1], 1, i);
        }
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(450);
        Label error = new Label();
        error.setStyle("-fx-text-fill: #B91C1C; -fx-wrap-text: true;");
        grid.add(error, 0, fields.length, 2, 1);
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String validation = validate(current, ma, cmt, ten, sinh, lop, que, phong, vao);
            if (validation != null) { error.setText(validation); event.consume(); }
        });
        dialog.setResultConverter(button -> button == saveType
                ? new SinhVien(ma.getText().trim(), cmt.getText().trim(), ten.getText().trim(),
                        sinh.getValue(), lop.getText().trim(), que.getText().trim(),
                        phong.getText().trim(), vao.getValue())
                : null);
        return dialog.showAndWait();
    }

    private String validate(SinhVien current, TextField ma, TextField cmt,
                            TextField ten, DatePicker sinh, TextField lop,
                            TextField que, TextField phong, DatePicker vao) {
        String id = ma.getText().trim(), cc = cmt.getText().trim();
        if (id.isEmpty() || cc.isEmpty() || ten.getText().isBlank()
                || lop.getText().isBlank() || que.getText().isBlank()
                || phong.getText().isBlank() || sinh.getValue() == null || vao.getValue() == null)
            return "Hãy điền đầy đủ các trường bắt buộc.";
        if (id.length() > 20 || cc.length() > 20 || ten.getText().length() > 100
                || lop.getText().length() > 50 || que.getText().length() > 100
                || phong.getText().length() > 10)
            return "Một hoặc nhiều trường vượt quá độ dài VARCHAR quy định ở YC1.";
        if (sinh.getValue().isAfter(LocalDate.now()) || vao.getValue().isBefore(sinh.getValue()))
            return "Ngày sinh/ngày vào ở không hợp lệ.";
        for (SinhVien sv : danhSach) {
            if (sv == current) continue;
            if (sv.getMaSV().equalsIgnoreCase(id)) return "Mã sinh viên đã tồn tại.";
            if (sv.getSoCmt().equalsIgnoreCase(cc)) return "Số CMT/CCCD đã tồn tại.";
        }
        // Khi kết nối MySQL cần kiểm tra phòng tồn tại và chưa vượt sức chứa tại Service/DB.
        return null;
    }

    private void warn(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Quản lý sinh viên");
        alert.showAndWait();
    }
    
    //Thông báo thành công
    private void info(String message) {
        Alert alert = new Alert(
                Alert.AlertType.INFORMATION,
                message,
                ButtonType.OK
        );

        alert.setTitle("Quản lý sinh viên");
        alert.showAndWait();
    }
    //Thông báo lỗi MySQL
    private void showDatabaseError(String action, SQLException e) {

        String detail;

        switch (e.getErrorCode()) {
            case 1062 ->
                detail = "Mã sinh viên hoặc số CMT/CCCD đã tồn tại.";

            case 1452 ->
                detail = "Dữ liệu tham chiếu không hợp lệ. "
                       + "Hãy kiểm tra số phòng.";

            case 1451 ->
                detail = "Không thể xóa hoặc thay đổi dữ liệu "
                       + "vì đang có bản ghi liên quan.";

            default ->
                detail = "Lỗi MySQL: " + e.getMessage();
        }

        Alert alert = new Alert(
                Alert.AlertType.ERROR,
                action + ".\n" + detail,
                ButtonType.OK
        );

        alert.setTitle("Lỗi cơ sở dữ liệu");
        alert.showAndWait();
    }
    //Chọn lại sinh viên sau khi tải dữ liệu
    private void selectStudent(String maSV) {

        for (SinhVien sv : filtered) {

            if (sv.getMaSV().equals(maSV)) {
                tableStudents.getSelectionModel().select(sv);
                tableStudents.scrollTo(sv);
                break;
            }
        }
    }  
    
}
