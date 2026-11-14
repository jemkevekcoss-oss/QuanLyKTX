package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.dao.DichVuDAO;
import com.ptit.quanlyktx.dao.SinhVienDAO;
import com.ptit.quanlyktx.dao.SuDungDichVuDAO;
import com.ptit.quanlyktx.dto.ChiPhiDichVuRow;
import com.ptit.quanlyktx.dto.LichSuDichVuRow;
import com.ptit.quanlyktx.model.DichVu;
import com.ptit.quanlyktx.model.SinhVien;
import com.ptit.quanlyktx.model.SuDungDichVu;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.geometry.Insets;

/** Màn Dịch vụ: Danh mục (CRUD), Lịch sử (CRUD), Thống kê sử dụng (truy vấn 2). */
public class DichVuController {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    private final DichVuDAO dichVuDAO = new DichVuDAO();
    private final SuDungDichVuDAO suDungDAO = new SuDungDichVuDAO();
    private final SinhVienDAO sinhVienDAO = new SinhVienDAO();

    private final ObservableList<DichVu> services = FXCollections.observableArrayList();
    private final ObservableList<LichSuDichVuRow> usages = FXCollections.observableArrayList();
    private final ObservableList<ChiPhiDichVuRow> reports = FXCollections.observableArrayList();
    private FilteredList<DichVu> filteredServices;
    private FilteredList<LichSuDichVuRow> filteredUsages;
    private Consumer<String> onNavigate;

    @FXML private Label lblTotalServices, lblMonthUses, lblMonthMoney;
    @FXML private Label lblCatalogCount, lblHistoryCount, lblReportCount, lblReportTotal;
    @FXML private TextField txtServiceSearch, txtHistorySearch;
    @FXML private DatePicker dpFrom, dpTo;
    @FXML private TabPane tabPane;
    @FXML private Tab tabCatalog, tabHistory, tabReport;

    @FXML private TableView<DichVu> tableServices;
    @FXML private TableColumn<DichVu, String> colMaDV, colTenDV, colDonGia, colDonViTinh;

    @FXML private TableView<LichSuDichVuRow> tableUsages;
    @FXML private TableColumn<LichSuDichVuRow, String> colIdSuDung, colMaSV, colHoTen,
            colLichSuDV, colThoiGian, colSoLuong, colThanhTien;

    @FXML private TableView<ChiPhiDichVuRow> tableReport;
    @FXML private TableColumn<ChiPhiDichVuRow, String> colTKMaSV, colTKHoTen,
            colTKTenDV, colTKSLSuDung, colTKTongTien;

    /** MainController gắn callback để nút "Xem doanh thu" chuyển tới mục Thống kê. */
    public void setOnNavigate(Consumer<String> handler) { this.onNavigate = handler; }

