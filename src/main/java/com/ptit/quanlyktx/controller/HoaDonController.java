package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.dao.HoaDonDAO;
import com.ptit.quanlyktx.dao.SinhVienDAO;
import com.ptit.quanlyktx.dto.HoaDonRow;
import com.ptit.quanlyktx.model.SinhVien;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Màn hình truy vấn 1: tổng tiền KTX theo tháng, in danh sách hoặc từng người. */
public class HoaDonController {

    private static final String ALL = "Tất cả sinh viên";
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final SinhVienDAO sinhVienDAO = new SinhVienDAO();
    private final ObservableList<HoaDonRow> bills = FXCollections.observableArrayList();
    private final Map<String, String> studentIdsByLabel = new LinkedHashMap<>();
    private YearMonth lastLoadedMonth;

    @FXML private TextField txtThang;
    @FXML private ComboBox<String> cmbSinhVien;
    @FXML private TableView<HoaDonRow> tableBills;
    @FXML private TableColumn<HoaDonRow, String> colMaSV;
    @FXML private TableColumn<HoaDonRow, String> colHoTen;
    @FXML private TableColumn<HoaDonRow, String> colPhong;
    @FXML private TableColumn<HoaDonRow, String> colTienPhong;
    @FXML private TableColumn<HoaDonRow, String> colTienDichVu;
    @FXML private TableColumn<HoaDonRow, String> colTienVeXe;
    @FXML private TableColumn<HoaDonRow, String> colTienPhatSinh;
    @FXML private TableColumn<HoaDonRow, String> colTongTien;
    @FXML private Label lblSoSinhVien;
    @FXML private Label lblTongTien;
    @FXML private Label lblCount;

    @FXML
    private void initialize() {
        colMaSV.setCellValueFactory(c -> str(c.getValue().getMaSV()));
        colHoTen.setCellValueFactory(c -> str(c.getValue().getHoTen()));
        colPhong.setCellValueFactory(c -> str(c.getValue().getSoPhong()));
        colTienPhong.setCellValueFactory(c -> str(money(c.getValue().getTienPhong())));
        colTienDichVu.setCellValueFactory(c -> str(money(c.getValue().getTienDichVu())));
        colTienVeXe.setCellValueFactory(c -> str(money(c.getValue().getTienVeXeThang())));
        colTienPhatSinh.setCellValueFactory(c -> str(money(c.getValue().getTienGuiXePhatSinh())));
        colTongTien.setCellValueFactory(c -> str(money(c.getValue().getTongPhaiTra())));

        tableBills.setItems(bills);
        txtThang.setText(YearMonth.now().toString());
        refreshStudentChoices();
        loadFromDatabase();
    }

    private ReadOnlyStringWrapper str(String s) {
        return new ReadOnlyStringWrapper(s == null ? "" : s);
    }

    private String money(BigDecimal amount) {
        NumberFormat fmt = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
        fmt.setMaximumFractionDigits(2);
        return fmt.format(amount == null ? BigDecimal.ZERO : amount) + " đ";
    }

