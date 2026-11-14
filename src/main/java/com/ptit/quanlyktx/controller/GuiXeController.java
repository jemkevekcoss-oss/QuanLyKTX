package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.dao.LuotGuiXeDAO;
import com.ptit.quanlyktx.dao.VeXeThangDAO;
import com.ptit.quanlyktx.dao.VeXeThangDAO.SinhVienRef;
import com.ptit.quanlyktx.dto.LuotGuiXeRow;
import com.ptit.quanlyktx.dto.VeXeThangRow;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Ba tab: vé tháng, trạm gửi/lấy xe, lịch sử. Không có "Hoạt động gần đây". */
public class GuiXeController {
    private final VeXeThangDAO ticketDAO = new VeXeThangDAO();
    private final LuotGuiXeDAO tripDAO = new LuotGuiXeDAO();

    private final ObservableList<VeXeThangRow> tickets = FXCollections.observableArrayList();
    private final ObservableList<LuotGuiXeRow> openTrips = FXCollections.observableArrayList();
    private final ObservableList<LuotGuiXeRow> trips = FXCollections.observableArrayList();
    private FilteredList<VeXeThangRow> filteredTickets;
    private FilteredList<LuotGuiXeRow> filteredTrips;

    private final DateTimeFormatter clock = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final NumberFormat currency = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
    private VeXeThangRow lookedUpTicket;
    private YearMonth viewedMonth = YearMonth.now();

    @FXML private TabPane tabPane;
    @FXML private TextField txtMonth;
    @FXML private Label lblTickets, lblStudents, lblRevenue, lblTicketCount;
    @FXML private TextField txtTicketSearch;
    @FXML private TableView<VeXeThangRow> tableTickets;
    @FXML private TableColumn<VeXeThangRow, String> colTicketId, colTicketPlate, colTicketType,
            colTicketStudent, colTicketMonth, colTicketFee, colTicketStatus;

    @FXML private TextField txtPlate;
    @FXML private Label lblPlate, lblOwner, lblOwnerInfo, lblValidity, lblVehicleType,
            lblVehicleId, lblFreeCount, lblGateHint, lblParked, lblTodayFees;
    @FXML private Button btnCheckIn, btnCheckOut;
    @FXML private VBox boxVehicle;
    @FXML private TableView<LuotGuiXeRow> tableOpen;
    @FXML private TableColumn<LuotGuiXeRow, String> colOpenPlate, colOpenTime, colOpenStudent;
    @FXML private TableColumn<LuotGuiXeRow, Void> colOpenAction;

    @FXML private DatePicker dpFrom, dpTo;
    @FXML private TextField txtHistorySearch;
    @FXML private ComboBox<String> cbHistoryStatus;
    @FXML private TableView<LuotGuiXeRow> tableHistory;
    @FXML private TableColumn<LuotGuiXeRow, String> colHistoryId, colHistoryPlate,
            colHistoryStudent, colHistoryIn, colHistoryOut, colHistoryFee, colHistoryState;
    @FXML private Label lblHistoryCount, lblHistoryFees;

    private ReadOnlyStringWrapper str(String s) { return new ReadOnlyStringWrapper(s == null ? "" : s); }
    private String money(BigDecimal n) { return currency.format(n == null ? BigDecimal.ZERO : n) + " đ"; }
    private String date(LocalDateTime t) { return t == null ? "—" : t.format(clock); }
    // Vé tháng có hiệu lực từ ngày đăng ký (hoặc ngày đầu kỳ nếu gia hạn trước).
    // Các vé cũ không có ngayDangKy vẫn áp dụng quy tắc theo tháng.
    private String status(VeXeThangRow r) {
        try {
            YearMonth kyVe = YearMonth.parse(r.thangDangKy());
            LocalDate homNay = LocalDate.now();
            YearMonth thangHienTai = YearMonth.from(homNay);
            if (kyVe.isBefore(thangHienTai)) return "Hết hạn";
            if (kyVe.isAfter(thangHienTai)
                    || (r.ngayDangKy() != null && homNay.isBefore(r.ngayDangKy()))) {
                return "Chưa hiệu lực";
            }
            return "✓ Còn hiệu lực";
        } catch (Exception ex) {
            return "Tháng không hợp lệ";
        }
    }