    @FXML private void initialize() {
        colMaDV.setCellValueFactory(c -> cell(c.getValue().getMaDV()));
        colTenDV.setCellValueFactory(c -> cell(c.getValue().getTenDV()));
        colDonGia.setCellValueFactory(c -> cell(money(c.getValue().getDonGia())));
        colDonViTinh.setCellValueFactory(c -> cell(c.getValue().getDonViTinh()));

        colIdSuDung.setCellValueFactory(c -> cell(String.valueOf(c.getValue().getSuDung().getIdSuDung())));
        colMaSV.setCellValueFactory(c -> cell(c.getValue().getSuDung().getMaSV()));
        colHoTen.setCellValueFactory(c -> cell(c.getValue().getTenSV()));
        colLichSuDV.setCellValueFactory(c -> cell(c.getValue().getTenDV()));
        colThoiGian.setCellValueFactory(c -> cell(dateTime(c.getValue().getSuDung().getThoiGianSuDung())));
        colSoLuong.setCellValueFactory(c -> cell(c.getValue().getSuDung().getSoLuong() + " " + safe(c.getValue().getDonViTinh())));
        colThanhTien.setCellValueFactory(c -> cell(money(c.getValue().getSuDung().getThanhTien())));

        colTKMaSV.setCellValueFactory(c -> cell(c.getValue().getMaSV()));
        colTKHoTen.setCellValueFactory(c -> cell(c.getValue().getHoTen()));
        colTKTenDV.setCellValueFactory(c -> cell(c.getValue().getTenDV()));
        colTKSLSuDung.setCellValueFactory(c -> cell(c.getValue().getTongSoLuong() + " " + safe(c.getValue().getDonViTinh())));
        colTKTongTien.setCellValueFactory(c -> cell(money(c.getValue().getTongTien())));

        tableServices.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableUsages.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableReport.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        filteredServices = new FilteredList<>(services, s -> true);
        tableServices.setItems(filteredServices);
        filteredUsages = new FilteredList<>(usages, r -> true);
        tableUsages.setItems(filteredUsages);
        tableReport.setItems(reports);

        txtServiceSearch.textProperty().addListener((obs, oldV, newV) -> applyServiceFilter());
        txtHistorySearch.textProperty().addListener((obs, oldV, newV) -> applyHistoryFilter());
        filteredServices.addListener((javafx.collections.ListChangeListener<DichVu>) c -> updateCounts());
        filteredUsages.addListener((javafx.collections.ListChangeListener<LichSuDichVuRow>) c -> updateCounts());

        LocalDate today = LocalDate.now();
        dpFrom.setValue(today.withDayOfMonth(1));
        dpTo.setValue(YearMonth.from(today).atEndOfMonth());
        refreshAll(null, null);
        runReport();
    }

    private static ReadOnlyStringWrapper cell(String s) { return new ReadOnlyStringWrapper(safe(s)); }
    private static String safe(String s) { return s == null ? "" : s; }
    private static String money(BigDecimal n) {
        if (n == null) return "0 đ";
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(2);
        return nf.format(n) + " đ";
    }
    private static String dateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    private void applyServiceFilter() {
        String term = safe(txtServiceSearch.getText()).trim().toLowerCase(Locale.ROOT);
        filteredServices.setPredicate(dv -> term.isEmpty() ||
                (safe(dv.getMaDV()) + " " + safe(dv.getTenDV()) + " " + safe(dv.getDonViTinh()))
                        .toLowerCase(Locale.ROOT).contains(term));
        updateCounts();
    }
    private void applyHistoryFilter() {
        String term = safe(txtHistorySearch.getText()).trim().toLowerCase(Locale.ROOT);
        filteredUsages.setPredicate(row -> {
            SuDungDichVu sd = row.getSuDung();
            String searchable = sd.getIdSuDung() + " " + safe(sd.getMaSV()) + " "
                    + safe(row.getTenSV()) + " " + safe(sd.getMaDV()) + " " + safe(row.getTenDV());
            return term.isEmpty() || searchable.toLowerCase(Locale.ROOT).contains(term);
        });
        updateCounts();
    }

