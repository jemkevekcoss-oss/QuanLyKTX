package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.dao.KhachThamDAO;
import com.ptit.quanlyktx.dao.KhachThamDAO.SinhVienOption;
import com.ptit.quanlyktx.dto.KhachThamRow;
import com.ptit.quanlyktx.model.KhachTham;
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

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Màn Khách thăm: mỗi bản ghi KHACH_THAM là một LƯỢT đến thăm. */
public class KhachThamController {

    private static final String ALL = "Tất cả thời gian";
    private static final String TODAY = "Hôm nay";
    private static final String WEEK = "Tuần này";
    private static final String MONTH = "Tháng này";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final KhachThamDAO khachThamDAO = new KhachThamDAO();
    private final ObservableList<KhachThamRow> visits = FXCollections.observableArrayList();
    private FilteredList<KhachThamRow> filteredVisits;

    @FXML private Label lblTotalVisits, lblMonthVisits, lblUniqueGuests;
    @FXML private Label lblCount, lblSelected;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbPeriod;
    @FXML private TableView<KhachThamRow> tableVisits;
    @FXML private TableColumn<KhachThamRow, String> colId, colGuestName, colGuestId,
            colBirth, colVisitDate, colStudentId, colStudentName, colRoom;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(c -> text(String.valueOf(c.getValue().getKhach().getIdKhach())));
        colGuestName.setCellValueFactory(c -> text(c.getValue().getKhach().getHoTen()));
        colGuestId.setCellValueFactory(c -> text(c.getValue().getKhach().getSoCmt()));
        colBirth.setCellValueFactory(c -> text(dateText(c.getValue().getKhach().getNgaySinh())));
        colVisitDate.setCellValueFactory(c -> text(dateText(c.getValue().getKhach().getNgayDenChoi())));
        colStudentId.setCellValueFactory(c -> text(c.getValue().getKhach().getMaSV()));
        colStudentName.setCellValueFactory(c -> text(c.getValue().getTenSinhVien()));
        colRoom.setCellValueFactory(c -> text(c.getValue().getSoPhong()));

