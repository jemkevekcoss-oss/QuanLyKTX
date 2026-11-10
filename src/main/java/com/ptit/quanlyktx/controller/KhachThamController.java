package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.dao.*;
import com.ptit.quanlyktx.dto.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import java.sql.SQLException;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.function.Function;

/** Controller độc lập cho KhachThamView.fxml (JavaFX 21+). */
public class KhachThamController {
    @FXML private Label lblTongLuot, lblLuotThang, lblKhachThang, lblDangTham, lblKhachCount, lblLuotCount, lblThongKe;
    @FXML private TextField txtTimKhach, txtTimLuot, txtThang;
    @FXML private TableView<KhachRow> tblKhach;
    @FXML private TableColumn<KhachRow,Number> colKhachId;
    @FXML private TableColumn<KhachRow,String> colCccd, colKhachTen, colKhachSinh;
    @FXML private TableView<LuotDenThamRow> tblLuot;
    @FXML private TableColumn<LuotDenThamRow,Number> colLuotId;
    @FXML private TableColumn<LuotDenThamRow,String> colLuotKhach,colLuotCccd,colLuotSv,colLuotDen,colLuotDi,colLuotStatus;
    @FXML private ComboBox<KhachRow> cboKhach;
    @FXML private ComboBox<SinhVienOption> cboSinhVien;
    @FXML private TableView<ThongKeKhachRow> tblThongKe;
    @FXML private TableColumn<ThongKeKhachRow,String> colTkSv,colTkTenSv,colTkLop,colTkPhong,colTkCccd,colTkTenKhach,colTkNgaySinh,colTkNgayGan;
    @FXML private TableColumn<ThongKeKhachRow,Number> colTkSoLan;