    private void updateCounts() {
        if (filteredServices == null || filteredUsages == null) return;
        lblCatalogCount.setText("Hiển thị: " + filteredServices.size() + " / " + services.size() + " dịch vụ");
        lblHistoryCount.setText("Hiển thị: " + filteredUsages.size() + " / " + usages.size() + " lượt sử dụng");
    }
    private void updateStats() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate endExclusive = start.plusMonths(1);
        List<SuDungDichVu> month = usages.stream().map(LichSuDichVuRow::getSuDung)
                .filter(u -> u.getThoiGianSuDung() != null)
                .filter(u -> {
                    LocalDate date = u.getThoiGianSuDung().toLocalDate();
                    return !date.isBefore(start) && date.isBefore(endExclusive);
                }).toList();
        BigDecimal sum = month.stream().map(SuDungDichVu::getThanhTien)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalServices.setText(String.valueOf(services.size()));
        lblMonthUses.setText(String.valueOf(month.size()));
        lblMonthMoney.setText(money(sum));
    }

    private void refreshAll(String serviceId, Integer usageId) {
        try {
            List<DichVu> fresh = dichVuDAO.findAll();
            services.setAll(fresh);
            if (serviceId != null) services.stream().filter(d -> serviceId.equals(d.getMaDV()))
                    .findFirst().ifPresent(d -> tableServices.getSelectionModel().select(d));
        } catch (SQLException e) { sqlError("Không đọc được danh mục dịch vụ", e); }
        try {
            List<LichSuDichVuRow> fresh = suDungDAO.findAll();
            usages.setAll(fresh);
            if (usageId != null) usages.stream().filter(r -> r.getSuDung().getIdSuDung() == usageId)
                    .findFirst().ifPresent(r -> tableUsages.getSelectionModel().select(r));
        } catch (SQLException e) { sqlError("Không đọc được lịch sử sử dụng", e); }
        applyServiceFilter();
        applyHistoryFilter();
        updateStats();
    }

    @FXML private void handleRefreshCatalog() { txtServiceSearch.clear(); refreshAll(null, null); }
    @FXML private void handleRefreshHistory() { txtHistorySearch.clear(); refreshAll(null, null); }

    @FXML private void handleAddService() {
        serviceForm(null).ifPresent(dv -> {
            try {
                if (dichVuDAO.insert(dv)) { refreshAll(dv.getMaDV(), null); info("Dịch vụ đã được thêm vào cơ sở dữ liệu."); }
                else warn("Không thêm được dịch vụ.");
            } catch (SQLException ex) { sqlError("Không thể thêm dịch vụ", ex); }
        });
    }
    @FXML private void handleEditService() {
        DichVu current = tableServices.getSelectionModel().getSelectedItem();
        if (current == null) { warn("Hãy chọn một dịch vụ để sửa."); return; }
        serviceForm(current).ifPresent(dv -> {
            try {
                if (dichVuDAO.update(dv)) { refreshAll(dv.getMaDV(), null); info("Đã cập nhật dịch vụ."); }
                else warn("Dịch vụ không còn tồn tại.");
            } catch (SQLException ex) { sqlError("Không thể sửa dịch vụ", ex); }
        });
    }
    @FXML private void handleDeleteService() {
        DichVu dv = tableServices.getSelectionModel().getSelectedItem();
        if (dv == null) { warn("Hãy chọn dịch vụ cần xóa."); return; }
        if (!confirm("Xóa dịch vụ " + dv.getMaDV() + "?\nDịch vụ đã có lịch sử sử dụng sẽ không thể xóa để bảo toàn dữ liệu hóa đơn.")) return;
        try {
            if (dichVuDAO.delete(dv.getMaDV())) { refreshAll(null, null); info("Đã xóa dịch vụ."); }
            else warn("Dịch vụ không còn tồn tại.");
        } catch (SQLException ex) { sqlError("Không thể xóa dịch vụ (có thể đã được sử dụng)", ex); }
    }

    private Optional<DichVu> serviceForm(DichVu current) {
        boolean edit = current != null;
        Dialog<DichVu> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Cập nhật dịch vụ" : "Thêm dịch vụ");
        prepareDialog(dialog);
        ButtonType save = new ButtonType(edit ? "Lưu thay đổi" : "Thêm dịch vụ", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(save, cancel);

        TextField ma = new TextField(edit ? current.getMaDV() : "");
        TextField ten = new TextField(edit ? current.getTenDV() : "");
        TextField gia = new TextField(edit ? current.getDonGia().toPlainString() : "");
        TextField dvt = new TextField(edit ? current.getDonViTinh() : "");
        ma.setPromptText("Ví dụ: DV_GIAT");
        ten.setPromptText("Nhập tên dịch vụ");
        gia.setPromptText("Ví dụ: 15000");
        dvt.setPromptText("Ví dụ: kg, suất, bình...");
        ma.setDisable(edit); // Mã dịch vụ là khóa chính: không sửa.

        GridPane grid = formGrid();
        grid.add(formField("Mã dịch vụ", ma, true), 0, 0);
        grid.add(formField("Tên dịch vụ", ten, true), 1, 0);
        grid.add(formField("Đơn giá (VNĐ)", gia, true), 0, 1);
        grid.add(formField("Đơn vị tính", dvt, true), 1, 1);

        Label err = errorLabel();
        VBox content = dialogContent(
                dialogHeading(edit ? "✎" : "+",
                        edit ? "Cập nhật thông tin dịch vụ" : "Thêm dịch vụ mới",
                        "Quản lý danh mục dịch vụ của ký túc xá."),
                hintBox(edit ? "Mã dịch vụ được giữ nguyên khi chỉnh sửa hồ sơ."
                             : "Điền đầy đủ 4 thông tin bắt buộc trước khi lưu."),
                sectionTitle("THÔNG TIN DỊCH VỤ"), grid, err);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(700);
        styleDialogButtons(dialog, save, cancel);

        Node saveBtn = dialog.getDialogPane().lookupButton(save);
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            String error = validateService(ma.getText(), ten.getText(), gia.getText(), dvt.getText());
            if (error != null) { showFormError(err, error); ev.consume(); }
        });
        dialog.setResultConverter(btn -> btn == save
                ? new DichVu(ma.getText().trim(), ten.getText().trim(),
                        new BigDecimal(gia.getText().trim()), dvt.getText().trim()) : null);
        return dialog.showAndWait();
    }

    private String validateService(String ma, String ten, String gia, String dvt) {
        if (ma.isBlank() || ten.isBlank() || gia.isBlank() || dvt.isBlank()) return "Hãy nhập đầy đủ 4 trường.";
        if (ma.trim().length() > 20 || ten.trim().length() > 100 || dvt.trim().length() > 30)
            return "Một trường đã vượt độ dài VARCHAR quy định.";
        try {
            BigDecimal value = new BigDecimal(gia.trim());
            if (value.signum() < 0 || value.scale() > 2 || value.precision() - value.scale() > 10)
                return "Đơn giá phải >= 0 và phù hợp DECIMAL(12,2).";
        } catch (NumberFormatException ex) { return "Đơn giá phải là số, ví dụ 15000."; }
        return null;
    }

    @FXML private void handleAddUsage() { usageForm(null).ifPresent(this::saveNewUsage); }
    @FXML private void handleEditUsage() {
        LichSuDichVuRow selected = tableUsages.getSelectionModel().getSelectedItem();
        if (selected == null) { warn("Hãy chọn một lượt sử dụng để sửa."); return; }
        usageForm(selected).ifPresent(sd -> {
            try {
                if (suDungDAO.update(sd)) {
                    refreshAll(null, sd.getIdSuDung()); runReport(); info("Đã sửa lượt sử dụng.");
                } else warn("Lượt sử dụng này không còn trong cơ sở dữ liệu.");
            } catch (SQLException ex) { sqlError("Không thể sửa lượt sử dụng", ex); }
        });
    }
    private void saveNewUsage(SuDungDichVu sd) {
        try {
            if (suDungDAO.insert(sd)) {
                refreshAll(null, null); runReport(); info("Đã ghi nhận lượt sử dụng vào cơ sở dữ liệu.");
            } else warn("Không thể ghi nhận lượt sử dụng.");
        } catch (SQLException ex) { sqlError("Không thể ghi nhận lượt sử dụng", ex); }
    }
    @FXML private void handleDeleteUsage() {
        LichSuDichVuRow row = tableUsages.getSelectionModel().getSelectedItem();
        if (row == null) { warn("Hãy chọn một lượt sử dụng cần xóa."); return; }
        int id = row.getSuDung().getIdSuDung();
        if (!confirm("Xóa lượt sử dụng #" + id + "?\nThao tác sẽ xóa dữ liệu và làm thay đổi tổng chi phí trong báo cáo.")) return;
        try {
            if (suDungDAO.delete(id)) { refreshAll(null, null); runReport(); info("Đã xóa lượt sử dụng."); }
            else warn("Lượt sử dụng không còn tồn tại.");
        } catch (SQLException ex) { sqlError("Không thể xóa lượt sử dụng", ex); }
    }

    private Optional<SuDungDichVu> usageForm(LichSuDichVuRow row) {
        SuDungDichVu current = row == null ? null : row.getSuDung();
        boolean edit = current != null;
        List<SinhVien> students;
        List<DichVu> serviceChoices;
        try {
            students = sinhVienDAO.findAll();
            serviceChoices = dichVuDAO.findAll();
        } catch (SQLException ex) { sqlError("Không tải được danh sách sinh viên/dịch vụ", ex); return Optional.empty(); }
        if (students.isEmpty() || serviceChoices.isEmpty()) {
            warn("Cần có ít nhất 1 sinh viên và 1 dịch vụ trước khi ghi nhận sử dụng.");
            return Optional.empty();
        }

        Dialog<SuDungDichVu> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Sửa lượt sử dụng #" + current.getIdSuDung() : "Ghi nhận sử dụng dịch vụ");
        prepareDialog(dialog);
        ButtonType save = new ButtonType(edit ? "Lưu thay đổi" : "Ghi nhận sử dụng", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(save, cancel);

        ComboBox<SinhVien> svBox = new ComboBox<>(FXCollections.observableArrayList(students));
        svBox.setConverter(new StringConverter<>() {
            @Override public String toString(SinhVien s) { return s == null ? "" : s.getMaSV() + " - " + s.getHoTen(); }
            @Override public SinhVien fromString(String text) { return null; }
        });
        ComboBox<DichVu> dvBox = new ComboBox<>(FXCollections.observableArrayList(serviceChoices));
        dvBox.setConverter(new StringConverter<>() {
            @Override public String toString(DichVu d) { return d == null ? "" : d.getMaDV() + " - " + d.getTenDV(); }
            @Override public DichVu fromString(String text) { return null; }
        });
        if (edit) {
            students.stream().filter(s -> s.getMaSV().equals(current.getMaSV()))
                    .findFirst().ifPresent(svBox::setValue);
            serviceChoices.stream().filter(s -> s.getMaDV().equals(current.getMaDV()))
                    .findFirst().ifPresent(dvBox::setValue);
        } else {
            svBox.getSelectionModel().selectFirst();
            dvBox.getSelectionModel().selectFirst();
        }
        svBox.setMaxWidth(Double.MAX_VALUE);
        dvBox.setMaxWidth(Double.MAX_VALUE);
        DatePicker day = new DatePicker(edit ? current.getThoiGianSuDung().toLocalDate() : LocalDate.now());
        TextField time = new TextField(edit ? current.getThoiGianSuDung().format(HOUR_MINUTE)
                : LocalTime.now().format(HOUR_MINUTE));
        time.setPromptText("HH:mm, ví dụ 14:30");
        TextField count = new TextField(edit ? String.valueOf(current.getSoLuong()) : "1");
        Label preview = new Label("Thành tiền dự kiến: —");
        preview.getStyleClass().add("svc-money-preview-value");

        // Giữ nguyên đơn giá hiệu dụng của giao dịch cũ nếu chỉ sửa số lượng/ngày/SV.
        // Nếu đổi sang dịch vụ khác thì áp dụng đơn giá hiện tại của dịch vụ mới.
        java.util.function.Supplier<BigDecimal> unitPrice = () -> {
            DichVu picked = dvBox.getValue();
            if (picked == null) return BigDecimal.ZERO;
            if (edit && picked.getMaDV().equals(current.getMaDV()) && current.getSoLuong() > 0) {
                return current.getThanhTien().divide(BigDecimal.valueOf(current.getSoLuong()), 10, RoundingMode.HALF_UP);
            }
            return picked.getDonGia();
        };
        Runnable previewMoney = () -> {
            try {
                int n = Integer.parseInt(count.getText().trim());
                preview.setText("Thành tiền dự kiến: " + money(unitPrice.get().multiply(BigDecimal.valueOf(n))));
            } catch (NumberFormatException e) { preview.setText("Thành tiền dự kiến: —"); }
        };
        count.textProperty().addListener((obs, a, b) -> previewMoney.run());
        dvBox.valueProperty().addListener((obs, a, b) -> previewMoney.run());
        previewMoney.run();

        GridPane grid = formGrid();
        grid.add(formField("Sinh viên", svBox, true), 0, 0, 2, 1);
        grid.add(formField("Dịch vụ", dvBox, true), 0, 1, 2, 1);
        grid.add(formField("Ngày sử dụng", day, true), 0, 2);
        grid.add(formField("Giờ (HH:mm)", time, true), 1, 2);
        grid.add(formField("Số lượng", count, true), 0, 3);
        VBox moneyPreview = new VBox(7, preview);
        moneyPreview.getStyleClass().add("svc-money-preview");
        moneyPreview.setAlignment(Pos.CENTER_LEFT);
        grid.add(moneyPreview, 1, 3);

        Label err = errorLabel();
        VBox content = dialogContent(
                dialogHeading(edit ? "✎" : "+",
                        edit ? "Cập nhật lượt sử dụng #" + current.getIdSuDung() : "Ghi nhận sử dụng dịch vụ",
                        "Theo dõi lịch sử sử dụng và chi phí của sinh viên."),
                hintBox("Thành tiền được tính từ đơn giá và số lượng; dữ liệu được lưu vào cơ sở dữ liệu."),
                sectionTitle("THÔNG TIN SỬ DỤNG"), grid, err);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(700);
        styleDialogButtons(dialog, save, cancel);
        Node saveBtn = dialog.getDialogPane().lookupButton(save);
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            String message = validateUsage(svBox, dvBox, day, time, count, unitPrice.get());
            if (message != null) { showFormError(err, message); ev.consume(); }
        });
        dialog.setResultConverter(btn -> {
            if (btn != save) return null;
            int qty = Integer.parseInt(count.getText().trim());
            BigDecimal total = unitPrice.get().multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            return new SuDungDichVu(edit ? current.getIdSuDung() : 0,
                    svBox.getValue().getMaSV(), dvBox.getValue().getMaDV(),
                    LocalDateTime.of(day.getValue(), LocalTime.parse(time.getText().trim(), HOUR_MINUTE)),
                    qty, total);
        });
        return dialog.showAndWait();
    }

    private String validateUsage(ComboBox<SinhVien> sv, ComboBox<DichVu> dv, DatePicker date,
                                 TextField hour, TextField quantity, BigDecimal unitPrice) {
        if (sv.getValue() == null || dv.getValue() == null || date.getValue() == null)
            return "Hãy chọn sinh viên, dịch vụ và ngày sử dụng.";
        try { LocalTime.parse(hour.getText().trim(), HOUR_MINUTE); }
        catch (DateTimeParseException ex) { return "Giờ phải có định dạng HH:mm (VD: 08:30)."; }
        int qty;
        try { qty = Integer.parseInt(quantity.getText().trim()); }
        catch (NumberFormatException ex) { return "Số lượng phải là số nguyên dương."; }
        if (qty <= 0) return "Số lượng phải lớn hơn 0.";
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        if (total.signum() < 0 || total.compareTo(new BigDecimal("9999999999.99")) > 0)
            return "Thành tiền không phù hợp DECIMAL(12,2).";
        return null;
    }

    @FXML private void handleRunReport() { runReport(); }
    private void runReport() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        if (from == null || to == null || to.isBefore(from)) {
            warn("Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.");
            return;
        }
        try {
            reports.setAll(suDungDAO.thongKeSuDung(from, to));
            BigDecimal total = reports.stream().map(ChiPhiDichVuRow::getTongTien)
                    .filter(n -> n != null).reduce(BigDecimal.ZERO, BigDecimal::add);
            lblReportCount.setText("Kết quả: " + reports.size() + " dòng (theo sinh viên và dịch vụ)");
            lblReportTotal.setText("Tổng cộng: " + money(total));
        } catch (SQLException ex) { sqlError("Không thực hiện được truy vấn 2", ex); }
    }
    @FXML private void handleNavigateRevenue() {
        if (onNavigate != null) onNavigate.accept("reports");
        else warn("Chưa kết nối điều hướng tới mục Thống kê trong MainController.");
    }

    // Chỉ trang trí Dialog; tất cả thao tác DAO, validation và truy vấn giữ nguyên.
    private void prepareDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("svc-dialog-pane");
        URL css = getClass().getResource("/com/ptit/quanlyktx/css/dichvu.css");
        if (css != null) pane.getStylesheets().add(css.toExternalForm());
        if (tabPane != null && tabPane.getScene() != null) {
            dialog.initOwner(tabPane.getScene().getWindow());
        }
        pane.setMinHeight(Region.USE_PREF_SIZE);
    }

    private void styleDialogButtons(Dialog<?> dialog, ButtonType primary, ButtonType secondary) {
        Node primaryButton = dialog.getDialogPane().lookupButton(primary);
        Node secondaryButton = dialog.getDialogPane().lookupButton(secondary);
        if (primaryButton != null) primaryButton.getStyleClass().add("svc-dialog-primary");
        if (secondaryButton != null) secondaryButton.getStyleClass().add("svc-dialog-secondary");
    }

    private VBox dialogContent(Node... children) {
        VBox content = new VBox(17);
        content.getStyleClass().add("svc-dialog-content");
        content.getChildren().addAll(children);
        return content;
    }

    private HBox dialogHeading(String mark, String title, String subtitle) {
        Label symbol = new Label(mark);
        symbol.getStyleClass().add("svc-dialog-symbol");
        VBox titles = new VBox(5);
        Label heading = new Label(title);
        heading.getStyleClass().add("svc-dialog-title");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("svc-dialog-subtitle");
        sub.setWrapText(true);
        titles.getChildren().addAll(heading, sub);
        HBox header = new HBox(14, symbol, titles);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("svc-dialog-header");
        return header;
    }

    private HBox hintBox(String message) {
        Label icon = new Label("i");
        icon.getStyleClass().add("svc-hint-icon");
        Label text = new Label(message);
        text.getStyleClass().add("svc-hint-text");
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox box = new HBox(10, icon, text);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("svc-dialog-hint");
        return box;
    }

    private Label sectionTitle(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("svc-form-section");
        return label;
    }