        tableVisits.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cbPeriod.setItems(FXCollections.observableArrayList(ALL, TODAY, WEEK, MONTH));
        cbPeriod.setValue(ALL);
        filteredVisits = new FilteredList<>(visits, v -> true);
        tableVisits.setItems(filteredVisits);

        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        cbPeriod.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        filteredVisits.addListener((javafx.collections.ListChangeListener<KhachThamRow>) change -> updateCount());
        tableVisits.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldRow, newRow) -> showSelection(newRow));

        loadFromDatabase(-1);
    }

    private ReadOnlyStringWrapper text(String value) {
        return new ReadOnlyStringWrapper(value == null ? "" : value);
    }

    private String dateText(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    private String safe(String value) { return value == null ? "" : value; }

    private void applyFilter() {
        String search = txtSearch.getText() == null ? "" :
                txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        String period = cbPeriod.getValue();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        filteredVisits.setPredicate(row -> {
            KhachTham k = row.getKhach();
            String haystack = (k.getIdKhach() + " " + safe(k.getSoCmt()) + " "
                    + safe(k.getHoTen()) + " " + safe(k.getMaSV()) + " "
                    + safe(row.getTenSinhVien()) + " " + safe(row.getSoPhong()))
                    .toLowerCase(Locale.ROOT);
            if (!search.isEmpty() && !haystack.contains(search)) return false;

            LocalDate date = k.getNgayDenChoi();
            if (period == null || ALL.equals(period)) return true;
            if (date == null) return false;
            return switch (period) {
                case TODAY -> date.isEqual(today);
                case WEEK -> !date.isBefore(monday) && !date.isAfter(today);
                case MONTH -> date.getMonthValue() == today.getMonthValue()
                        && date.getYear() == today.getYear();
                default -> true;
            };
        });
        updateCount();
    }

    private void updateSummary() {
        LocalDate today = LocalDate.now();
        List<KhachThamRow> monthVisits = visits.stream()
                .filter(row -> {
                    LocalDate d = row.getKhach().getNgayDenChoi();
                    return d != null && d.getYear() == today.getYear()
                            && d.getMonthValue() == today.getMonthValue();
                }).toList();
        Set<String> uniqueIds = monthVisits.stream()
                .map(row -> row.getKhach().getSoCmt())
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());

        lblTotalVisits.setText(String.valueOf(visits.size()));
        lblMonthVisits.setText(String.valueOf(monthVisits.size()));
        lblUniqueGuests.setText(String.valueOf(uniqueIds.size()));
    }

    private void updateCount() {
        lblCount.setText("Hiển thị: " + filteredVisits.size() + " / " + visits.size() + " lượt thăm");
    }

    private void showSelection(KhachThamRow row) {
        if (row == null) {
            lblSelected.setText("Chọn một lượt khách trong bảng để xem tóm tắt.");
            return;
        }
        KhachTham k = row.getKhach();
        lblSelected.setText("Lượt #" + k.getIdKhach() + " · " + k.getHoTen()
                + " (" + k.getSoCmt() + ") đến ngày " + dateText(k.getNgayDenChoi())
                + " · Thăm " + k.getMaSV() + " - " + safe(row.getTenSinhVien())
                + " · Phòng " + safe(row.getSoPhong()));
    }

    private boolean loadFromDatabase(int preferredId) {
        try {
            List<KhachThamRow> fresh = khachThamDAO.findAll();
            visits.setAll(fresh);
            updateSummary();
            applyFilter();
            KhachThamRow selected = filteredVisits.stream()
                    .filter(row -> row.getKhach().getIdKhach() == preferredId)
                    .findFirst().orElse(null);
            if (selected != null) {
                tableVisits.getSelectionModel().select(selected);
                tableVisits.scrollTo(selected);
            } else if (!filteredVisits.isEmpty()) {
                tableVisits.getSelectionModel().selectFirst();
            } else {
                tableVisits.getSelectionModel().clearSelection();
                showSelection(null);
            }
            return true;
        } catch (SQLException ex) {
            showDatabaseError("Không thể tải danh sách khách thăm", ex);
            return false;
        }
    }

    @FXML
    private void handleRefresh() {
        txtSearch.clear();
        cbPeriod.setValue(ALL);
        loadFromDatabase(-1);
    }

    @FXML
    private void handleAdd() {
        showForm(null).ifPresent(guest -> {
            try {
                if (khachThamDAO.insert(guest)) {
                    txtSearch.clear();
                    cbPeriod.setValue(ALL);
                    loadFromDatabase(-1);
                    info("Đã lưu lượt khách đến thăm vào MySQL.");
                } else warn("Không thể lưu lượt khách.");
            } catch (SQLException ex) {
                showDatabaseError("Không thể ghi nhận khách đến", ex);
            }
        });
    }

    @FXML
    private void handleEdit() {
        KhachThamRow selected = selectedRow();
        if (selected == null) return;
        showForm(selected.getKhach()).ifPresent(guest -> {
            try {
                if (khachThamDAO.update(guest)) {
                    loadFromDatabase(guest.getIdKhach());
                    info("Cập nhật lượt thăm thành công!");
                } else warn("Lượt thăm không còn trong MySQL.");
            } catch (SQLException ex) {
                showDatabaseError("Không thể sửa lượt khách", ex);
            }
        });
    }

    @FXML
    private void handleDelete() {
        KhachThamRow selected = selectedRow();
        if (selected == null) return;
        int id = selected.getKhach().getIdKhach();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xóa lượt #" + id + " của khách " + selected.getKhach().getHoTen()
                + " khỏi MySQL?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận xóa lượt thăm");
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            if (khachThamDAO.delete(id)) {
                loadFromDatabase(-1);
                info("Xóa lượt thăm thành công!");
            } else warn("Không tìm thấy lượt thăm cần xóa.");
        } catch (SQLException ex) {
            showDatabaseError("Không thể xóa lượt khách", ex);
        }
    }

    private KhachThamRow selectedRow() {
        KhachThamRow row = tableVisits.getSelectionModel().getSelectedItem();
        if (row == null) warn("Hãy chọn một lượt khách trong bảng trước.");
        return row;
    }

    private Optional<KhachTham> showForm(KhachTham current) {
        List<SinhVienOption> options;
        try {
            options = khachThamDAO.findStudents();
        } catch (SQLException ex) {
            showDatabaseError("Không thể tải danh sách sinh viên", ex);
            return Optional.empty();
        }
        if (options.isEmpty()) {
            warn("Chưa có sinh viên nào trong CSDL. Hãy thêm sinh viên trước.");
            return Optional.empty();
        }

        boolean editing = current != null;
        Dialog<KhachTham> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Sửa lượt khách đến thăm" : "Ghi nhận khách đến thăm");
        dialog.setHeaderText("Mỗi bản ghi là một lượt đến thăm sinh viên");
        ButtonType save = new ButtonType("Lưu vào MySQL", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField identity = new TextField(editing ? safe(current.getSoCmt()) : "");
        identity.setPromptText("CMT/CCCD của khách (không bắt buộc duy nhất)");
        TextField name = new TextField(editing ? safe(current.getHoTen()) : "");
        name.setPromptText("Họ và tên khách");
        DatePicker birthday = new DatePicker(editing ? current.getNgaySinh() : null);
        DatePicker visitDate = new DatePicker(editing ? current.getNgayDenChoi() : LocalDate.now());
        ComboBox<SinhVienOption> student = new ComboBox<>(FXCollections.observableArrayList(options));
        student.setMaxWidth(Double.MAX_VALUE);
        student.setPromptText("Chọn sinh viên được thăm");
        if (editing) {
            options.stream().filter(s -> s.maSV().equals(current.getMaSV()))
                    .findFirst().ifPresent(student::setValue);
        }
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(11);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("Số CMT/CCCD *"), identity);
        grid.addRow(1, new Label("Họ tên khách *"), name);
        grid.addRow(2, new Label("Ngày sinh (tùy chọn)"), birthday);
        grid.addRow(3, new Label("Ngày đến chơi *"), visitDate);
        grid.addRow(4, new Label("Sinh viên được thăm *"), student);
        Label error = new Label();
        error.setWrapText(true);
        error.setStyle("-fx-text-fill: #B91C1C;");
        grid.add(error, 0, 5, 2, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(560);

        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String problem = validate(identity.getText(), name.getText(),
                    birthday.getValue(), visitDate.getValue(), student.getValue());
            if (problem != null) {
                error.setText(problem);
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == save
                ? new KhachTham(editing ? current.getIdKhach() : 0,
                        identity.getText().trim(), name.getText().trim(),
                        birthday.getValue(), visitDate.getValue(), student.getValue().maSV())
                : null);
        return dialog.showAndWait();
    }

    private String validate(String cmt, String name, LocalDate birthday,
                            LocalDate visitDate, SinhVienOption student) {
        if (cmt == null || cmt.isBlank() || name == null || name.isBlank()
                || visitDate == null || student == null)
            return "Vui lòng điền đầy đủ các trường có dấu *.";
        if (cmt.trim().length() > 20 || name.trim().length() > 100)
            return "CMT/CCCD tối đa 20 ký tự; họ tên tối đa 100 ký tự.";
        if (visitDate.isAfter(LocalDate.now()))
            return "Ngày đến chơi không được nằm trong tương lai.";
        if (birthday != null && birthday.isAfter(visitDate))
            return "Ngày sinh không thể sau ngày đến chơi.";
        return null;
    }

    private void warn(String msg) { message(Alert.AlertType.WARNING, "Quản lý khách thăm", msg); }
    private void info(String msg) { message(Alert.AlertType.INFORMATION, "Quản lý khách thăm", msg); }
    private void message(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showDatabaseError(String action, SQLException ex) {
        String detail = switch (ex.getErrorCode()) {
            case 1062 -> "Bản ghi bị trùng khóa hoặc ràng buộc UNIQUE.";
            case 1452 -> "Mã sinh viên được thăm không tồn tại.";
            case 1451 -> "Không thể xóa vì có dữ liệu đang tham chiếu.";
            default -> "MySQL: " + ex.getMessage();
        };
        message(Alert.AlertType.ERROR, "Lỗi MySQL", action + ".\n" + detail);
    }
}