    private final KhachThamDAO khachDao = new KhachThamDAO();
    private final LuotDenThamDAO luotDao = new LuotDenThamDAO();
    private final ThongKeKhachDAO thongKeDao = new ThongKeKhachDAO();
    private final ObservableList<KhachRow> khachData = FXCollections.observableArrayList();
    private final ObservableList<LuotDenThamRow> luotData = FXCollections.observableArrayList();
    private final FilteredList<KhachRow> filteredKhach = new FilteredList<>(khachData, k -> true);
    private final FilteredList<LuotDenThamRow> filteredLuot = new FilteredList<>(luotData, k -> true);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter EDIT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private void initialize() {
        column(colKhachId,KhachRow::idKhach);
        column(colCccd,KhachRow::soCmt);
        column(colKhachTen,KhachRow::hoTen);
        column(colKhachSinh,k -> fmt(k.ngaySinh()));
        column(colLuotId,LuotDenThamRow::idLuot);
        column(colLuotKhach,LuotDenThamRow::tenKhach);
        column(colLuotCccd,LuotDenThamRow::soCmt);
        column(colLuotSv,v -> v.maSv()+" — "+v.tenSv());
        column(colLuotDen,v -> v.thoiGianDen()==null ? fmt(v.ngayDen())+" (chưa rõ giờ)" : DATETIME_FMT.format(v.thoiGianDen()));
        column(colLuotDi,v -> v.thoiGianDi()==null ? "—" : DATETIME_FMT.format(v.thoiGianDi()));
        column(colLuotStatus,LuotDenThamRow::trangThai);
        column(colTkSv,ThongKeKhachRow::maSv);
        column(colTkTenSv,ThongKeKhachRow::tenSv);
        column(colTkLop,ThongKeKhachRow::lop);
        column(colTkPhong,ThongKeKhachRow::soPhong);
        column(colTkCccd,ThongKeKhachRow::soCmt);
        column(colTkTenKhach,ThongKeKhachRow::tenKhach);
        column(colTkNgaySinh,v -> fmt(v.ngaySinh()));
        column(colTkNgayGan,v -> fmt(v.ngayDenGanNhat()));
        column(colTkSoLan,ThongKeKhachRow::soLanDen);
        // Danh ba co 4 cot: co gian cot theo be rong bang, khong de cot trong ben phai.
        tblKhach.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblKhach.setItems(filteredKhach);
        tblLuot.setItems(filteredLuot);
        // To mau trang thai mot cach nhe nhang, khong can them cot DTO.
        colLuotStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("kt-status-active", "kt-status-closed", "kt-status-unknown");
                setText(empty ? null : value);
                if (!empty && value != null) {
                    if ("Đang thăm".equals(value)) getStyleClass().add("kt-status-active");
                    else if ("Đã rời KTX".equals(value)) getStyleClass().add("kt-status-closed");
                    else getStyleClass().add("kt-status-unknown");
                }
            }
        });
        txtTimKhach.textProperty().addListener((obs,oldText,newText) -> filterKhach());
        txtTimLuot.textProperty().addListener((obs,oldText,newText) -> filterLuot());
        txtThang.setText(YearMonth.now().toString());
        reloadAll();
    }
    private static <T,V> void column(TableColumn<T,V> c, Function<T,V> f) {
        c.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(f.apply(x.getValue())));
    }
    private static String fmt(LocalDate d) { return d==null ? "—" : DATE_FMT.format(d); }
    private static String norm(String s) { return s==null?"":s.trim().toLowerCase(Locale.ROOT); }
    private static void error(String title, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Lỗi"); a.setHeaderText(title); a.setContentText(e.getMessage()); a.showAndWait();
    }
    private static void message(String s) {
        Alert a = new Alert(Alert.AlertType.INFORMATION,s,ButtonType.OK); a.setHeaderText(null); a.showAndWait();
    }
    private static boolean confirm(String s) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,s,ButtonType.YES,ButtonType.NO);
        a.setHeaderText("Xác nhận thao tác"); return a.showAndWait().orElse(ButtonType.NO)==ButtonType.YES;
    }
    private void reloadAll() {
        try {
            Integer selectedGuest = tblKhach.getSelectionModel().getSelectedItem()==null ? null : tblKhach.getSelectionModel().getSelectedItem().idKhach();
            khachData.setAll(khachDao.findAll());
            luotData.setAll(luotDao.findAll());
            cboKhach.setItems(FXCollections.observableArrayList(khachData));
            cboSinhVien.setItems(FXCollections.observableArrayList(luotDao.findSinhVien()));
            if (selectedGuest != null) khachData.stream().filter(k -> k.idKhach()==selectedGuest)
                    .findFirst().ifPresent(k -> tblKhach.getSelectionModel().select(k));
            // So lieu ba the tren cung: toan bo luot, luot trong thang, khach khac nhau trong thang.
            YearMonth thangHienTai = YearMonth.now();
            List<LuotDenThamRow> luotThang = luotData.stream()
                    .filter(v -> v.ngayDen() != null && YearMonth.from(v.ngayDen()).equals(thangHienTai))
                    .toList();
            lblTongLuot.setText(String.valueOf(luotData.size()));
            lblLuotThang.setText(String.valueOf(luotThang.size()));
            lblKhachThang.setText(String.valueOf(luotThang.stream()
                    .map(LuotDenThamRow::idKhach).distinct().count()));
            // Hien thi trang thai dang o KTX rieng trong tab Luot den tham.
            lblDangTham.setText(String.valueOf(luotData.stream()
                    .filter(v -> v.thoiGianDen()!=null && v.thoiGianDi()==null).count()));
            filterKhach(); filterLuot();
            // Đồng bộ lại thống kê sau mọi thao tác CRUD.
            traCuuThongKe();
        } catch (SQLException e) { error("Không thể tải dữ liệu từ MySQL",e); }
    }
    @FXML private void lamMoi() { reloadAll(); }
    private void filterKhach() {
        String q = norm(txtTimKhach.getText());
        filteredKhach.setPredicate(k -> norm(k.soCmt()).contains(q) || norm(k.hoTen()).contains(q)
                || String.valueOf(k.idKhach()).contains(q));
        lblKhachCount.setText("Hiển thị " + filteredKhach.size() + " / " + khachData.size() + " khách");
    }
    private void filterLuot() {
        String q = norm(txtTimLuot.getText());
        filteredLuot.setPredicate(v -> norm(v.tenKhach()).contains(q)||norm(v.soCmt()).contains(q)
                ||norm(v.maSv()).contains(q)||norm(v.tenSv()).contains(q)||String.valueOf(v.idLuot()).contains(q));
        lblLuotCount.setText("Hiển thị " + filteredLuot.size() + " / " + luotData.size() + " lượt");
    }

    @FXML private void themKhach() { guestDialog(null); }
    @FXML private void suaKhach() {
        KhachRow k = tblKhach.getSelectionModel().getSelectedItem();
        if (k==null) { message("Chọn một khách trong bảng trước."); return; }
        guestDialog(k);
    }
    private void guestDialog(KhachRow old) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(old==null ? "Thêm khách" : "Sửa hồ sơ khách");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        TextField cccd = new TextField(old==null?"":old.soCmt());
        TextField name = new TextField(old==null?"":old.hoTen());
        DatePicker birth = new DatePicker(old==null?null:old.ngaySinh());
        GridPane grid = form();
        grid.addRow(0,new Label("Số CMT/CCCD:"),cccd);
        grid.addRow(1,new Label("Họ và tên:"),name);
        grid.addRow(2,new Label("Ngày sinh:"),birth);
        dialog.getDialogPane().setContent(grid);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.OK) return;
        if (cccd.getText().isBlank()||name.getText().isBlank()) { message("CCCD và họ tên không được trống."); return; }
        try {
            KhachRow k = new KhachRow(old==null?0:old.idKhach(),cccd.getText().trim(),name.getText().trim(),birth.getValue());
            if (old==null) khachDao.insert(k); else khachDao.update(k);
            reloadAll();
        } catch (SQLException e) { error("Không thể lưu khách (CCCD có thể bị trùng)",e); }
    }
    @FXML private void xoaKhach() {
        KhachRow k = tblKhach.getSelectionModel().getSelectedItem();
        if (k==null) { message("Chọn một khách trước."); return; }
        if (!confirm("Xóa hồ sơ " + k.hoTen() + "? Hồ sơ đã có lịch sử thăm sẽ bị MySQL chặn.")) return;
        try { khachDao.delete(k.idKhach()); reloadAll(); }
        catch (SQLException e) { error("Không xóa được. Khách có thể đã có lịch sử thăm (ON DELETE RESTRICT).",e); }
    }

    @FXML private void checkIn() {
        KhachRow k = cboKhach.getValue(); SinhVienOption s = cboSinhVien.getValue();
        if (k==null||s==null) { message("Chọn khách và sinh viên được thăm trước khi check-in."); return; }
        if (!confirm("Ghi nhận " + k.hoTen() + " đến thăm " + s.hoTen() + " ngay lúc này?")) return;
        try { luotDao.checkIn(k.idKhach(),s.maSv()); reloadAll(); }
        catch (SQLException e) { error("Không check-in được",e); }
    }
    @FXML private void checkOut() {
        LuotDenThamRow v = tblLuot.getSelectionModel().getSelectedItem();
        if (v==null) { message("Chọn lượt đến thăm trong bảng trước."); return; }
        if (v.thoiGianDen()==null||v.thoiGianDi()!=null) { message("Chỉ check-out được lượt đã check-in và chưa ra về."); return; }
        if (!confirm("Xác nhận khách " + v.tenKhach() + " đã rời KTX?")) return;
        try { luotDao.checkOut(v.idLuot()); reloadAll(); }
        catch (SQLException e) { error("Không check-out được",e); }
    }
    @FXML private void suaLuot() {
        LuotDenThamRow v = tblLuot.getSelectionModel().getSelectedItem();
        if (v==null) { message("Chọn một lượt thăm trước."); return; }
        Dialog<ButtonType> dlg = new Dialog<>(); dlg.setTitle("Sửa lượt đến thăm #"+v.idLuot());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        ComboBox<KhachRow> k = new ComboBox<>(FXCollections.observableArrayList(khachData));
        k.setMaxWidth(Double.MAX_VALUE);
        khachData.stream().filter(x -> x.idKhach()==v.idKhach()).findFirst().ifPresent(k::setValue);
        ComboBox<SinhVienOption> sv = new ComboBox<>(cboSinhVien.getItems());
        sv.setMaxWidth(Double.MAX_VALUE);
        sv.getItems().stream().filter(x -> x.maSv().equals(v.maSv())).findFirst().ifPresent(sv::setValue);
        DatePicker ngay = new DatePicker(v.ngayDen());
        TextField den = new TextField(v.thoiGianDen()==null?"":EDIT_FMT.format(v.thoiGianDen()));
        TextField di = new TextField(v.thoiGianDi()==null?"":EDIT_FMT.format(v.thoiGianDi()));
        den.setPromptText("yyyy-MM-dd HH:mm hoặc để trống");
        di.setPromptText("yyyy-MM-dd HH:mm hoặc để trống");
        GridPane g=form(); g.addRow(0,new Label("Khách:"),k);g.addRow(1,new Label("Sinh viên:"),sv);
        g.addRow(2,new Label("Ngày đến:"),ngay);g.addRow(3,new Label("Giờ đến:"),den);g.addRow(4,new Label("Giờ đi:"),di);
        dlg.getDialogPane().setContent(g);
        if (dlg.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.OK) return;
        try {
            if (k.getValue()==null||sv.getValue()==null||ngay.getValue()==null) throw new IllegalArgumentException("Thiếu khách, SV hoặc ngày đến.");
            LocalDateTime timeDen = den.getText().isBlank()?null:LocalDateTime.parse(den.getText().trim(),EDIT_FMT);
            LocalDateTime timeDi = di.getText().isBlank()?null:LocalDateTime.parse(di.getText().trim(),EDIT_FMT);
            if (timeDen==null && timeDi!=null) throw new IllegalArgumentException("Không thể có giờ đi khi thiếu giờ đến.");
            if (timeDen!=null && !timeDen.toLocalDate().equals(ngay.getValue())) throw new IllegalArgumentException("Ngày đến phải khớp với phần ngày của giờ đến.");
            if (timeDi!=null && timeDi.isBefore(timeDen)) throw new IllegalArgumentException("Giờ đi không được trước giờ đến.");
            luotDao.updateVisit(v.idLuot(),k.getValue().idKhach(),sv.getValue().maSv(),ngay.getValue(),timeDen,timeDi);
            reloadAll();
        } catch (SQLException | IllegalArgumentException e) { error("Không lưu được lượt đến thăm",e); }
    }
    @FXML private void xoaLuot() {
        LuotDenThamRow v=tblLuot.getSelectionModel().getSelectedItem();
        if (v==null) { message("Chọn một lượt thăm trước."); return; }
        if (!confirm("Xóa vĩnh viễn lượt #"+v.idLuot()+"? Hành động này làm thay đổi số liệu thống kê.")) return;
        try { luotDao.deleteVisit(v.idLuot()); reloadAll(); }
        catch (SQLException e) { error("Không xóa được lượt thăm",e); }
    }
    @FXML private void traCuuThongKe() {
        try {
            YearMonth month=YearMonth.parse(txtThang.getText().trim());
            List<ThongKeKhachRow> rows=thongKeDao.thongKeTheoThang(month);
            tblThongKe.setItems(FXCollections.observableArrayList(rows));
            long total=rows.stream().mapToLong(ThongKeKhachRow::soLanDen).sum();
            lblThongKe.setText("Tháng " + month + "  •  " + rows.size() + " cặp SV–khách  •  Tổng " + total + " lượt thăm");
        } catch (DateTimeParseException e) { lblThongKe.setText("Tháng không hợp lệ. Nhập YYYY-MM, ví dụ 2026-10."); }
        catch (SQLException e) { error("Không tải được thống kê",e); }
    }
    private static GridPane form() {
        GridPane g = new GridPane(); g.setHgap(12);g.setVgap(12);g.setPadding(new Insets(16));return g;
    }
}