//    private GridPane formGrid() {
//        GridPane grid = new GridPane();
//        grid.setPadding(
//            new javafx.geometry.Insets(0, 20, 0, 20)
//        );
//        grid.setHgap(20);
//        grid.setVgap(15);
//        grid.getStyleClass().add("svc-form-grid");
//        ColumnConstraints left = new ColumnConstraints();
//        left.setPercentWidth(50);
//        left.setHgrow(Priority.ALWAYS);
//        ColumnConstraints right = new ColumnConstraints();
//        right.setPercentWidth(50);
//        right.setHgrow(Priority.ALWAYS);
//        grid.getColumnConstraints().addAll(left, right);
//        return grid;
//    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();

        grid.setPadding(new Insets(0, 20, 0, 20));
        grid.setHgap(20);
        grid.setVgap(15);

        grid.getStyleClass().add("svc-form-grid");

        ColumnConstraints left = new ColumnConstraints();
        left.setHgrow(Priority.ALWAYS);
        left.setFillWidth(true);

        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        right.setFillWidth(true);

        // Không dùng setPercentWidth(50)
        // để tránh xung đột với khoảng cách giữa các cột
        grid.getColumnConstraints().addAll(left, right);

        return grid;
    }

//    private VBox formField(String title, Control control, boolean required) {
//        Label label = new Label(title + (required ? " *" : ""));
//        label.getStyleClass().add("svc-form-label");
//        control.getStyleClass().add("svc-form-input");
//        control.setMaxWidth(Double.MAX_VALUE);
//        control.setPrefHeight(44);
//        VBox group = new VBox(7, label, control);
//        group.setMaxWidth(Double.MAX_VALUE);
//        group.getStyleClass().add("svc-form-group");
//        return group;
//    }

    private VBox formField(String title, Control control, boolean required) {
        Label label = new Label(title + (required ? " *" : ""));
        label.getStyleClass().add("svc-form-label");

        control.getStyleClass().add("svc-form-input");

        control.setMinWidth(0);
        control.setPrefWidth(0);
        control.setMaxWidth(Double.MAX_VALUE);
        control.setPrefHeight(44);

        VBox group = new VBox(7, label, control);
        group.setMinWidth(0);
        group.setPrefWidth(0);
        group.setMaxWidth(Double.MAX_VALUE);

        group.getStyleClass().add("svc-form-group");

        GridPane.setHgrow(group, Priority.ALWAYS);
        GridPane.setFillWidth(group, true);

        return group;
    }

    
    
    
    private Label errorLabel() {
        Label err = new Label();
        err.getStyleClass().add("svc-form-error");
        err.setWrapText(true);
        err.setVisible(false);
        err.setManaged(false);
        return err;
    }

    private void showFormError(Label err, String message) {
        err.setText(message);
        err.setVisible(true);
        err.setManaged(true);
    }

    private VBox messageContent(Alert.AlertType type, String headline, String message) {
        Label symbol = new Label(switch (type) {
            case ERROR -> "×";
            case WARNING -> "!";
            case CONFIRMATION -> "?";
            default -> "✓";
        });
        symbol.getStyleClass().addAll("svc-msg-symbol", switch (type) {
            case ERROR -> "svc-msg-error";
            case WARNING -> "svc-msg-warning";
            case CONFIRMATION -> "svc-msg-question";
            default -> "svc-msg-success";
        });
        Label title = new Label(headline);
        title.getStyleClass().add("svc-msg-title");
        Label detail = new Label(message);
        detail.getStyleClass().add("svc-msg-detail");
        detail.setWrapText(true);
        detail.setMaxWidth(Double.MAX_VALUE);
        detail.setMinHeight(Region.USE_PREF_SIZE);
        VBox text = new VBox(8, title, detail);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox top = new HBox(15, symbol, text);
        top.setAlignment(Pos.TOP_LEFT);
        VBox box = new VBox(top);
        box.getStyleClass().add("svc-message-content");
        return box;
    }

    private boolean confirm(String message) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Xác nhận xóa");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setGraphic(null);
        prepareDialog(dialog);
        ButtonType delete = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(delete, cancel);
        dialog.getDialogPane().setContent(messageContent(Alert.AlertType.CONFIRMATION,
                "Xác nhận xóa dữ liệu", message));
        dialog.getDialogPane().setPrefWidth(565);
        styleDialogButtons(dialog, delete, cancel);
        dialog.getDialogPane().lookupButton(delete).getStyleClass().remove("svc-dialog-primary");
        dialog.getDialogPane().lookupButton(delete).getStyleClass().add("svc-dialog-danger");
        return dialog.showAndWait().orElse(cancel) == delete;
    }

    private void showNotice(Alert.AlertType type, String headline, String message) {
        Alert dialog = new Alert(type);
        dialog.setTitle("Quản lý dịch vụ");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setGraphic(null);
        prepareDialog(dialog);
        ButtonType close = new ButtonType("Đã hiểu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(close);
        dialog.getDialogPane().setContent(messageContent(type, headline, message));
        dialog.getDialogPane().setPrefWidth(565);
        styleDialogButtons(dialog, close, close);
        // Với một nút duy nhất, giữ kiểu nút chính.
        Node btn = dialog.getDialogPane().lookupButton(close);
        btn.getStyleClass().remove("svc-dialog-secondary");
        btn.getStyleClass().add("svc-dialog-primary");
        dialog.showAndWait();
    }

    private void info(String msg) {
        showNotice(Alert.AlertType.INFORMATION, "Thành công", msg);
    }
    private void warn(String msg) {
        showNotice(Alert.AlertType.WARNING, "Cần kiểm tra thông tin", msg);
    }
    private void sqlError(String title, SQLException ex) {
        String explanation = ex.getSQLState() != null && ex.getSQLState().startsWith("23")
                ? "Có thể trùng khóa, sai khóa ngoại hoặc dữ liệu đang được tham chiếu.\n" : "";
        String detail = explanation + (ex.getMessage() == null ? "Vui lòng thử lại." : ex.getMessage());
        showNotice(Alert.AlertType.ERROR, title, detail);
        ex.printStackTrace();
    }
}
