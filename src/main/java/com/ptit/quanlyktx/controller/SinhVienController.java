package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.model.SinhVien;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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

/** Quản lý sinh viên nội trú: CRUD và thống kê dữ liệu thực từ cơ sở dữ liệu. */
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

        // Lấy dữ liệu thật từ cơ sở dữ liệu
        loadFromDatabase();
    }
    
    private boolean loadFromDatabase() {
    try {
        // Đọc dữ liệu mới nhất từ cơ sở dữ liệu
        danhSach.setAll(sinhVienDAO.findAll());

        // Đồng bộ bảng và thẻ thống kê với dữ liệu cơ sở dữ liệu mới nhất.
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

    // Thống kê từ toàn bộ danh sách đọc từ cơ sở dữ liệu, không phụ thuộc từ khóa tìm kiếm.
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
                // Thêm sinh viên vào cơ sở dữ liệu
                boolean success = sinhVienDAO.insert(sv);

                if (success) {
                    // Tải lại danh sách từ cơ sở dữ liệu
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
                // Cập nhật thông tin sinh viên trong cơ sở dữ liệu
                boolean success = sinhVienDAO.update(updated);

                if (success) {
                    // Tải lại danh sách từ cơ sở dữ liệu
                    if (loadFromDatabase()) {
                        // Chọn lại sinh viên vừa sửa
                        selectStudent(updated.getMaSV());
                    }

                    info("Cập nhật thông tin sinh viên thành công!");
                } else {
                    warn("Không tìm thấy hồ sơ sinh viên cần cập nhật trong cơ sở dữ liệu.");
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

        // Chỉ xác nhận xóa bằng dialog tiếng Việt theo phong cách ứng dụng.
        if (!confirmDelete(current)) {
            return;
        }

        try {
            // Xóa sinh viên trong cơ sở dữ liệu theo mã sinh viên
            boolean success = sinhVienDAO.delete(current.getMaSV());

            if (success) {
                // Tải lại danh sách sau khi xóa
                loadFromDatabase();

                info("Xóa sinh viên thành công!");
            } else {
                warn("Không tìm thấy hồ sơ sinh viên cần xóa trong cơ sở dữ liệu.");
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
        // Dialog có cửa sổ riêng nên cần nạp stylesheet tại đây.
        loadDialogCss(pane);

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
        ma.setEditable(!editing);
        if (editing) {
            ma.getStyleClass().add("sv-field-readonly");
        }

        GridPane grid = new GridPane();
        grid.setPadding(
            new javafx.geometry.Insets(0, 20, 0, 20)
        );
        grid.setHgap(20);
        grid.setVgap(17);
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

        // Header có điểm nhấn; hai cột thông tin và banner rõ ràng, thoáng hơn.
        Label mark = new Label(editing ? "✎" : "+");
        mark.getStyleClass().add("sv-form-mark");
        Label title = new Label(editing ? "Cập nhật hồ sơ sinh viên" : "Thêm sinh viên nội trú");
        title.getStyleClass().add("sv-dialog-title");
        Label subtitle = new Label("Thông tin sinh viên được lưu trong cơ sở dữ liệu của hệ thống.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("sv-dialog-description");
        VBox titleBox = new VBox(5, title, subtitle);
        HBox formHeader = new HBox(15, mark, titleBox);
        formHeader.setAlignment(Pos.CENTER_LEFT);
        formHeader.getStyleClass().add("sv-dialog-header");

        Label hintIcon = new Label("i");
        hintIcon.getStyleClass().add("sv-hint-icon");
        Label tip = new Label(editing
                ? "Mã sinh viên được giữ nguyên để bảo đảm liên kết dữ liệu."
                : "Điền đầy đủ 8 trường thông tin và kiểm tra phòng trước khi lưu.");
        tip.setWrapText(true);
        tip.getStyleClass().add("sv-dialog-banner-label");
        HBox banner = new HBox(10, hintIcon, tip);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().add("sv-dialog-banner");
        HBox.setHgrow(tip, Priority.ALWAYS);

        Label section = new Label("THÔNG TIN HỒ SƠ");
        section.getStyleClass().add("sv-dialog-section-label");

        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("sv-validation-error");
        error.setVisible(false);
        error.managedProperty().bind(error.visibleProperty());

        VBox content = new VBox(18, formHeader, banner, section, grid, error);
        content.getStyleClass().add("sv-dialog-content");
        pane.setContent(content);
        pane.setPrefWidth(760);
        attachOwner(dialog);

        Button saveButton = (Button) pane.lookupButton(saveType);
        saveButton.getStyleClass().add("sv-dialog-save");
        Button cancelButton = (Button) pane.lookupButton(cancelType);
        cancelButton.getStyleClass().add("sv-dialog-cancel");
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String validation = validate(current, ma, cmt, ten, sinh, lop, que, phong, vao);
            if (validation != null) {
                error.setText(validation);
                error.setVisible(true);
                event.consume();
            } else {
                error.setVisible(false);
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
        Label required = new Label("*");
        required.getStyleClass().add("sv-field-required");
        HBox captionRow = new HBox(4, title, required);
        captionRow.setAlignment(Pos.CENTER_LEFT);
        if (input instanceof TextField) {
            input.getStyleClass().add("sv-field-input");
        } else if (input instanceof DatePicker) {
            input.getStyleClass().add("sv-date-input");
        }
        input.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(8, captionRow, input);
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
            return "Một số thông tin quá dài. Vui lòng rút ngắn nội dung và thử lại.";
        if (sinh.getValue().isAfter(LocalDate.now()) || vao.getValue().isBefore(sinh.getValue()))
            return "Ngày sinh/ngày vào ở không hợp lệ.";
        for (SinhVien sv : danhSach) {
            if (sv == current) continue;
            if (sv.getMaSV().equalsIgnoreCase(id)) return "Mã sinh viên đã tồn tại.";
            if (sv.getSoCmt().equalsIgnoreCase(cc)) return "Số CMT/CCCD đã tồn tại.";
        }
        // Khi kết nối cơ sở dữ liệu cần kiểm tra phòng tồn tại và chưa vượt sức chứa tại Service/DB.
        return null;
    }

    // Mọi thông báo trong module Sinh viên dùng cùng một giao diện.
    private void warn(String message) {
        showNotice("warning", "Cần kiểm tra thông tin", message);
    }

    private void info(String message) {
        showNotice("success", "Thao tác thành công", message);
    }

    private void showNotice(String kind, String heading, String message) {
        ButtonType ok = new ButtonType("Đã hiểu", ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = createMessageDialog(kind, heading, message, ok);
        Button button = (Button) dialog.getDialogPane().lookupButton(ok);
        button.getStyleClass().add("sv-message-primary");
        dialog.showAndWait();
    }

    private boolean confirmDelete(SinhVien sv) {
        ButtonType delete = new ButtonType("Xóa sinh viên", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        String text = "Bạn có chắc muốn xóa hồ sơ của " + sv.getHoTen()
                + " (" + sv.getMaSV() + ")?\n"
                + "Dữ liệu liên quan có thể bị ảnh hưởng. Hành động này không thể hoàn tác.";
        Dialog<ButtonType> dialog = createMessageDialog(
                "confirm", "Xác nhận xóa sinh viên", text, delete, cancel);
        ((Button) dialog.getDialogPane().lookupButton(delete))
                .getStyleClass().add("sv-message-danger-button");
        ((Button) dialog.getDialogPane().lookupButton(cancel))
                .getStyleClass().add("sv-message-secondary");
        return dialog.showAndWait().filter(delete::equals).isPresent();
    }

    private Dialog<ButtonType> createMessageDialog(String kind, String heading,
                                                    String message, ButtonType... buttons) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("PTIT KTX - Quản lý sinh viên");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.setResizable(false);
        attachOwner(dialog);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(buttons);
        pane.getStyleClass().addAll("sv-message-pane", "sv-message-" + kind);
        loadDialogCss(pane);

        String symbol = switch (kind) {
            case "success" -> "✓";
            case "warning" -> "!";
            case "error" -> "×";
            default -> "?";
        };
        Label icon = new Label(symbol);
        icon.getStyleClass().add("sv-message-icon");
        Label title = new Label(heading);
        title.getStyleClass().add("sv-message-title");
        title.setWrapText(true);

        HBox header = new HBox(14, icon, title);
        header.setAlignment(Pos.CENTER_LEFT);

        Region rule = new Region();
        rule.getStyleClass().add("sv-message-rule");
        rule.setMinHeight(1);

        Label body = new Label(message);
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);
        body.getStyleClass().add("sv-message-body");

        VBox layout = new VBox(17, header, rule, body);
        layout.getStyleClass().add("sv-message-content");
        pane.setContent(layout);
        pane.setPrefWidth(465);
        dialog.setResultConverter(button -> button);
        return dialog;
    }

    private void attachOwner(Dialog<?> dialog) {
        if (tableStudents != null && tableStudents.getScene() != null
                && tableStudents.getScene().getWindow() != null) {
            dialog.initOwner(tableStudents.getScene().getWindow());
        }
    }

    private void loadDialogCss(DialogPane pane) {
        java.net.URL css = getClass().getResource(
                "/com/ptit/quanlyktx/css/sinhvien.css");
        if (css != null) {
            pane.getStylesheets().add(css.toExternalForm());
        }
    }

    // Giữ nguyên nghiệp vụ CSDL, chỉ chuyển lỗi kỹ thuật thành thông báo dễ hiểu.
    private void showDatabaseError(String action, SQLException e) {
        String raw = e.getMessage() == null ? "" : e.getMessage();
        String reason = raw.toLowerCase(Locale.ROOT);
        String detail;

        if (reason.contains("sức chứa") || reason.contains("suc chua")
                || reason.contains("phòng đã đủ") || reason.contains("phong da du")
                || reason.contains("phòng đã đầy") || reason.contains("phong da day")) {
            detail = "Phòng đã đủ số người ở tối đa. Vui lòng chọn phòng còn chỗ.";
        } else if (reason.contains("unique constraint") || reason.contains("duplicate entry")
                || reason.contains("sinh_vien.ma_sv") || reason.contains("sinh_vien.so_cmt")) {
            detail = "Mã sinh viên hoặc số CMT/CCCD đã tồn tại trong cơ sở dữ liệu.";
        } else if (reason.contains("foreign key") || reason.contains("khóa ngoại")) {
            detail = action.toLowerCase(Locale.ROOT).contains("xóa")
                    ? "Không thể xóa hồ sơ vì đang có dữ liệu liên quan."
                    : "Phòng ở không tồn tại hoặc dữ liệu liên kết chưa hợp lệ.";
        } else if (reason.contains("database is locked") || reason.contains("database locked")) {
            detail = "Cơ sở dữ liệu đang được sử dụng. Vui lòng thử lại sau.";
        } else if (reason.contains("check constraint")) {
            detail = "Dữ liệu không đáp ứng một ràng buộc hợp lệ của hệ thống.";
        } else {
            detail = "Không thể hoàn thành thao tác với cơ sở dữ liệu. "
                    + "Vui lòng kiểm tra thông tin hoặc thử lại.";
        }
        // Lưu chi tiết kỹ thuật cho người phát triển kiểm tra, không hiện lên dialog.
        System.err.println("[SinhVien - Database] " + action + ": " + raw);
        showNotice("error", action, detail);
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
