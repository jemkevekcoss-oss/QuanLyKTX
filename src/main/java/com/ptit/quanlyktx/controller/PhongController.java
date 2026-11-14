package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.dao.PhongDAO;
import com.ptit.quanlyktx.dto.PhongThongKe;
import com.ptit.quanlyktx.model.Phong;
import com.ptit.quanlyktx.model.SinhVien;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Điều khiển PhongView.fxml – dữ liệu đọc từ cơ sở dữ liệu, không hard-code. */
public class PhongController {

    private static final String ALL = "Tất cả trạng thái";
    private static final String EMPTY = "Trống";
    private static final String AVAILABLE = "Còn chỗ";
    private static final String FULL = "Đầy";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PhongDAO phongDAO = new PhongDAO();
    private final ObservableList<PhongThongKe> rooms = FXCollections.observableArrayList();
    private final ObservableList<SinhVien> members = FXCollections.observableArrayList();
    private final NumberFormat vnd = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
    private FilteredList<PhongThongKe> filteredRooms;

    @FXML private Label lblTotalRooms, lblOccupiedRooms, lblEmptyBeds;
    @FXML private Label lblRoomCount, lblRoomDetail, lblRoomDetailSub;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbStatus;
    @FXML private TableView<PhongThongKe> tableRooms;
    @FXML private TableColumn<PhongThongKe, String> colSoPhong, colLoaiPhong, colDonGia, colTrangThai;
    @FXML private TableColumn<PhongThongKe, Number> colSucChua, colDangO, colConTrong;
    @FXML private TableView<SinhVien> tableStudents;
    @FXML private TableColumn<SinhVien, String> colStudentNo, colStudentName,
            colStudentId, colStudentClass, colStudentMoveIn;

