package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.dao.ThongKeDAO;
import com.ptit.quanlyktx.dto.DoanhThuDichVuRow;
import com.ptit.quanlyktx.dto.TopSinhVienDichVuRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

/** Màn hình thống kê doanh thu, không tạo/sửa/xóa CSDL. */
public class ThongKeController {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter THANG_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM").withResolverStyle(ResolverStyle.STRICT);
    private static final String[] MAU_COT = {
        "#2580eb", "#8051e7", "#fb702b", "#02ae72", "#f24455",
        "#12a7be", "#b7791f", "#6474da", "#d8569a", "#609b41"
    };

    @FXML private TextField txtThang;
    @FXML private Label lblTongDoanhThu, lblKyDoanhThu, lblTongLuot,
            lblDvCoDoanhThu, lblThangDaXem, lblDemDv,
            lblSoDong, lblTongLuotBang, lblTongBang,
            lblBieuDoRong, lblBieuDoTrong, lblTrangThai;

    @FXML private TableView<DoanhThuDichVuRow> tblDoanhThu;
    @FXML private TableColumn<DoanhThuDichVuRow, String> colMaDv, colTenDv;
    @FXML private TableColumn<DoanhThuDichVuRow, BigDecimal> colDonGia, colSoLuong, colDoanhThu;
    @FXML private TableColumn<DoanhThuDichVuRow, Integer> colSoLuot;

    @FXML private ComboBox<String> cboPhamVi;
    @FXML private ScrollPane scrollBieuDo;
    @FXML private BarChart<Number, String> chartDoanhThu;
    @FXML private NumberAxis axisTien;
    @FXML private CategoryAxis axisDichVu;

    // Bảng xếp hạng bổ sung, độc lập với bảng doanh thu và biểu đồ hiện tại.
    @FXML private ComboBox<String> cboTopGioiHan, cboTopTieuChi;
    @FXML private Label lblTopKy, lblTopMoTa, lblTopSoDong, lblTopTongTien, lblTopTrangThai;
    @FXML private TableView<TopSinhVienDichVuRow> tblTopSinhVien;
    @FXML private TableColumn<TopSinhVienDichVuRow, Integer> colTopThuHang, colTopSoLuot;
    @FXML private TableColumn<TopSinhVienDichVuRow, String> colTopMaSv, colTopHoTen;
    @FXML private TableColumn<TopSinhVienDichVuRow, BigDecimal> colTopTongTien;

    private final ThongKeDAO dao = new ThongKeDAO();
    private final List<DoanhThuDichVuRow> duLieu = new ArrayList<>();
    private YearMonth thangDangXem;

    @FXML
    private void initialize() {
        thietLapBang();
        thietLapBangTopSinhVien();
        cboPhamVi.getItems().setAll("Top 5", "Top 10", "Tất cả");
        cboPhamVi.getSelectionModel().select("Top 5");
        cboPhamVi.valueProperty().addListener((obs, cu, moi) -> capNhatBieuDo());

        // Tab mới chỉ có hai mức Top 5/Top 10, không thay đổi cboPhamVi của biểu đồ.
        cboTopGioiHan.getItems().setAll("Top 5", "Top 10");
        cboTopGioiHan.getSelectionModel().select("Top 5");
        cboTopTieuChi.getItems().setAll("Tổng tiền dịch vụ", "Số lượt sử dụng");
        cboTopTieuChi.getSelectionModel().select("Tổng tiền dịch vụ");
        cboTopGioiHan.valueProperty().addListener((obs, cu, moi) -> capNhatTopSinhVien());
        cboTopTieuChi.valueProperty().addListener((obs, cu, moi) -> capNhatTopSinhVien());

        chartDoanhThu.setAnimated(false);
        chartDoanhThu.setLegendVisible(false);
        axisTien.setForceZeroInRange(true);
        axisTien.setTickLabelFormatter(new StringConverter<Number>() {
            @Override public String toString(Number n) {
                if (n == null) return "";
                double v = n.doubleValue();
                if (Math.abs(v) >= 1_000_000) return gonSo(v / 1_000_000) + "tr";
                if (Math.abs(v) >= 1_000) return gonSo(v / 1_000) + "k";
                return String.valueOf(Math.round(v));
            }
            @Override public Number fromString(String s) { return 0; }
        });

        // Khi mở lần đầu, ưu tiên tháng có giao dịch gần nhất cho dễ kiểm tra.
        YearMonth khoiDau = YearMonth.now();
        try {
            YearMonth ganNhat = dao.findThangGanNhatCoSuDung();
            if (ganNhat != null) khoiDau = ganNhat;
        } catch (SQLException ex) {
            // Lệnh Tra cứu sẽ hiển thị thông báo nếu không đọc được dữ liệu SQLite.
        }
        txtThang.setText(khoiDau.toString());
        traCuu();
    }

