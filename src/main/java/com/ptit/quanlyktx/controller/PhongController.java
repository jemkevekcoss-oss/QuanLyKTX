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
import javafx.scene.Node;
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

/** Điều khiển PhongView.fxml – dữ liệu đến từ MySQL, không hard-code. */
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
        // Bảng PHONG + số chỗ được tính từ MySQL.
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

    /** Tải danh sách phòng mới từ MySQL, không sửa dữ liệu đang có nếu tải lỗi. */
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn muốn xóa phòng " + chosen.getSoPhong() + " khỏi MySQL?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận xóa phòng");
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            if (phongDAO.delete(chosen.getSoPhong())) {
                loadFromDatabase(null);
                info("Xóa phòng thành công!");
            } else warn("Phòng không còn tồn tại trong MySQL.");
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
            dialog.setHeaderText("Phòng " + chosen.getSoPhong() + " · " + chosen.getLoaiPhong());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            Label info = new Label("Đơn giá: " + formatMoney(chosen.getDonGia())
                    + " / tháng   |   Sức chứa: " + chosen.getSoNguoiToiDa()
                    + "   |   Hiện có: " + students.size());
            TableView<SinhVien> detailTable = new TableView<>();
            detailTable.setItems(FXCollections.observableArrayList(students));
            detailTable.getColumns().addAll(
                    studentColumn("Mã sinh viên", SinhVien::getMaSV, 150),
                    studentColumn("Họ và tên", SinhVien::getHoTen, 190),
                    studentColumn("Lớp", SinhVien::getLop, 120),
                    studentColumn("Ngày vào ở", s -> dateText(s.getNgayVaoO()), 130)
            );
            detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            detailTable.setPrefHeight(300);
            VBox content = new VBox(12, info, detailTable);
            content.setPadding(new Insets(8));
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefWidth(675);
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

    private Optional<Phong> showForm(PhongThongKe current) {
        boolean editing = current != null;
        Dialog<Phong> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Sửa phòng ở" : "Thêm phòng ở");
        dialog.setHeaderText("Nhập 4 thuộc tính của bảng PHONG");
        ButtonType save = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField roomNumber = new TextField(editing ? current.getSoPhong() : "");
        TextField type = new TextField(editing ? current.getLoaiPhong() : "");
        TextField price = new TextField(editing ? current.getDonGia().toPlainString() : "");
        TextField capacity = new TextField(editing ? String.valueOf(current.getSoNguoiToiDa()) : "");
        roomNumber.setPromptText("Ví dụ: P101");
        type.setPromptText("Ví dụ: Tiêu chuẩn 4 người");
        price.setPromptText("Ví dụ: 600000 (VNĐ/tháng)");
        capacity.setPromptText("Ví dụ: 4");
        roomNumber.setDisable(editing); // Không đổi khóa chính khi sửa.

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Số phòng"), roomNumber);
        grid.addRow(1, new Label("Loại phòng"), type);
        grid.addRow(2, new Label("Đơn giá (VNĐ)"), price);
        grid.addRow(3, new Label("Sức chứa tối đa"), capacity);
        Label error = new Label();
        error.setStyle("-fx-text-fill: #B91C1C; -fx-wrap-text: true;");
        error.setWrapText(true);
        grid.add(error, 0, 4, 2, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(470);

        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, e -> {
            String message = validateForm(current, roomNumber.getText(), type.getText(),
                    price.getText(), capacity.getText());
            if (message != null) {
                error.setText(message);
                e.consume();
            }
        });
        dialog.setResultConverter(button -> button == save
            ? new Phong(roomNumber.getText().trim(), type.getText().trim(),
                    new BigDecimal(price.getText().trim()), Integer.parseInt(capacity.getText().trim()))
            : null);
        return dialog.showAndWait();
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

    private void warn(String msg) { message(Alert.AlertType.WARNING, "Quản lý phòng ở", msg); }
    private void info(String msg) { message(Alert.AlertType.INFORMATION, "Quản lý phòng ở", msg); }
    private void message(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
    private void showDatabaseError(String action, SQLException ex) {
        String detail = switch (ex.getErrorCode()) {
            case 1062 -> "Số phòng đã tồn tại.";
            case 1451 -> "Phòng còn được bảng khác tham chiếu, không thể xóa.";
            case 1452 -> "Dữ liệu tham chiếu không hợp lệ.";
            default -> ex.getMessage();
        };
        message(Alert.AlertType.ERROR, "Lỗi MySQL", action + ".\n" + detail);
    }
}