    @FXML
    private void initialize() {
        // Bảng PHONG + số chỗ được tính từ cơ sở dữ liệu.
        colSoPhong.setCellValueFactory(c -> text(c.getValue().getSoPhong()));
        colLoaiPhong.setCellValueFactory(c -> text(c.getValue().getLoaiPhong()));
        colDonGia.setCellValueFactory(c -> text(formatMoney(c.getValue().getDonGia())));
        colSucChua.setCellValueFactory(c -> number(c.getValue().getSoNguoiToiDa()));
        colDangO.setCellValueFactory(c -> number(c.getValue().getSoNguoiDangO()));
        colConTrong.setCellValueFactory(c -> number(c.getValue().getSoChoConTrong()));
        colTrangThai.setCellValueFactory(c -> text(c.getValue().getTrangThai()));
        colTrangThai.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("room-badge-full", "room-badge-open", "room-badge-empty");
                if (empty || status == null) {
                    setText(null);
                    return;
                }
                setText(status);
                getStyleClass().add(switch (status) {
                    case FULL -> "room-badge-full";
                    case AVAILABLE -> "room-badge-open";
                    default -> "room-badge-empty";
                });
            }
        });

        // Bảng SINH_VIEN thuộc phòng được chọn.
        colStudentNo.setCellValueFactory(c -> text(
                String.valueOf(members.indexOf(c.getValue()) + 1)));
        colStudentName.setCellValueFactory(c -> text(c.getValue().getHoTen()));
        colStudentId.setCellValueFactory(c -> text(c.getValue().getMaSV()));
        colStudentClass.setCellValueFactory(c -> text(c.getValue().getLop()));
        colStudentMoveIn.setCellValueFactory(c -> text(dateText(c.getValue().getNgayVaoO())));
        tableStudents.setItems(members);
        tableStudents.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableRooms.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        cbStatus.setItems(FXCollections.observableArrayList(ALL, EMPTY, AVAILABLE, FULL));
        cbStatus.setValue(ALL);
        filteredRooms = new FilteredList<>(rooms, r -> true);
        tableRooms.setItems(filteredRooms);

        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        cbStatus.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        filteredRooms.addListener((javafx.collections.ListChangeListener<PhongThongKe>) change -> {
            updateRoomCount();
            ensureSelection();
        });
        tableRooms.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldRoom, newRoom) -> loadMembers(newRoom));
        tableRooms.setRowFactory(tv -> {
            TableRow<PhongThongKe> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) handleRoomDetails();
            });
            return row;
        });

        loadFromDatabase(null);
    }

    private ReadOnlyStringWrapper text(String s) {
        return new ReadOnlyStringWrapper(s == null ? "" : s);
    }
    private ReadOnlyObjectWrapper<Number> number(int n) {
        return new ReadOnlyObjectWrapper<>(n);
    }
    private String formatMoney(BigDecimal amount) {
        return amount == null ? "—" : vnd.format(amount) + " đ";
    }
    private String dateText(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    private void applyFilter() {
        String search = txtSearch.getText() == null ? "" :
                txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        String status = cbStatus.getValue();
        filteredRooms.setPredicate(room -> {
            String haystack = (room.getSoPhong() + " " + room.getLoaiPhong())
                    .toLowerCase(Locale.ROOT);
            return (search.isEmpty() || haystack.contains(search))
                    && (status == null || status.equals(ALL) || status.equals(room.getTrangThai()));
        });
        ensureSelection();
        updateRoomCount();
    }

    private void ensureSelection() {
        PhongThongKe selected = tableRooms.getSelectionModel().getSelectedItem();
        if (selected == null || !filteredRooms.contains(selected)) {
            if (filteredRooms.isEmpty()) {
                tableRooms.getSelectionModel().clearSelection();
                loadMembers(null);
            } else {
                tableRooms.getSelectionModel().selectFirst();
            }
        }
    }

    private void updateSummary() {
        lblTotalRooms.setText(String.valueOf(rooms.size()));
        long occupied = rooms.stream().filter(r -> r.getSoNguoiDangO() > 0).count();
        int freeBeds = rooms.stream().mapToInt(PhongThongKe::getSoChoConTrong).sum();
        lblOccupiedRooms.setText(String.valueOf(occupied));
        lblEmptyBeds.setText(String.valueOf(freeBeds));
    }
    private void updateRoomCount() {
        lblRoomCount.setText("Hiển thị: " + filteredRooms.size() + " / " + rooms.size() + " phòng");
    }

    /** Tải danh sách phòng mới từ cơ sở dữ liệu, không sửa dữ liệu đang có nếu tải lỗi. */
    private boolean loadFromDatabase(String preferredRoomId) {
        try {
            List<PhongThongKe> fresh = phongDAO.findAll();
            rooms.setAll(fresh);
            updateSummary();
            updateRoomCount();
            if (preferredRoomId != null) selectRoom(preferredRoomId);
            ensureSelection();
            return true;
        } catch (SQLException ex) {
            showDatabaseError("Không thể tải danh sách phòng", ex);
            return false;
        }
    }

    private void selectRoom(String soPhong) {
        for (PhongThongKe room : filteredRooms) {
            if (room.getSoPhong().equals(soPhong)) {
                tableRooms.getSelectionModel().select(room);
                tableRooms.scrollTo(room);
                return;
            }
        }
    }

    private void loadMembers(PhongThongKe room) {
        if (room == null) {
            members.clear();
            lblRoomDetail.setText("Sinh viên trong phòng");
            lblRoomDetailSub.setText("Chọn phòng ở bảng trên");
            return;
        }
        try {
            List<SinhVien> fresh = phongDAO.findStudentsByRoom(room.getSoPhong());
            members.setAll(fresh);
            lblRoomDetail.setText("Sinh viên phòng " + room.getSoPhong());
            lblRoomDetailSub.setText("Hiện có " + fresh.size() + " sinh viên; sức chứa "
                    + room.getSoNguoiToiDa() + " người");
        } catch (SQLException ex) {
            // Không xóa bảng cũ để tránh gây nhầm lẫn khi DB mất kết nối.
            lblRoomDetailSub.setText("Không tải được dữ liệu mới – hãy nhấn Làm mới");
            showDatabaseError("Không thể tải sinh viên của phòng " + room.getSoPhong(), ex);
        }
    }

    @FXML
    private void handleRefresh() {
        txtSearch.clear();
        cbStatus.setValue(ALL);
        loadFromDatabase(null);
    }

    @FXML
    private void handleAdd() {
        showForm(null).ifPresent(phong -> {
            try {
                if (phongDAO.insert(phong)) {
                    txtSearch.clear();
                    cbStatus.setValue(ALL);
                    loadFromDatabase(phong.getSoPhong());
                    info("Thêm phòng " + phong.getSoPhong() + " thành công!");
                } else warn("Không thể thêm phòng.");
            } catch (SQLException ex) {
                showDatabaseError("Không thể thêm phòng", ex);
            }
        });
    }

    @FXML
    private void handleEdit() {
        PhongThongKe chosen = selectedRoom();
        if (chosen == null) return;
        showForm(chosen).ifPresent(phong -> {
            try {
                if (phongDAO.update(phong)) {
                    loadFromDatabase(phong.getSoPhong());
                    info("Cập nhật phòng " + phong.getSoPhong() + " thành công!");
                } else warn("Không tìm thấy phòng cần cập nhật.");
            } catch (SQLException ex) {
                showDatabaseError("Không thể sửa phòng", ex);
            }
        });
    }

    @FXML
    private void handleDelete() {
        PhongThongKe chosen = selectedRoom();
        if (chosen == null) return;
        if (chosen.getSoNguoiDangO() > 0) {
            warn("Phòng " + chosen.getSoPhong() + " đang có " + chosen.getSoNguoiDangO()
                 + " sinh viên. Hãy chuyển sinh viên sang phòng khác trước khi xóa.");
            return;
        }
        // Xác nhận xóa bằng Dialog tiếng Việt, giữ nguyên quy tắc không xóa phòng có SV.
        if (!confirmDelete(chosen)) return;
        try {
            if (phongDAO.delete(chosen.getSoPhong())) {
                loadFromDatabase(null);
                info("Xóa phòng thành công!");
            } else warn("Phòng không còn tồn tại trong cơ sở dữ liệu.");
        } catch (SQLException ex) {
            showDatabaseError("Không thể xóa phòng", ex);
        }
    }

    @FXML
    private void handleRoomDetails() {
        PhongThongKe chosen = selectedRoom();
        if (chosen == null) return;
        try {
            List<SinhVien> students = phongDAO.findStudentsByRoom(chosen.getSoPhong());
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Chi tiết phòng " + chosen.getSoPhong());
            dialog.setHeaderText(null);
            dialog.setResizable(true);
            attachOwner(dialog);

            ButtonType close = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
            DialogPane pane = dialog.getDialogPane();
            pane.getButtonTypes().add(close);
            pane.getStyleClass().add("room-dialog-pane");
            loadDialogCss(pane);

            Label mark = new Label("▦");
            mark.getStyleClass().add("room-form-mark");
            Label title = new Label("Thông tin phòng " + chosen.getSoPhong());
            title.getStyleClass().add("room-dialog-title");
            Label subtitle = new Label(chosen.getLoaiPhong());
            subtitle.getStyleClass().add("room-dialog-description");
            subtitle.setWrapText(true);
            HBox header = new HBox(14, mark, new VBox(5, title, subtitle));
            header.setAlignment(Pos.CENTER_LEFT);

            Label summary = new Label("Đơn giá: " + formatMoney(chosen.getDonGia())
                    + " / tháng    •    Sức chứa: " + chosen.getSoNguoiToiDa()
                    + "    •    Hiện có: " + students.size());
            summary.getStyleClass().add("room-detail-summary");
            summary.setMaxWidth(Double.MAX_VALUE);
            summary.setWrapText(true);

            Label section = new Label("DANH SÁCH SINH VIÊN TRONG PHÒNG");
            section.getStyleClass().add("room-dialog-section-label");
            TableView<SinhVien> detailTable = new TableView<>();
            detailTable.getStyleClass().add("room-table");
            detailTable.setItems(FXCollections.observableArrayList(students));
            detailTable.setPlaceholder(new Label("Phòng chưa có sinh viên"));
            detailTable.getColumns().addAll(
                    studentColumn("Mã sinh viên", SinhVien::getMaSV, 150),
                    studentColumn("Họ và tên", SinhVien::getHoTen, 190),
                    studentColumn("Lớp", SinhVien::getLop, 120),
                    studentColumn("Ngày vào ở", st -> dateText(st.getNgayVaoO()), 130)
            );
            detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            detailTable.setPrefHeight(295);

            VBox content = new VBox(17, header, summary, section, detailTable);
            content.getStyleClass().add("room-dialog-content");
            pane.setContent(content);
            pane.setPrefWidth(720);
            ((Button) pane.lookupButton(close)).getStyleClass().add("room-btn-secondary");
            dialog.showAndWait();
        } catch (SQLException ex) {
            showDatabaseError("Không thể xem chi tiết phòng", ex);
        }
    }

    private TableColumn<SinhVien, String> studentColumn(
            String title, java.util.function.Function<SinhVien, String> extractor, double width) {
        TableColumn<SinhVien, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(c -> text(extractor.apply(c.getValue())));
        return column;
    }

    private PhongThongKe selectedRoom() {
        PhongThongKe room = tableRooms.getSelectionModel().getSelectedItem();
        if (room == null) warn("Hãy chọn một phòng trong bảng trước.");
        return room;
    }

    /** Chỉ thay bố cục form; giữ nguyên 4 trường, kết quả trả về và hàm validateForm. */
    private Optional<Phong> showForm(PhongThongKe current) {
        boolean editing = current != null;
        Dialog<Phong> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Sửa phòng ở" : "Thêm phòng ở");
        dialog.setHeaderText(null);
        dialog.setResizable(true);
        attachOwner(dialog);

        ButtonType save = new ButtonType(editing ? "Lưu thay đổi" : "Thêm phòng",
                ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(save, cancel);
        pane.getStyleClass().add("room-dialog-pane");
        loadDialogCss(pane);

        TextField roomNumber = new TextField(editing ? current.getSoPhong() : "");
        TextField type = new TextField(editing ? current.getLoaiPhong() : "");
        TextField price = new TextField(editing ? current.getDonGia().toPlainString() : "");
        TextField capacity = new TextField(editing ? String.valueOf(current.getSoNguoiToiDa()) : "");
        roomNumber.setPromptText("Ví dụ: P101");
        type.setPromptText("Ví dụ: Phòng tiêu chuẩn 4 người");
        price.setPromptText("Ví dụ: 600000");
        capacity.setPromptText("Ví dụ: 4");
        // Mã phòng vẫn không thể sửa để bảo vệ các khóa ngoại.
        roomNumber.setEditable(!editing);
        if (editing) roomNumber.getStyleClass().add("room-field-readonly");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(19);
        // Khoảng lùi riêng của 4 ô nhập, tránh sát mép cửa sổ Dialog.
        grid.setPadding(new Insets(0, 14, 0, 14));
        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col2.setHgrow(Priority.ALWAYS);
        col1.setFillWidth(true);
        col2.setFillWidth(true);
        grid.getColumnConstraints().addAll(col1, col2);
        grid.add(formField("Số phòng", roomNumber), 0, 0);
        grid.add(formField("Loại phòng", type), 1, 0);
        grid.add(formField("Đơn giá (VNĐ)", price), 0, 1);
        grid.add(formField("Sức chứa tối đa", capacity), 1, 1);

        Label mark = new Label(editing ? "✎" : "+");
        mark.getStyleClass().add("room-form-mark");
        Label heading = new Label(editing ? "Cập nhật thông tin phòng" : "Thêm phòng ký túc xá");
        heading.getStyleClass().add("room-dialog-title");
        Label subtitle = new Label("Thông tin phòng được lưu trong cơ sở dữ liệu của hệ thống.");
        subtitle.getStyleClass().add("room-dialog-description");
        subtitle.setWrapText(true);
        HBox header = new HBox(15, mark, new VBox(5, heading, subtitle));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("room-dialog-header");

        Label hintIcon = new Label("i");
        hintIcon.getStyleClass().add("room-hint-icon");
        Label tip = new Label(editing
                ? "Số phòng được giữ nguyên. Sức chứa mới không được thấp hơn số sinh viên đang ở."
                : "Nhập đủ 4 trường thông tin, kiểm tra đơn giá và sức chứa trước khi lưu.");
        tip.setWrapText(true);
        tip.getStyleClass().add("room-dialog-banner-label");
        HBox banner = new HBox(10, hintIcon, tip);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().add("room-dialog-banner");
        HBox.setHgrow(tip, Priority.ALWAYS);

        Label section = new Label("THÔNG TIN PHÒNG Ở");
        section.getStyleClass().add("room-dialog-section-label");
        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("room-validation-error");
        error.setVisible(false);
        error.managedProperty().bind(error.visibleProperty());

        VBox content = new VBox(19, header, banner, section, grid, error);
        content.getStyleClass().add("room-dialog-content");
        pane.setContent(content);
        pane.setPrefWidth(680);

        Button saveButton = (Button) pane.lookupButton(save);
        saveButton.getStyleClass().add("room-btn-primary");
        ((Button) pane.lookupButton(cancel)).getStyleClass().add("room-btn-secondary");
        saveButton.addEventFilter(ActionEvent.ACTION, e -> {
            String message = validateForm(current, roomNumber.getText(), type.getText(),
                    price.getText(), capacity.getText());
            if (message != null) {
                error.setText(message);
                error.setVisible(true);
                e.consume();
            } else {
                error.setVisible(false);
            }
        });
        dialog.setResultConverter(button -> button == save
            ? new Phong(roomNumber.getText().trim(), type.getText().trim(),
                    new BigDecimal(price.getText().trim()), Integer.parseInt(capacity.getText().trim()))
            : null);
        return dialog.showAndWait();
    }

    private VBox formField(String label, TextField field) {
        Label caption = new Label(label);
        caption.getStyleClass().add("room-field-label");
        Label star = new Label("*");
        star.getStyleClass().add("room-field-required");
        HBox captionRow = new HBox(4, caption, star);
        captionRow.setAlignment(Pos.CENTER_LEFT);
        field.getStyleClass().add("room-field-input");
        field.setPrefWidth(260);
        field.setMaxWidth(Double.MAX_VALUE);
        VBox wrapper = new VBox(8, captionRow, field);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    private String validateForm(PhongThongKe current, String roomNumber, String type,
                                String priceText, String capacityText) {
        String id = roomNumber.trim();
        String roomType = type.trim();
        if (id.isEmpty() || roomType.isEmpty() || priceText.isBlank() || capacityText.isBlank())
            return "Vui lòng nhập đầy đủ 4 trường.";
        if (id.length() > 10 || roomType.length() > 50)
            return "Số phòng tối đa 10 ký tự; loại phòng tối đa 50 ký tự.";
        int capacity;
        BigDecimal price;
        try {
            price = new BigDecimal(priceText.trim());
            capacity = Integer.parseInt(capacityText.trim());
        } catch (NumberFormatException ex) {
            return "Đơn giá và sức chứa phải là số. Ví dụ: 600000 và 4.";
        }
        if (price.signum() <= 0 || price.scale() > 2 || price.precision() - price.scale() > 10)
            return "Đơn giá phải > 0, tối đa 10 chữ số nguyên và 2 chữ số thập phân.";
        if (capacity <= 0) return "Sức chứa phải lớn hơn 0.";
        if (current != null && capacity < current.getSoNguoiDangO())
            return "Phòng đang có " + current.getSoNguoiDangO()
                    + " sinh viên, không thể giảm sức chứa xuống " + capacity + ".";
        return null;
    }

    // Thống nhất các thông báo của Phòng ở với phong cách Dialog module Sinh viên.
    private void warn(String msg) {
        showNotice("warning", "Cần kiểm tra thông tin", msg);
    }

    private void info(String msg) {
        showNotice("success", "Thao tác thành công", msg);
    }

    private void showNotice(String kind, String title, String msg) {
        ButtonType ok = new ButtonType("Đã hiểu", ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = createMessageDialog(kind, title, msg, ok);
        ((Button) dialog.getDialogPane().lookupButton(ok)).getStyleClass().add("room-message-primary");
        dialog.showAndWait();
    }

    private boolean confirmDelete(PhongThongKe room) {
        ButtonType delete = new ButtonType("Xóa phòng", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        String msg = "Bạn có chắc chắn muốn xóa phòng " + room.getSoPhong() + " ("
                + room.getLoaiPhong() + ")?\n"
                + "Hành động này không thể hoàn tác.";
        Dialog<ButtonType> dialog = createMessageDialog(
                "confirm", "Xác nhận xóa phòng", msg, delete, cancel);
        ((Button) dialog.getDialogPane().lookupButton(delete)).getStyleClass().add("room-message-danger-button");
        ((Button) dialog.getDialogPane().lookupButton(cancel)).getStyleClass().add("room-message-secondary");
        return dialog.showAndWait().filter(delete::equals).isPresent();
    }

    private Dialog<ButtonType> createMessageDialog(String kind, String title, String msg,
                                                    ButtonType... buttons) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("PTIT KTX - Quản lý phòng ở");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.setResizable(false);
        attachOwner(dialog);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(buttons);
        pane.getStyleClass().addAll("room-message-pane", "room-message-" + kind);
        loadDialogCss(pane);

        String symbol = switch (kind) {
            case "success" -> "✓";
            case "warning" -> "!";
            case "error" -> "×";
            default -> "?";
        };
        Label icon = new Label(symbol);
        icon.getStyleClass().add("room-message-icon");
        Label heading = new Label(title);
        heading.getStyleClass().add("room-message-title");
        heading.setWrapText(true);
        HBox header = new HBox(14, icon, heading);
        header.setAlignment(Pos.CENTER_LEFT);

        Region rule = new Region();
        rule.getStyleClass().add("room-message-rule");
        rule.setMinHeight(1);
        Label body = new Label(msg);
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);
        body.getStyleClass().add("room-message-body");
        VBox content = new VBox(17, header, rule, body);
        content.getStyleClass().add("room-message-content");
        pane.setContent(content);
        pane.setPrefWidth(480);
        dialog.setResultConverter(button -> button);
        return dialog;
    }

    private void attachOwner(Dialog<?> dialog) {
        if (tableRooms != null && tableRooms.getScene() != null
                && tableRooms.getScene().getWindow() != null) {
            dialog.initOwner(tableRooms.getScene().getWindow());
        }
    }

    private void loadDialogCss(DialogPane pane) {
        java.net.URL css = getClass().getResource("/com/ptit/quanlyktx/css/phong.css");
        if (css != null) pane.getStylesheets().add(css.toExternalForm());
    }

    /** Giữ lỗi kỹ thuật ở console; chỉ hiển thị nội dung dễ hiểu cho người dùng. */
    private void showDatabaseError(String action, SQLException ex) {
        String raw = ex.getMessage() == null ? "" : ex.getMessage();
        String reason = raw.toLowerCase(Locale.ROOT);
        String detail;
        if (reason.contains("không thể giảm sức chứa") || reason.contains("sức chứa")) {
            detail = "Không thể giảm sức chứa: số sinh viên đang ở vượt quá giới hạn mới.";
        } else if (reason.contains("unique constraint") || reason.contains("primary key") || reason.contains("primarykey")
                || reason.contains("phong.so_phong") || reason.contains("duplicate entry")) {
            detail = "Số phòng đã tồn tại. Vui lòng nhập một số phòng khác.";
        } else if (reason.contains("foreign key") || reason.contains("foreignkey")
                || reason.contains("khóa ngoại")) {
            detail = action.toLowerCase(Locale.ROOT).contains("xóa")
                    ? "Phòng vẫn còn dữ liệu liên quan, nên không thể xóa."
                    : "Dữ liệu liên kết chưa hợp lệ. Vui lòng kiểm tra lại.";
        } else if (reason.contains("database is locked") || reason.contains("sqlite_busy")
                || reason.contains("sqlite_locked")) {
            detail = "Cơ sở dữ liệu đang bận. Vui lòng thử lại sau.";
        } else if (reason.contains("check constraint") || reason.contains("sqlite_constraint_check")
                || reason.contains("not null constraint") || reason.contains("sqlite_constraint_notnull")) {
            detail = "Đơn giá, sức chứa hoặc thông tin phòng chưa hợp lệ.";
        } else {
            detail = "Không thể hoàn thành thao tác với cơ sở dữ liệu. "
                    + "Vui lòng kiểm tra thông tin hoặc thử lại.";
        }
        System.err.println("[Phong - Database] " + action + ": " + raw);
        showNotice("error", action, detail);
    }
}