    private void thietLapBang() {
        colMaDv.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().maDv()));
        colTenDv.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().tenDv()));
        colDonGia.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().donGia()));
        colSoLuot.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().soLuotSuDung()));
        colSoLuong.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().tongSoLuong()));
        colDoanhThu.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().tongDoanhThu()));

        colDonGia.setCellFactory(c -> taoOThapPhan(false, true));
        colSoLuong.setCellFactory(c -> taoOThapPhan(true, false));
        colDoanhThu.setCellFactory(c -> taoOThapPhan(false, true));
        colSoLuot.setCellFactory(c -> new TableCell<>() {
            { getStyleClass().add("number-cell"); }
            @Override protected void updateItem(Integer so, boolean empty) {
                super.updateItem(so, empty);
                setText(empty || so == null ? null : so.toString());
            }
        });

        // Các cột số giữ gọn, chỉ cột Tên dịch vụ giãn theo phần rộng còn lại.
        tblDoanhThu.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tblDoanhThu.widthProperty().addListener((obs, cu, moi) ->
                colTenDv.setPrefWidth(Math.max(230, moi.doubleValue() - 660)));
        tblDoanhThu.setPlaceholder(new Label("Danh mục dịch vụ đang trống"));
    }

    /** Các ô số vẫn giữ kiểu BigDecimal để sắp xếp theo giá trị thật. */
    private TableCell<DoanhThuDichVuRow, BigDecimal> taoOThapPhan(boolean kemDonVi, boolean laTien) {
        return new TableCell<>() {
            { getStyleClass().add(laTien ? "money-cell" : "number-cell"); }
            @Override protected void updateItem(BigDecimal so, boolean empty) {
                super.updateItem(so, empty);
                if (empty || so == null) { setText(null); return; }
                String chuoi = formatSo(so);
                if (kemDonVi) {
                    DoanhThuDichVuRow dong = getTableRow() == null ? null : getTableRow().getItem();
                    if (dong != null) chuoi += " " + dong.donViTinh();
                } else if (laTien) {
                    chuoi += " đ";
                }
                setText(chuoi);
            }
        };
    }

    @FXML
    private void traCuu() {
        final YearMonth thang;
        try {
            thang = YearMonth.parse(txtThang.getText().trim(), THANG_FORMAT);
        } catch (DateTimeParseException | NullPointerException ex) {
            thongBao(Alert.AlertType.WARNING, "Tháng không hợp lệ",
                    "Hãy nhập tháng theo định dạng YYYY-MM, ví dụ: 2026-10.");
            return;
        }
        try {
            List<DoanhThuDichVuRow> moi = dao.findDoanhThuTheoThang(thang);
            duLieu.clear();
            duLieu.addAll(moi);
            thangDangXem = thang;
            tblDoanhThu.setItems(FXCollections.observableArrayList(moi));
            // Trả bảng về thứ tự doanh thu giảm dần sau mỗi lần tra cứu.
            tblDoanhThu.getSortOrder().clear();
            capNhatThongSo();
            capNhatBieuDo();
            lblTrangThai.setText("Đã tải dữ liệu cho tháng " + thang);
            // Truy vấn độc lập: lỗi ở bảng Top không làm mất dữ liệu 2 tab cũ.
            capNhatTopSinhVien();
        } catch (SQLException ex) {
            lblTrangThai.setText("Không thể cập nhật dữ liệu. Vui lòng kiểm tra kết nối SQLite và file CSDL.");
            thongBao(Alert.AlertType.ERROR, "Không thể truy vấn SQLite", ex.getMessage());
        }
    }

    @FXML
    private void lamMoi() {
        // Đọc lại chính tháng đang nhập, kể cả khi DB đã thay đổi từ bên ngoài.
        traCuu();
    }

    private void capNhatThongSo() {
        BigDecimal tien = duLieu.stream().map(DoanhThuDichVuRow::tongDoanhThu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int luot = duLieu.stream().mapToInt(DoanhThuDichVuRow::soLuotSuDung).sum();
        long coDoanhThu = duLieu.stream()
                .filter(r -> r.tongDoanhThu().signum() > 0).count();

        lblTongDoanhThu.setText(formatTien(tien));
        lblKyDoanhThu.setText("Tháng " + thangDangXem);
        lblTongLuot.setText(formatSo(BigDecimal.valueOf(luot)));
        lblDvCoDoanhThu.setText(coDoanhThu + " / " + duLieu.size());
        lblThangDaXem.setText("Đang xem: " + thangDangXem);
        lblDemDv.setText(duLieu.size() + " dịch vụ");
        lblSoDong.setText("Hiển thị: " + duLieu.size() + " / " + duLieu.size() + " dịch vụ");
        lblTongLuotBang.setText(formatSo(BigDecimal.valueOf(luot)));
        lblTongBang.setText(formatTien(tien));
    }

    private void capNhatBieuDo() {
        if (chartDoanhThu == null || cboPhamVi == null || thangDangXem == null) return;
        List<DoanhThuDichVuRow> theoDoanhThu = new ArrayList<>(duLieu);
        theoDoanhThu.sort((a, b) -> {
            int t = b.tongDoanhThu().compareTo(a.tongDoanhThu());
            return t != 0 ? t : a.maDv().compareTo(b.maDv());
        });
        String phamVi = cboPhamVi.getValue();
        int gioiHan = "Top 5".equals(phamVi) ? 5 : "Top 10".equals(phamVi) ? 10 : Integer.MAX_VALUE;
        List<DoanhThuDichVuRow> hienThi = theoDoanhThu.stream().limit(gioiHan)
                .collect(Collectors.toList());
        boolean coDoanhThu = duLieu.stream().anyMatch(r -> r.tongDoanhThu().signum() > 0);
        boolean coDuLieu = !hienThi.isEmpty() && coDoanhThu;

        scrollBieuDo.setVisible(coDuLieu);
        scrollBieuDo.setManaged(coDuLieu);
        lblBieuDoTrong.setVisible(!coDuLieu);
        lblBieuDoTrong.setManaged(!coDuLieu);
        if (!coDuLieu) {
            chartDoanhThu.getData().clear();
            lblBieuDoRong.setText("Không có doanh thu phát sinh trong tháng " + thangDangXem);
            return;
        }

        lblBieuDoRong.setText("Hiển thị " + hienThi.size() + "/" + duLieu.size()
                + " dịch vụ · Tháng " + thangDangXem
                + (duLieu.size() > hienThi.size() ? " · Chọn 'Tất cả' để xem đầy đủ" : ""));

        // Trục danh mục được sắp ngược để doanh thu lớn nhất ở trên cùng.
        List<String> nhan = new ArrayList<>();
        for (DoanhThuDichVuRow r : hienThi) nhan.add(r.tenDv() + " · " + r.maDv());
        Collections.reverse(nhan);
        axisDichVu.setAutoRanging(false);
        axisDichVu.setCategories(FXCollections.observableArrayList(nhan));

        XYChart.Series<Number, String> series = new XYChart.Series<>();
        for (int i = 0; i < hienThi.size(); i++) {
            DoanhThuDichVuRow r = hienThi.get(i);
            String category = r.tenDv() + " · " + r.maDv();
            XYChart.Data<Number, String> data = new XYChart.Data<>(r.tongDoanhThu(), category);
            String color = MAU_COT[i % MAU_COT.length];
            String tooltip = r.tenDv() + " (" + r.maDv() + ")\n"
                    + "Lượt sử dụng: " + r.soLuotSuDung() + "\n"
                    + "Tổng số lượng: " + formatSo(r.tongSoLuong()) + " " + r.donViTinh() + "\n"
                    + "Doanh thu: " + formatTien(r.tongDoanhThu());
            data.nodeProperty().addListener((obs, cu, node) -> toMauNode(node, color, tooltip));
            series.getData().add(data);
        }
        chartDoanhThu.getData().setAll(series);
        // Biểu đồ cao theo số dòng; ScrollPane giữ khu vực tab không bị kéo dài.
        double chieuCao = Math.max(370, hienThi.size() * 48.0 + 105);
        chartDoanhThu.setMinHeight(chieuCao);
        chartDoanhThu.setPrefHeight(chieuCao);
        scrollBieuDo.setVvalue(0);

    }

    /** Cấu hình bảng xếp hạng riêng; cột không tự sắp xếp để giữ đúng thứ hạng SQL. */
    private void thietLapBangTopSinhVien() {
        colTopThuHang.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().thuHang()));
        colTopMaSv.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().maSv()));
        colTopHoTen.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().hoTen()));
        colTopSoLuot.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().soLuot()));
        colTopTongTien.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().tongTien()));

        colTopThuHang.setCellFactory(c -> new TableCell<>() {
            { getStyleClass().add("tk-rank-cell"); }
            @Override protected void updateItem(Integer thuHang, boolean empty) {
                super.updateItem(thuHang, empty);
                setText(empty || thuHang == null ? null : String.format("%02d", thuHang));
            }
        });
        colTopSoLuot.setCellFactory(c -> new TableCell<>() {
            { getStyleClass().add("number-cell"); }
            @Override protected void updateItem(Integer soLuot, boolean empty) {
                super.updateItem(soLuot, empty);
                setText(empty || soLuot == null ? null : soLuot + " lượt");
            }
        });
        colTopTongTien.setCellFactory(c -> new TableCell<>() {
            { getStyleClass().add("money-cell"); }
            @Override protected void updateItem(BigDecimal soTien, boolean empty) {
                super.updateItem(soTien, empty);
                setText(empty || soTien == null ? null : formatTien(soTien));
            }
        });
        tblTopSinhVien.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (TableColumn<TopSinhVienDichVuRow, ?> col : tblTopSinhVien.getColumns()) {
            col.setSortable(false);
            col.setReorderable(false);
        }
        tblTopSinhVien.setPlaceholder(new Label("Tháng này chưa có sinh viên sử dụng dịch vụ."));
    }

    /** Chỉ đọc thêm thống kê Top; không can thiệp vào bảng và biểu đồ doanh thu. */
    private void capNhatTopSinhVien() {
        if (thangDangXem == null) return;
        int gioiHan = "Top 10".equals(cboTopGioiHan.getValue()) ? 10 : 5;
        boolean theoTien = !"Số lượt sử dụng".equals(cboTopTieuChi.getValue());
        lblTopKy.setText("Tháng " + thangDangXem);
        lblTopMoTa.setText("Xếp hạng theo " + (theoTien ? "tổng tiền dịch vụ" : "số lượt sử dụng")
                + " · " + cboTopGioiHan.getValue() + " · Tháng " + thangDangXem);
        try {
            List<TopSinhVienDichVuRow> rows = dao.findTopSinhVienTheoThang(thangDangXem, gioiHan, theoTien);
            tblTopSinhVien.setItems(FXCollections.observableArrayList(rows));
            BigDecimal tongTienTop = rows.stream().map(TopSinhVienDichVuRow::tongTien)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblTopSoDong.setText("Hiển thị: " + rows.size() + " sinh viên · " + cboTopGioiHan.getValue());
            lblTopTongTien.setText(formatTien(tongTienTop));
            lblTopTrangThai.setText(rows.isEmpty()
                    ? "Không có giao dịch dịch vụ trong tháng được chọn."
                    : "Thống kê những sinh viên có lượt sử dụng hoặc chi phí dịch vụ cao nhất tháng.");
        } catch (SQLException ex) {
            // Lỗi riêng của truy vấn mới không làm thay đổi dữ liệu bảng/biểu đồ cũ.
            tblTopSinhVien.getItems().clear();
            lblTopSoDong.setText("Không tải được bảng xếp hạng");
            lblTopTongTien.setText("—");
            lblTopTrangThai.setText("Không thể đọc thống kê Top sinh viên: " + ex.getMessage());
        }
    }

    private static void toMauNode(Node node, String color, String tooltip) {
        if (node != null) {
            node.setStyle("-fx-bar-fill: " + color + ";");
            Tooltip.install(node, new Tooltip(tooltip));
        }
    }

    private static String gonSo(double v) {
        return v == Math.rint(v) ? Long.toString(Math.round(v))
                : String.format(Locale.US, "%.1f", v);
    }

    private static String formatSo(BigDecimal n) {
        if (n == null) return "0";
        NumberFormat nf = NumberFormat.getNumberInstance(VI);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(2);
        return nf.format(n);
    }

    private static String formatTien(BigDecimal n) {
        if (n == null) return "0 đ";
        return formatSo(n.setScale(0, RoundingMode.HALF_UP)) + " đ";
    }

    private static void thongBao(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "Không có thông tin chi tiết" : message);
        alert.showAndWait();
    }
}
