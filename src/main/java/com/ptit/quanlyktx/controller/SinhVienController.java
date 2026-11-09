package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.model.SinhVien;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


import com.ptit.quanlyktx.dao.SinhVienDAO;
import java.sql.SQLException;

/** Quản lý sinh viên nội trú: CRUD và thống kê dữ liệu thực từ MySQL. */
public class SinhVienController {
    
    private final SinhVienDAO sinhVienDAO = new SinhVienDAO();
        
    
    @FXML private TextField txtSearch;
    @FXML private TableView<SinhVien> tableStudents;
    @FXML private TableColumn<SinhVien, String> colMaSV, colHoTen, colSoCmt,
            colLop, colQueQuan, colSoPhong, colNgayVaoO;
    @FXML private Label lblCount;
    @FXML private Label lblTotalStudents;
    @FXML private Label lblOccupiedRooms;
    @FXML private Label lblNewThisMonth;
    @FXML private TableColumn<SinhVien, String> colNgaySinh;

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObservableList<SinhVien> danhSach = FXCollections.observableArrayList();
    private FilteredList<SinhVien> filtered;
    
    @FXML
    private void initialize() {
        colMaSV.setCellValueFactory(c -> str(c.getValue().getMaSV()));
        colHoTen.setCellValueFactory(c -> str(c.getValue().getHoTen()));
        colSoCmt.setCellValueFactory(c -> str(c.getValue().getSoCmt()));
        colNgaySinh.setCellValueFactory(c -> str(formatDate(c.getValue().getNgaySinh())));
        colLop.setCellValueFactory(c -> str(c.getValue().getLop()));
        colQueQuan.setCellValueFactory(c -> str(c.getValue().getQueQuan()));
        colSoPhong.setCellValueFactory(c -> str(c.getValue().getSoPhong()));
        colNgayVaoO.setCellValueFactory(c -> str(formatDate(c.getValue().getNgayVaoO())));
        // Đặt bằng Java để tránh lỗi FXMLLoader coerce CONSTRAINED_RESIZE_POLICY.
        tableStudents.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        
       
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

        // Đồng bộ bảng và thẻ thống kê với dữ liệu MySQL mới nhất.
        updateCount();
        updateStatistics();

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

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_DATE);
    }

    // Thống kê từ toàn bộ danh sách đọc từ MySQL, không phụ thuộc từ khóa tìm kiếm.
    private void updateStatistics() {
        lblTotalStudents.setText(String.valueOf(danhSach.size()));

        Set<String> occupied = danhSach.stream()
                .map(SinhVien::getSoPhong)
                .filter(p -> p != null && !p.isBlank())
                .map(p -> p.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        lblOccupiedRooms.setText(String.valueOf(occupied.size()));

        YearMonth now = YearMonth.now();
        long newStudents = danhSach.stream()
                .map(SinhVien::getNgayVaoO)
                .filter(date -> date != null && YearMonth.from(date).equals(now))
                .count();
        lblNewThisMonth.setText(String.valueOf(newStudents));
    }

    private void applySearch(String text) {
        String term = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(sv -> term.isEmpty() || (
                (safe(sv.getMaSV()) + " " + safe(sv.getHoTen()) + " "
                + safe(sv.getSoCmt()) + " " + safe(sv.getLop())
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

    /** Cửa sổ thêm/sửa: chỉ đổi bố cục, vẫn dùng hàm validate và DAO cũ. */
    private Optional<SinhVien> showForm(SinhVien current) {
        Dialog<SinhVien> dialog = new Dialog<>();
        boolean editing = current != null;
        dialog.setTitle(editing ? "Sửa thông tin sinh viên" : "Thêm sinh viên");
        dialog.setHeaderText(null);
        dialog.setResizable(true);

        ButtonType saveType = new ButtonType(
                editing ? "Lưu thay đổi" : "Thêm sinh viên", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(saveType, cancelType);
        pane.getStyleClass().add("sv-dialog-pane");
        // Dialog là một cửa sổ riêng, phải nạp CSS riêng cho nó.
        java.net.URL css = getClass().getResource(
                "/com/ptit/quanlyktx/css/sinhvien.css");
        if (css != null) {
            pane.getStylesheets().add(css.toExternalForm());
        }

        TextField ma = new TextField(editing ? safe(current.getMaSV()) : "");
        TextField cmt = new TextField(editing ? safe(current.getSoCmt()) : "");
        TextField ten = new TextField(editing ? safe(current.getHoTen()) : "");
        TextField lop = new TextField(editing ? safe(current.getLop()) : "");
        TextField que = new TextField(editing ? safe(current.getQueQuan()) : "");
        TextField phong = new TextField(editing ? safe(current.getSoPhong()) : "");
        DatePicker sinh = new DatePicker(editing ? current.getNgaySinh() : null);
        DatePicker vao = new DatePicker(editing ? current.getNgayVaoO() : LocalDate.now());
        ma.setPromptText("Ví dụ B22DCCN001");
        cmt.setPromptText("Số CMT/CCCD duy nhất");
        ten.setPromptText("Nhập họ và tên đầy đủ");
        lop.setPromptText("Ví dụ D22CNPM1");
        que.setPromptText("Tỉnh / thành phố");
        phong.setPromptText("Ví dụ P101");
        // Giữ khóa chính khi sửa để tránh ảnh hưởng FK các bảng liên quan.
        ma.setDisable(editing);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(13);
        ColumnConstraints left = new ColumnConstraints();
        ColumnConstraints right = new ColumnConstraints();
        left.setPercentWidth(50);
        right.setPercentWidth(50);
        left.setHgrow(Priority.ALWAYS);
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(left, right);
        grid.add(formField("Mã sinh viên", ma), 0, 0);
        grid.add(formField("Họ và tên", ten), 1, 0);
        grid.add(formField("Số CMT/CCCD", cmt), 0, 1);
        grid.add(formField("Ngày sinh", sinh), 1, 1);
        grid.add(formField("Lớp", lop), 0, 2);
        grid.add(formField("Quê quán", que), 1, 2);
        grid.add(formField("Phòng ở", phong), 0, 3);
        grid.add(formField("Ngày vào ở", vao), 1, 3);

        Label title = new Label(editing ? "Cập nhật hồ sơ sinh viên" : "Thêm sinh viên nội trú");
        title.getStyleClass().add("sv-dialog-title");
        Label subtitle = new Label("Thông tin được lưu trực tiếp vào bảng SINH_VIEN trong MySQL.");
        subtitle.getStyleClass().add("sv-dialog-description");
        Label tip = new Label(editing
                ? "Mã sinh viên không thể thay đổi khi chỉnh sửa hồ sơ."
                : "Điền đủ 8 trường thông tin. Kiểm tra số phòng trước khi lưu.");
        tip.getStyleClass().add("sv-dialog-banner-label");
        VBox banner = new VBox(tip);
        banner.getStyleClass().add("sv-dialog-banner");

        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("sv-validation-error");

        VBox content = new VBox(14, title, subtitle, banner, grid, error);
        content.getStyleClass().add("sv-dialog-content");
        pane.setContent(content);
        pane.setPrefWidth(690);

        Button saveButton = (Button) pane.lookupButton(saveType);
        saveButton.getStyleClass().add("sv-dialog-save");
        Button cancelButton = (Button) pane.lookupButton(cancelType);
        cancelButton.getStyleClass().add("sv-dialog-cancel");
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String validation = validate(current, ma, cmt, ten, sinh, lop, que, phong, vao);
            if (validation != null) {
                error.setText(validation);
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == saveType
                ? new SinhVien(ma.getText().trim(), cmt.getText().trim(), ten.getText().trim(),
                        sinh.getValue(), lop.getText().trim(), que.getText().trim(),
                        phong.getText().trim(), vao.getValue())
                : null);
        return dialog.showAndWait();
    }

    private VBox formField(String caption, Control input) {
        Label title = new Label(caption);
        title.getStyleClass().add("sv-field-label");
        if (input instanceof TextField) {
            input.getStyleClass().add("sv-field-input");
        }
        input.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(6, title, input);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
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