    private void refreshStudentChoices() {
        String previouslySelected = studentIdsByLabel.get(cmbSinhVien.getValue());
        studentIdsByLabel.clear();
        studentIdsByLabel.put(ALL, null);
        try {
            for (SinhVien sv : sinhVienDAO.findAll()) {
                studentIdsByLabel.put(sv.getMaSV() + " - " + sv.getHoTen(), sv.getMaSV());
            }
            cmbSinhVien.getItems().setAll(studentIdsByLabel.keySet());
            String selectedLabel = ALL;
            if (previouslySelected != null) {
                for (Map.Entry<String, String> e : studentIdsByLabel.entrySet()) {
                    if (previouslySelected.equals(e.getValue())) {
                        selectedLabel = e.getKey();
                        break;
                    }
                }
            }
            cmbSinhVien.setValue(selectedLabel);
        } catch (SQLException ex) {
            cmbSinhVien.getItems().setAll(ALL);
            cmbSinhVien.setValue(ALL);
            showError("Không tải được danh sách sinh viên", ex.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        loadFromDatabase();
    }

    @FXML
    private void handleRefresh() {
        refreshStudentChoices();
        loadFromDatabase();
    }

    private void loadFromDatabase() {
        YearMonth month;
        try {
            month = YearMonth.parse(txtThang.getText().trim());
        } catch (DateTimeParseException | NullPointerException ex) {
            showError("Tháng không hợp lệ", "Hãy nhập tháng theo dạng YYYY-MM, ví dụ 2026-11.");
            return;
        }

        String maSV = studentIdsByLabel.get(cmbSinhVien.getValue());
        try {
            List<HoaDonRow> fresh = hoaDonDAO.findByMonth(month, maSV);
            bills.setAll(fresh);
            lastLoadedMonth = month;
            tableBills.getSelectionModel().clearSelection();
            BigDecimal total = bills.stream().map(HoaDonRow::getTongPhaiTra)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblSoSinhVien.setText(String.valueOf(bills.size()));
            lblTongTien.setText(money(total));
            lblCount.setText("Hiển thị: " + bills.size() + " sinh viên · Tháng " + month);
        } catch (SQLException ex) {
            // Giữ lại kết quả cũ, không hiển thị số 0 giả khi lỗi DB.
            showError("Không thể truy vấn hóa đơn từ MySQL", ex.getMessage());
        }
    }

    @FXML
    private void handlePrintAll() {
        if (bills.isEmpty()) {
            showWarning("Không có bản ghi để in. Hãy nhấn Tra cứu trước.");
            return;
        }
        printBills(new ArrayList<>(bills), false);
    }

    @FXML
    private void handlePrintOne() {
        HoaDonRow selected = tableBills.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Hãy chọn một sinh viên trong bảng trước khi in.");
            return;
        }
        printBills(List.of(selected), true);
    }

    private void printBills(List<HoaDonRow> data, boolean personal) {
        if (lastLoadedMonth == null) {
            showWarning("Hãy tra cứu dữ liệu trước khi in.");
            return;
        }
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showWarning("Không tìm thấy máy in. Có thể chọn 'Microsoft Print to PDF' trên Windows.");
            return;
        }
        if (!job.showPrintDialog(tableBills.getScene().getWindow())) return;

        // Mỗi trang in tối đa 12 SV; tránh tràn trang đối với danh sách dài.
        final int pageSize = personal ? 1 : 12;
        for (int start = 0; start < data.size(); start += pageSize) {
            int end = Math.min(start + pageSize, data.size());
            VBox paper = new VBox(9);
            paper.setPadding(new Insets(18));
            Text title = new Text(personal ? "PHIẾU TỔNG HỢP THANH TOÁN" : "DANH SÁCH TIỀN KTX PHẢI TRẢ");
            title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            paper.getChildren().add(title);
            paper.getChildren().add(line("Tháng: " + lastLoadedMonth + " | PTIT KTX", true));
            paper.getChildren().add(line("Phí phòng, dịch vụ, vé xe tháng và phí phát sinh", false));
            paper.getChildren().add(line("------------------------------------------------------------", false));

            for (HoaDonRow row : data.subList(start, end)) {
                paper.getChildren().add(line(row.getMaSV() + " - " + row.getHoTen()
                    + " | Lớp: " + row.getLop() + " | Phòng: " + row.getSoPhong(), true));
                paper.getChildren().add(line("Phòng: " + money(row.getTienPhong())
                    + " | Dịch vụ: " + money(row.getTienDichVu()), false));
                paper.getChildren().add(line("Vé tháng: " + money(row.getTienVeXeThang())
                    + " | Phí phát sinh: " + money(row.getTienGuiXePhatSinh()), false));
                paper.getChildren().add(line("TỔNG PHẢI TRẢ: " + money(row.getTongPhaiTra()), true));
                paper.getChildren().add(line("", false));
            }
            if (!job.printPage(paper)) {
                job.cancelJob();
                showWarning("In chưa hoàn tất. Kiểm tra lại máy in hoặc khổ giấy.");
                return;
            }
        }
        job.endJob();
    }

    private Text line(String value, boolean bold) {
        Text t = new Text(value);
        t.setFont(Font.font("Arial", bold ? FontWeight.BOLD : FontWeight.NORMAL, 11));
        return t;
    }

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).showAndWait();
    }

    private void showError(String title, String message) {
        Alert dialog = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        dialog.setTitle("Quản lý hóa đơn");
        dialog.setHeaderText(title);
        dialog.showAndWait();
    }
}