    @FXML private void initialize() {
        // 1. Vé tháng: dữ liệu JOIN từ SQLite, không phải dữ liệu mẫu.
        colTicketId.setCellValueFactory(c -> str(c.getValue().maVeXe()));
        colTicketPlate.setCellValueFactory(c -> str(c.getValue().bienSoXe()));
        colTicketType.setCellValueFactory(c -> str(c.getValue().loaiXe()));
        colTicketStudent.setCellValueFactory(c -> str(
                c.getValue().hoTen() + " (" + c.getValue().maSV() + ")"));
        colTicketMonth.setCellValueFactory(c -> str(c.getValue().thangDangKy()));
        colTicketFee.setCellValueFactory(c -> str(money(c.getValue().phiHangThang())));
        colTicketStatus.setCellValueFactory(c -> str(status(c.getValue())));
        colTicketStatus.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : s);
                getStyleClass().removeAll("parking-status-valid", "parking-status-expired", "parking-status-neutral");
                if (!empty && s != null) getStyleClass().add(s.startsWith("✓") ?
                        "parking-status-valid" : (s.equals("Hết hạn") ?
                        "parking-status-expired" : "parking-status-neutral"));
            }
        });
        tableTickets.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        filteredTickets = new FilteredList<>(tickets, t -> true);
        tableTickets.setItems(filteredTickets);
        txtTicketSearch.textProperty().addListener((obs, old, now) -> applyTicketFilter());
        tableTickets.setRowFactory(tv -> {
            TableRow<VeXeThangRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) showTicketDetails(row.getItem());
            });
            return row;
        });

        // 2. Xe đang trong bãi.
        colOpenPlate.setCellValueFactory(c -> str(c.getValue().bienSoXe()));
        colOpenTime.setCellValueFactory(c -> str(date(c.getValue().thoiGianGui())));
        colOpenStudent.setCellValueFactory(c -> str(c.getValue().hoTen()));
        colOpenAction.setCellFactory(col -> new TableCell<>() {
            private final Button take = new Button("→ Xác nhận lấy");
            {
                take.getStyleClass().add("parking-outline-success");
                take.setOnAction(e -> {
                    LuotGuiXeRow row = getTableView().getItems().get(getIndex());
                    confirmCheckout(row);
                });
            }
            @Override protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(empty ? null : take);
            }
        });
        tableOpen.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableOpen.setItems(openTrips);
        btnCheckIn.setDisable(true);
        btnCheckOut.setDisable(true);

        // 3. Lịch sử: dữ liệu từ SQLite, lọc tại giao diện.
        colHistoryId.setCellValueFactory(c -> str(String.valueOf(c.getValue().idLuot())));
        colHistoryPlate.setCellValueFactory(c -> str(c.getValue().bienSoXe()));
        colHistoryStudent.setCellValueFactory(c -> str(c.getValue().hoTen() + " (" + c.getValue().maSV() + ")"));
        colHistoryIn.setCellValueFactory(c -> str(date(c.getValue().thoiGianGui())));
        colHistoryOut.setCellValueFactory(c -> str(date(c.getValue().thoiGianLay())));
        colHistoryFee.setCellValueFactory(c -> str(money(c.getValue().tienPhatSinh())));
        // Làm nổi bật các lượt phát sinh phí; không thay đổi số tiền lưu trong SQLite.
        colHistoryFee.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                getStyleClass().remove("parking-fee-positive");
                if (!empty && getTableRow() != null && getTableRow().getItem() != null) {
                    BigDecimal fee = getTableRow().getItem().tienPhatSinh();
                    if (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) {
                        getStyleClass().add("parking-fee-positive");
                    }
                }
            }
        });
        colHistoryState.setCellValueFactory(c -> str(c.getValue().dangGui() ? "● Đang gửi" : "✓ Đã lấy"));
        tableHistory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        filteredTrips = new FilteredList<>(trips, t -> true);
        tableHistory.setItems(filteredTrips);
        txtHistorySearch.textProperty().addListener((obs, old, now) -> applyHistoryFilter());
        cbHistoryStatus.setItems(FXCollections.observableArrayList(
                "Tất cả", "Đang gửi", "Đã lấy", "Có phí phát sinh"));
        cbHistoryStatus.setValue("Tất cả");
        cbHistoryStatus.valueProperty().addListener((obs, old, now) -> applyHistoryFilter());
        dpFrom.setValue(LocalDate.now().minusMonths(1).withDayOfMonth(1));
        dpTo.setValue(LocalDate.now());

        // Nhập tháng bất kỳ theo YYYY-MM; chỉ truy vấn khi nhấn Xem hoặc Enter.
        txtMonth.setText(viewedMonth.toString());
        txtMonth.setTooltip(new Tooltip("Nhập tháng bất kỳ theo YYYY-MM, ví dụ 2024-03"));
        loadTickets();
        loadGate();
        loadHistory();
    }

    // -------- TAB 1: VE THANG --------
    private void loadTickets() {
        try {
            tickets.setAll(ticketDAO.findByMonth(viewedMonth));
            long students = tickets.stream().map(VeXeThangRow::maSV).distinct().count();
            BigDecimal revenue = tickets.stream().map(VeXeThangRow::phiHangThang)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            lblTickets.setText(String.valueOf(tickets.size()));
            lblStudents.setText(String.valueOf(students));
            lblRevenue.setText(money(revenue));
            applyTicketFilter();
        } catch (SQLException ex) { error("Không tải được danh sách vé xe tháng", ex); }
    }

    private void applyTicketFilter() {
        if (filteredTickets == null) return;
        String s = txtTicketSearch.getText() == null ? "" : txtTicketSearch.getText().strip().toLowerCase(Locale.ROOT);
        filteredTickets.setPredicate(t -> (t.maVeXe() + " " + t.bienSoXe() + " " + t.loaiXe()
                + " " + t.maSV() + " " + t.hoTen()).toLowerCase(Locale.ROOT).contains(s));
        lblTicketCount.setText("Hiển thị: " + filteredTickets.size() + " / " + tickets.size() + " vé");
    }

    @FXML private void handleRefreshTickets() { txtTicketSearch.clear(); loadTickets(); }

    @FXML private void handleAddTicket() {
        // Vẫn được tra cứu tháng quá khứ, nhưng không tạo vé "mua lùi ngày".
        if (viewedMonth.isBefore(YearMonth.now())) {
            warn("Không thể đăng ký vé cho tháng đã qua (" + viewedMonth + ").\n"
                    + "Hãy chọn tháng hiện tại hoặc một tháng tương lai.");
            return;
        }
        editTicketForm(null).ifPresent(f -> {
            try {
                String code = ticketDAO.register(f.plate, f.type, f.owner, viewedMonth);
                info("Đăng ký vé thành công", "Mã vé mới: " + code);
                loadTickets();
            } catch (SQLException ex) { error("Không đăng ký được vé", ex); }
        });
    }

    @FXML private void handleEditTicket() {
        VeXeThangRow selected = selectedTicket();
        if (selected == null) return;
        editTicketForm(selected).ifPresent(f -> {
            try {
                ticketDAO.edit(selected.maVeXe(), f.plate, f.type, f.owner,
                        YearMonth.parse(selected.thangDangKy()));
                loadTickets();
                info("Thành công", "Đã cập nhật thông tin vé.");
            } catch (SQLException ex) { error("Không sửa được vé", ex); }
        });
    }

    @FXML private void handleDeleteTicket() {
        VeXeThangRow selected = selectedTicket();
        if (selected == null) return;
        if (!confirm("Xóa vé " + selected.maVeXe() + "?", "Thao tác này chỉ thành công nếu vé chưa có lượt gửi xe.")) return;
        try {
            ticketDAO.delete(selected.maVeXe());
            loadTickets();
        } catch (SQLException ex) { error("Không thể xóa vé", ex); }
    }

    @FXML private void handleTicketDetail() {
        VeXeThangRow selected = selectedTicket();
        if (selected != null) showTicketDetails(selected);
    }

    @FXML private void handleRenewTicket() {
        VeXeThangRow selected = selectedTicket();
        if (selected == null) return;
        renewTicket(selected);
    }

    // Hộp thoại gia hạn tùy chỉnh – giữ nguyên nghiệp vụ ticketDAO.renew(...).
    private void renewTicket(VeXeThangRow selected) {
        YearMonth oldMonth = YearMonth.parse(selected.thangDangKy());
        YearMonth next = oldMonth.plusMonths(1);

        Dialog<YearMonth> dialog = new Dialog<>();
        dialog.setTitle("Gia hạn vé tháng");
        dialog.setHeaderText(null);
        if (tabPane.getScene() != null) dialog.initOwner(tabPane.getScene().getWindow());

        ButtonType approve = new ButtonType("↻  Xác nhận gia hạn", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(approve, cancel);
        applyTicketFormStyle(pane);

        Label title = new Label("Gia hạn vé xe tháng");
        title.getStyleClass().add("parking-form-heading");
        Label subtitle = new Label("Đăng ký sang tháng mới, giữ nguyên vé cũ và lịch sử gửi xe");
        subtitle.getStyleClass().add("parking-form-subheading");

        Label mini = new Label("BIỂN KIỂM SOÁT");
        mini.getStyleClass().add("parking-form-card-caption");
        Label plate = new Label(selected.bienSoXe());
        plate.getStyleClass().add("parking-form-card-plate");
        Label infoLine = new Label(selected.loaiXe() + "  ·  " + selected.maVeXe());
        infoLine.getStyleClass().add("parking-form-card-description");
        VBox card = new VBox(5, mini, plate, infoLine);
        card.getStyleClass().add("parking-form-card");
        card.setMaxWidth(Double.MAX_VALUE);

        TextField txtNext = new TextField(next.toString());
        txtNext.setPromptText("YYYY-MM");
        txtNext.getStyleClass().add("parking-form-input");
        txtNext.setMaxWidth(Double.MAX_VALUE);
        VBox monthField = ticketFormField("Tháng muốn gia hạn (YYYY-MM)", txtNext);

        Label note = new Label("ⓘ  Tháng đang đăng ký: " + oldMonth
                + "  ·  Phí tháng mới: 100.000đ  ·  Mỗi SV tối đa 2 xe/tháng");
        note.getStyleClass().add("parking-form-note");
        note.setWrapText(true);
        Label error = new Label();
        error.getStyleClass().add("parking-form-error");
        error.setWrapText(true);

        HBox header = ticketFormHeader("↻", title, subtitle);
        VBox content = new VBox(14, header, card, monthField, note, error);
        content.getStyleClass().add("parking-form-content");
        content.setMinWidth(0);
        content.setPrefWidth(490);
        pane.setContent(content);
        pane.setPrefWidth(580);
        pane.lookupButton(approve).getStyleClass().add("parking-form-success-button");
        pane.lookupButton(cancel).getStyleClass().add("parking-form-cancel-button");

        // Không cho đóng bằng nút xác nhận nếu tháng nhập sai hoặc không lớn hơn tháng cũ.
        pane.lookupButton(approve).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String value = txtNext.getText() == null ? "" : txtNext.getText().trim();
            if (!value.matches("(?!0000)\\d{4}-(0[1-9]|1[0-2])")) {
                error.setText("Hãy nhập đúng tháng dạng YYYY-MM, ví dụ 2026-12.");
                e.consume();
                return;
            }
            YearMonth target = YearMonth.parse(value);
            if (!target.isAfter(oldMonth)) {
                error.setText("Tháng gia hạn phải sau tháng " + oldMonth + ".");
                e.consume();
                return;
            }
            if (target.isBefore(YearMonth.now())) {
                error.setText("Không thể gia hạn cho tháng đã qua (" + target + ").");
                e.consume();
            }
        });
        dialog.setResultConverter(button -> button == approve
                ? YearMonth.parse(txtNext.getText().trim()) : null);

        dialog.showAndWait().ifPresent(target -> {
            try {
                String newId = ticketDAO.renew(selected, target);
                info("Gia hạn thành công", "Đã tạo vé mới: " + newId
                        + "\nVé cũ được giữ nguyên để đối chiếu hóa đơn.");
                viewedMonth = target;
                txtMonth.setText(target.toString());
                loadTickets();
            } catch (Exception ex) { error("Không gia hạn được vé", ex); }
        });
    }

    // Chọn tháng tùy ý, không phụ thuộc tháng có trong CSDL hay danh sách có sẵn.
    @FXML private void handleApplyMonth() {
        String input = txtMonth.getText() == null ? "" : txtMonth.getText().trim();
        if (!input.matches("(?!0000)\\d{4}-(0[1-9]|1[0-2])")) {
            warn("Tháng phải có định dạng YYYY-MM (ví dụ 2024-03, 2026-11).\n"
                    + "Tháng phải thuộc khoảng 01–12 và năm từ 0001–9999.");
            txtMonth.requestFocus();
            txtMonth.selectAll();
            return;
        }
        try {
            viewedMonth = YearMonth.parse(input);
            txtMonth.setText(viewedMonth.toString());
            loadTickets();
        } catch (java.time.format.DateTimeParseException ex) {
            warn("Tháng không hợp lệ. Hãy nhập YYYY-MM, ví dụ 2026-10.");
            txtMonth.requestFocus();
            txtMonth.selectAll();
        }
    }

    private VeXeThangRow selectedTicket() {
        VeXeThangRow value = tableTickets.getSelectionModel().getSelectedItem();
        if (value == null) warn("Hãy chọn một vé trên bảng trước.");
        return value;
    }

    private record TicketForm(String plate, String type, String owner) {}

    // Dùng chung style cho hai hộp thoại Sửa/Đăng ký và Gia hạn.
    private void applyTicketFormStyle(DialogPane pane) {
        pane.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/com/ptit/quanlyktx/css/guixe.css")
        ).toExternalForm());
        pane.getStyleClass().add("parking-form-dialog");
    }

    // Phần đầu dialog đồng bộ: biểu tượng, tiêu đề và mô tả.
    private HBox ticketFormHeader(String symbol, Label title, Label subtitle) {
        Label icon = new Label(symbol);
        icon.getStyleClass().add("parking-form-symbol");
        icon.setMinSize(48, 48);
        icon.setPrefSize(48, 48);
        icon.setAlignment(javafx.geometry.Pos.CENTER);
        title.setWrapText(true);
        subtitle.setWrapText(true);
        VBox captions = new VBox(4, title, subtitle);
        captions.setMinWidth(0);
        HBox.setHgrow(captions, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(13, icon, captions);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return header;
    }

    private VBox ticketFormField(String caption, Control field) {
        Label label = new Label(caption);
        label.getStyleClass().add("parking-form-label");
        field.setMinWidth(0);
        field.setMaxWidth(Double.MAX_VALUE);
        VBox result = new VBox(7, label, field);
        result.setMinWidth(0);
        result.setMaxWidth(Double.MAX_VALUE);
        return result;
    }

    private Optional<TicketForm> editTicketForm(VeXeThangRow old) {
        boolean editing = old != null;
        String month = editing ? old.thangDangKy() : viewedMonth.toString();
        Dialog<TicketForm> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Sửa vé tháng" : "Đăng ký vé xe tháng");
        dialog.setHeaderText(null);
        if (tabPane.getScene() != null) dialog.initOwner(tabPane.getScene().getWindow());
        ButtonType save = new ButtonType(editing ? "✓  Lưu thay đổi" : "+  Đăng ký vé",
                ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(save, cancel);
        applyTicketFormStyle(pane);

        Label title = new Label(editing ? "Chỉnh sửa vé xe tháng" : "Đăng ký vé xe tháng");
        title.getStyleClass().add("parking-form-heading");
        Label subtitle = new Label(editing
                ? "Điều chỉnh thông tin phương tiện và người đăng ký"
                : "Nhập thông tin xe và sinh viên để tạo vé mới");
        subtitle.getStyleClass().add("parking-form-subheading");

        Label cardCaption = new Label(editing ? "MÃ VÉ · THÁNG ĐĂNG KÝ" : "THÁNG ĐĂNG KÝ");
        cardCaption.getStyleClass().add("parking-form-card-caption");
        Label cardTitle = new Label(editing ? old.maVeXe() : month);
        cardTitle.getStyleClass().add("parking-form-card-plate");
        Label cardInfo = new Label(editing ? "Tháng " + month + " · " + old.bienSoXe()
                : "Phí cố định 100.000đ/vé · Mã vé tự động tạo");
        cardInfo.getStyleClass().add("parking-form-card-description");
        VBox card = new VBox(5, cardCaption, cardTitle, cardInfo);
        card.getStyleClass().add("parking-form-card");
        card.setMaxWidth(Double.MAX_VALUE);

        TextField plate = new TextField(editing ? old.bienSoXe() : "");
        TextField type = new TextField(editing ? old.loaiXe() : "");
        plate.setPromptText("Ví dụ: 29B1-12345");
        type.setPromptText("Ví dụ: Xe máy Honda Wave");
        plate.getStyleClass().add("parking-form-input");
        type.getStyleClass().add("parking-form-input");
        plate.setMinWidth(0);
        type.setMinWidth(0);

        ComboBox<SinhVienRef> owner = new ComboBox<>();
        owner.getStyleClass().add("parking-form-select");
        owner.setPromptText("Chọn sinh viên đăng ký...");
        owner.setMinWidth(0);
        owner.setMaxWidth(Double.MAX_VALUE);
        try {
            owner.setItems(FXCollections.observableArrayList(ticketDAO.findStudents()));
            if (editing) owner.getItems().stream()
                    // Dùng so sánh không phân biệt chữ hoa/chữ thường cho mã sinh viên.
                    .filter(s -> s.maSV().equalsIgnoreCase(old.maSV()))
                    .findFirst().ifPresent(owner::setValue);
        } catch (SQLException ex) {
            error("Không tải được danh sách sinh viên", ex);
            return Optional.empty();
        }

        GridPane grid = new GridPane();
        grid.setMinWidth(0);
        grid.setHgap(14);
        grid.setVgap(14);
        javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints();
        javafx.scene.layout.ColumnConstraints c2 = new javafx.scene.layout.ColumnConstraints();
        c1.setPercentWidth(50);
        c2.setPercentWidth(50);
        c1.setMinWidth(0);
        c2.setMinWidth(0);
        c1.setFillWidth(true);
        c2.setFillWidth(true);
        c1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        c2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);
        grid.add(ticketFormField("Biển số xe", plate), 0, 0);
        grid.add(ticketFormField("Loại phương tiện", type), 1, 0);
        grid.add(ticketFormField("Sinh viên đăng ký", owner), 0, 1, 2, 1);

        Label hint = new Label("ⓘ  Mỗi sinh viên tối đa 2 vé/tháng. Phí vé: 100.000đ/tháng.");
        hint.getStyleClass().add("parking-form-note");
        hint.setWrapText(true);
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("parking-form-error");
        errorLabel.setWrapText(true);

        HBox header = ticketFormHeader(editing ? "✎" : "+", title, subtitle);
        VBox content = new VBox(14, header, card, grid, hint, errorLabel);
        content.getStyleClass().add("parking-form-content");
        content.setMinWidth(0);
        content.setPrefWidth(618);
        pane.setContent(content);
        pane.setPrefWidth(710);
        pane.lookupButton(save).getStyleClass().add("parking-form-save-button");
        pane.lookupButton(cancel).getStyleClass().add("parking-form-cancel-button");
        pane.lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (plate.getText().isBlank() || type.getText().isBlank() || owner.getValue() == null) {
                errorLabel.setText("Vui lòng nhập biển số, loại xe và chọn sinh viên.");
                e.consume();
            }
        });
        dialog.setResultConverter(b -> b == save
                ? new TicketForm(plate.getText().strip(), type.getText().strip(), owner.getValue().maSV())
                : null);
        return dialog.showAndWait();
    }

    // Dialog chi tiết được tạo bằng JavaFX nodes, style tách riêng trong guixe.css.
    private VBox ticketInfoField(String title, String value) {
        Label caption = new Label(title);
        caption.getStyleClass().add("parking-detail-caption");
        Label text = new Label(value == null || value.isBlank() ? "—" : value);
        text.getStyleClass().add("parking-detail-value");
        text.setWrapText(true);
        VBox field = new VBox(5, caption, text);
        field.getStyleClass().add("parking-detail-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private void showTicketDetails(VeXeThangRow t) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết vé tháng · " + t.maVeXe());
        dialog.setHeaderText(null);
        if (tabPane.getScene() != null) dialog.initOwner(tabPane.getScene().getWindow());

        ButtonType edit = new ButtonType("✎  Sửa vé", ButtonBar.ButtonData.OTHER);
        ButtonType renew = new ButtonType("↻  Gia hạn", ButtonBar.ButtonData.OTHER);
        ButtonType close = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(edit, renew, close);
        pane.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/com/ptit/quanlyktx/css/guixe.css")
        ).toExternalForm());
        pane.getStyleClass().add("parking-detail-dialog");

        Label heading = new Label("Chi tiết vé tháng");
        heading.getStyleClass().add("parking-detail-heading");
        Label hint = new Label("Mã vé: " + t.maVeXe());
        hint.getStyleClass().add("parking-detail-subheading");
        VBox headline = new VBox(4, heading, hint);
        HBox.setHgrow(headline, javafx.scene.layout.Priority.ALWAYS);

        String state = status(t);
        Label badge = new Label(state);
        badge.getStyleClass().add("parking-detail-badge");
        if (state.startsWith("✓")) badge.getStyleClass().add("parking-detail-badge-valid");
        else if (state.equals("Hết hạn")) badge.getStyleClass().add("parking-detail-badge-expired");
        else badge.getStyleClass().add("parking-detail-badge-future");

        HBox top = new HBox(12, headline, badge);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label plateTitle = new Label("BIỂN KIỂM SOÁT");
        plateTitle.getStyleClass().add("parking-detail-plate-caption");
        Label plate = new Label(t.bienSoXe());
        plate.getStyleClass().add("parking-detail-plate-number");
        Label type = new Label(t.loaiXe() + "  ·  " + t.maVeXe());
        type.getStyleClass().add("parking-detail-plate-description");
        VBox plateCard = new VBox(7, plateTitle, plate, type);
        plateCard.getStyleClass().add("parking-detail-plate-card");
        plateCard.setMaxWidth(Double.MAX_VALUE);

        YearMonth month = YearMonth.parse(t.thangDangKy());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate ngayBatDau = month.atDay(1);
        if (t.ngayDangKy() != null && t.ngayDangKy().isAfter(ngayBatDau)) {
            ngayBatDau = t.ngayDangKy();
        }
        LocalDate ngayKetThuc = month.atEndOfMonth();
        String validity = ngayBatDau.isAfter(ngayKetThuc)
                ? "Ngày đăng ký nằm sau kỳ vé – kiểm tra lại dữ liệu"
                : ngayBatDau.format(fmt) + " – " + ngayKetThuc.format(fmt);
        String ngayDangKy = t.ngayDangKy() == null
                ? "Chưa ghi nhận (vé cũ)" : t.ngayDangKy().format(fmt);

        GridPane details = new GridPane();
        details.getStyleClass().add("parking-detail-grid");
        details.setHgap(12);
        details.setVgap(12);
        javafx.scene.layout.ColumnConstraints half1 = new javafx.scene.layout.ColumnConstraints();
        javafx.scene.layout.ColumnConstraints half2 = new javafx.scene.layout.ColumnConstraints();
        half1.setPercentWidth(50);
        half2.setPercentWidth(50);
        details.getColumnConstraints().addAll(half1, half2);
        details.add(ticketInfoField("Chủ xe", t.hoTen()), 0, 0);
        details.add(ticketInfoField("Mã sinh viên", t.maSV()), 1, 0);
        details.add(ticketInfoField("Lớp", t.lop()), 0, 1);
        details.add(ticketInfoField("Phòng", t.soPhong()), 1, 1);
        details.add(ticketInfoField("Tháng đăng ký", t.thangDangKy()), 0, 2);
        details.add(ticketInfoField("Ngày đăng ký thực tế", ngayDangKy), 1, 2);
        details.add(ticketInfoField("Phí vé tháng", money(t.phiHangThang())), 0, 3, 2, 1);

        Label validityCaption = new Label("THỜI HẠN VÉ");
        validityCaption.getStyleClass().add("parking-detail-caption");
        Label validityValue = new Label(validity);
        validityValue.getStyleClass().add("parking-detail-validity");
        VBox validityBox = new VBox(5, validityCaption, validityValue);
        validityBox.getStyleClass().add("parking-detail-validity-box");

        VBox content = new VBox(16, top, plateCard, details, validityBox);
        content.getStyleClass().add("parking-detail-content");
        content.setMinWidth(0);
        content.setPrefWidth(600);
        pane.setContent(content);
        pane.setPrefWidth(675);
        pane.setMinWidth(590);
        pane.lookupButton(edit).getStyleClass().add("parking-detail-edit-button");
        pane.lookupButton(renew).getStyleClass().add("parking-detail-renew-button");
        pane.lookupButton(close).getStyleClass().add("parking-detail-close-button");
        dialog.setResultConverter(button -> button);

        dialog.showAndWait().ifPresent(button -> {
            tableTickets.getSelectionModel().select(t);
            if (button == edit) handleEditTicket();
            else if (button == renew) renewTicket(t);
        });
    }

    // -------- TAB 2: GUI / LAY XE --------
    @FXML private void handleLookupPlate() {
        String plate = txtPlate.getText() == null ? "" : txtPlate.getText().strip();
        if (plate.isEmpty()) { warn("Nhập biển số trước khi tra cứu."); return; }
        try {
            lookedUpTicket = ticketDAO.findByPlate(plate, YearMonth.now());
            showLookup();
        } catch (SQLException ex) { error("Tra cứu biển số thất bại", ex); }
    }

    private void showLookup() {
        if (lookedUpTicket == null) {
            lblPlate.setText("Không tìm thấy vé trong tháng hiện tại");
            lblOwner.setText("Vui lòng kiểm tra biển số và tháng hiện tại.");
            lblOwnerInfo.setText("");
            lblValidity.setText("Không có vé");
            lblValidity.getStyleClass().removeAll(
                    "parking-status-valid", "parking-status-expired", "parking-status-neutral");
            lblValidity.getStyleClass().add("parking-status-neutral");
            lblVehicleType.setText("—"); lblVehicleId.setText("—");
            lblFreeCount.setText("— / 2");
            lblGateHint.setText("Tra cứu biển số để kiểm tra vé tháng và thực hiện gửi/lấy xe.");
            btnCheckIn.setDisable(true); btnCheckOut.setDisable(true);
            return;
        }
        VeXeThangRow t = lookedUpTicket;
        lblPlate.setText(t.bienSoXe());
        lblOwner.setText(t.maSV() + " – " + t.hoTen());
        lblOwnerInfo.setText("Lớp " + t.lop() + " · Phòng " + t.soPhong());
        lblVehicleType.setText(t.loaiXe());
        lblVehicleId.setText(t.maVeXe());
        String veStatus = status(t);
        boolean coHieuLuc = t.conHieuLuc();
        lblValidity.setText(veStatus + " · " + t.thangDangKy());
        lblValidity.getStyleClass().removeAll(
                "parking-status-neutral", "parking-status-expired", "parking-status-valid");
        lblValidity.getStyleClass().add(coHieuLuc ? "parking-status-valid"
                : (veStatus.equals("Hết hạn") ? "parking-status-expired" : "parking-status-neutral"));
        boolean parked = openTrips.stream().anyMatch(p -> p.maVeXe().equals(t.maVeXe()));
        try {
            int used = tripDAO.countToday(t.maVeXe());
            lblFreeCount.setText(Math.max(0, 2 - used) + " / 2");
            lblGateHint.setText(parked
                    ? "Xe đang trong bãi. Hãy xác nhận lấy trước khi gửi tiếp."
                    : (!coHieuLuc
                        ? "Vé chưa có hiệu lực hôm nay. Không thể ghi nhận gửi mới."
                        : "Xe chưa được gửi. Từ lượt gửi thứ 3 trong ngày, phí phát sinh là 3.000đ/lượt."));
        } catch (SQLException ex) {
            lblFreeCount.setText("— / 2");
            lblGateHint.setText("Chưa đọc được số lượt trong ngày.");
        }
        btnCheckIn.setDisable(!coHieuLuc || parked);
        // Xe đã ở trong bãi vẫn được phép ra, kể cả nếu kỳ vé vừa hết hạn.
        btnCheckOut.setDisable(!parked);
    }

    private void loadGate() {
        try {
            openTrips.setAll(tripDAO.findOpen());
            lblParked.setText(openTrips.size() + " xe");
            lblTodayFees.setText(money(tripDAO.todayFees()));
            showLookup();
        } catch (SQLException ex) { error("Không tải được xe đang trong bãi", ex); }
    }

    @FXML private void handleRefreshGate() { loadGate(); }

    @FXML private void handleCheckIn() {
        if (lookedUpTicket == null) { warn("Hãy tra cứu xe có vé tháng trước."); return; }
        try {
            // Đọc lại từ SQLite để tránh dùng dữ liệu tra cứu đã cũ khi ngày đổi.
            VeXeThangRow latest = ticketDAO.findByPlate(
                    lookedUpTicket.bienSoXe(), YearMonth.now());
            if (latest == null || !latest.maVeXe().equals(lookedUpTicket.maVeXe())
                    || !latest.conHieuLuc()) {
                lookedUpTicket = latest;
                showLookup();
                warn("Vé tháng chưa có hiệu lực tại ngày hiện tại hoặc đã thay đổi."
                        + " Hãy kiểm tra ngày đăng ký rồi tra cứu lại.");
                return;
            }
            lookedUpTicket = latest;
            if (openTrips.stream().anyMatch(p -> p.maVeXe().equals(latest.maVeXe()))) {
                warn("Xe đang ở trong bãi. Hãy xác nhận lấy trước khi gửi tiếp.");
                return;
            }
            int used = tripDAO.countToday(lookedUpTicket.maVeXe());
            BigDecimal expected = used >= 2 ? new BigDecimal("3000") : BigDecimal.ZERO;
            String message = expected.signum() > 0
                    ? "Đây là lượt vượt 2 lượt miễn phí hôm nay. Phí phát sinh " + money(expected) + "."
                    : "Lượt này được miễn phí theo quy tắc 2 lượt/xe/ngày.";
            if (!confirm("Ghi nhận gửi xe " + lookedUpTicket.bienSoXe() + "?", message)) return;
            BigDecimal actual = tripDAO.checkIn(lookedUpTicket.maVeXe());
            loadGate(); loadHistory();
            info("Đã ghi nhận gửi", "Biển số " + lookedUpTicket.bienSoXe() + "\nPhí phát sinh: " + money(actual));
        } catch (SQLException ex) { error("Không ghi nhận gửi được", ex); }
    }

    @FXML private void handleCheckOutSelected() {
        if (lookedUpTicket == null) { warn("Hãy tra cứu xe trước."); return; }
        LuotGuiXeRow open = openTrips.stream()
                .filter(t -> t.maVeXe().equals(lookedUpTicket.maVeXe()))
                .findFirst().orElse(null);
        if (open == null) { warn("Xe chưa gửi hoặc đã lấy, không thể xác nhận lấy."); return; }
        confirmCheckout(open);
    }

    private void confirmCheckout(LuotGuiXeRow row) {
        if (!confirm("Xác nhận lấy xe " + row.bienSoXe() + "?",
                "Thời gian gửi: " + date(row.thoiGianGui()) + "\nLượt ID: " + row.idLuot())) return;
        try {
            tripDAO.checkOut(row.idLuot());
            loadGate(); loadHistory();
            info("Đã xác nhận lấy", "Xe " + row.bienSoXe() + " đã rời bãi.");
        } catch (SQLException ex) { error("Không xác nhận lấy được", ex); }
    }

    // -------- TAB 3: LICH SU --------
    private void loadHistory() {
        try {
            if (dpFrom.getValue() == null || dpTo.getValue() == null
                    || dpFrom.getValue().isAfter(dpTo.getValue())) {
                warn("Khoảng ngày tìm kiếm không hợp lệ."); return;
            }
            trips.setAll(tripDAO.findHistory(dpFrom.getValue(), dpTo.getValue()));
            applyHistoryFilter();
        } catch (SQLException ex) { error("Không tải được lịch sử gửi xe", ex); }
    }

    private void applyHistoryFilter() {
        if (filteredTrips == null) return;
        String term = txtHistorySearch.getText() == null ? "" :
                txtHistorySearch.getText().strip().toLowerCase(Locale.ROOT);
        String state = cbHistoryStatus.getValue();
        filteredTrips.setPredicate(t -> {
            boolean matching = (t.maVeXe() + " " + t.bienSoXe() + " " + t.maSV() + " " + t.hoTen())
                    .toLowerCase(Locale.ROOT).contains(term);
            boolean validState = state == null || state.equals("Tất cả")
                    || (state.equals("Đang gửi") && t.dangGui())
                    || (state.equals("Đã lấy") && !t.dangGui())
                    || (state.equals("Có phí phát sinh") && t.tienPhatSinh() != null
                            && t.tienPhatSinh().compareTo(BigDecimal.ZERO) > 0);
            return matching && validState;
        });
        BigDecimal sum = BigDecimal.ZERO;
        for (LuotGuiXeRow r : filteredTrips) {
            if (r.tienPhatSinh() != null) sum = sum.add(r.tienPhatSinh());
        }
        lblHistoryCount.setText("Hiển thị: " + filteredTrips.size() + " / " + trips.size() + " lượt");
        lblHistoryFees.setText(money(sum));
    }

    @FXML private void handleSearchHistory() { loadHistory(); }
    @FXML private void handleRefreshHistory() {
        txtHistorySearch.clear();
        cbHistoryStatus.setValue("Tất cả");
        dpFrom.setValue(LocalDate.now().minusMonths(1).withDayOfMonth(1));
        dpTo.setValue(LocalDate.now());
        loadHistory();
    }

    // -------- THÔNG BÁO CHUNG (CHỈ THAY ĐỔI GIAO DIỆN) --------
    private enum NoticeKind { SUCCESS, WARNING, ERROR, CONFIRM }

    private boolean showNotice(NoticeKind kind, String heading, String message) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(heading);
        dialog.setHeaderText(null);
        dialog.setResizable(false);
        if (tabPane != null && tabPane.getScene() != null) {
            dialog.initOwner(tabPane.getScene().getWindow());
        }

        boolean isConfirm = kind == NoticeKind.CONFIRM;
        boolean isDelete = isConfirm && heading.toLowerCase(Locale.ROOT).startsWith("xóa");
        String action = isDelete ? "Xóa vé" :
                (heading.startsWith("Ghi nhận gửi") ? "Ghi nhận gửi" :
                 (heading.startsWith("Xác nhận lấy") ? "Xác nhận lấy" : "Xác nhận"));
        ButtonType accept = new ButtonType(isConfirm ? action : "Đã hiểu", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(accept);
        if (isConfirm) pane.getButtonTypes().add(cancel);
        pane.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/com/ptit/quanlyktx/css/guixe.css")
        ).toExternalForm());
        pane.getStyleClass().add("parking-notice-dialog");

        String iconText = switch (kind) {
            case SUCCESS -> "✓";
            case WARNING -> "!";
            case ERROR -> "×";
            case CONFIRM -> "?";
        };
        String kindClass = switch (kind) {
            case SUCCESS -> "parking-notice-success";
            case WARNING -> "parking-notice-warning";
            case ERROR -> "parking-notice-error";
            case CONFIRM -> "parking-notice-confirm";
        };
        Label icon = new Label(iconText);
        icon.getStyleClass().addAll("parking-notice-icon", kindClass);
        icon.setMinSize(48, 48);
        icon.setPrefSize(48, 48);
        icon.setAlignment(javafx.geometry.Pos.CENTER);

        Label title = new Label(heading);
        title.getStyleClass().add("parking-notice-heading");
        title.setWrapText(true);
        title.setMinWidth(0);
        title.setMaxWidth(Double.MAX_VALUE);
        Label subtitle = new Label(switch (kind) {
            case SUCCESS -> "Thao tác đã hoàn tất";
            case WARNING -> "Vui lòng kiểm tra trước khi tiếp tục";
            case ERROR -> "Không thể hoàn thành thao tác";
            case CONFIRM -> "Kiểm tra thông tin trước khi xác nhận";
        });
        subtitle.getStyleClass().add("parking-notice-subtitle");
        subtitle.setWrapText(true);
        VBox headingBox = new VBox(4, title, subtitle);
        headingBox.setMinWidth(0);
        HBox.setHgrow(headingBox, javafx.scene.layout.Priority.ALWAYS);
        HBox top = new HBox(14, icon, headingBox);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label body = new Label(message == null || message.isBlank() ? "Vui lòng thử lại." : message);
        body.getStyleClass().add("parking-notice-message");
        body.setWrapText(true);
        body.setMinWidth(0);
        body.setPrefWidth(390);
        body.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(16, top, new Separator(), body);
        content.getStyleClass().add("parking-notice-content");
        content.setMinWidth(0);
        content.setPrefWidth(418);
        pane.setContent(content);
        pane.setPrefWidth(495);
        pane.lookupButton(accept).getStyleClass().add(isDelete
                ? "parking-notice-danger-button" : "parking-notice-confirm-button");
        if (isConfirm) pane.lookupButton(cancel).getStyleClass().add("parking-notice-cancel-button");
        dialog.setResultConverter(button -> button);
        return dialog.showAndWait().orElse(null) == accept;
    }

    private void warn(String message) {
        showNotice(NoticeKind.WARNING, "Cần kiểm tra thông tin", message);
    }
    private void info(String title, String message) {
        showNotice(NoticeKind.SUCCESS, title, message);
    }
    private void error(String title, Exception ex) {
        String message = ex == null ? null : ex.getMessage();
        showNotice(NoticeKind.ERROR, title, message);
    }
    private boolean confirm(String title, String message) {
        return showNotice(NoticeKind.CONFIRM, title, message);
    }
}
