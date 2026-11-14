package com.ptit.quanlyktx.controller;

import com.ptit.quanlyktx.dao.*;
import com.ptit.quanlyktx.dto.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.event.ActionEvent;
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
    // Tat ca Dialog dung CSS rieng: scene cua Dialog khong tu ke thua CSS tu KhachThamView.
    private static final String DIALOG_CSS = "/com/ptit/quanlyktx/css/khachtham.css";

    private static Dialog<ButtonType> styledDialog(String windowTitle, int width) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(windowTitle);
        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("kt-dialog-pane");
        var css = KhachThamController.class.getResource(DIALOG_CSS);
        if (css != null) pane.getStylesheets().add(css.toExternalForm());
        pane.setMinWidth(width);
        pane.setPrefWidth(width);
        dialog.setResizable(false);
        return dialog;
    }

    private static Label wrapLabel(String text, String style) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add(style);
        return label;
    }

    private static VBox dialogBody(String icon, String heading, String description, String type) {
        Label symbol = new Label(icon);
        symbol.getStyleClass().addAll("kt-dialog-symbol", "kt-dialog-symbol-" + type);
        VBox titles = new VBox(5,
                wrapLabel(heading, "kt-dialog-heading"),
                wrapLabel(description, "kt-dialog-subtitle"));
        titles.setMinWidth(0);
        HBox header = new HBox(14, symbol, titles);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titles, Priority.ALWAYS);
        VBox content = new VBox(18, header);
        content.getStyleClass().add("kt-dialog-content");
        return content;
    }

    private static Label infoStrip(String text) {
        Label label = wrapLabel("ⓘ  " + text, "kt-dialog-info");
        return label;
    }

    private static VBox field(String text, Control control) {
        control.setMaxWidth(Double.MAX_VALUE);
        control.getStyleClass().add("kt-dialog-input");
        Label label = new Label(text);
        label.getStyleClass().add("kt-dialog-field-label");
        VBox box = new VBox(7, label, control);
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static GridPane form() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("kt-dialog-form");
        grid.setHgap(20);
        grid.setVgap(18);
        grid.setPadding(new Insets(2, 8, 2, 8));
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(50);
        left.setHgrow(Priority.ALWAYS);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(50);
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(left, right);
        return grid;
    }

    private static void configureButtons(Dialog<ButtonType> dialog, ButtonType action, ButtonType cancel,
                                         boolean dangerous) {
        Node yes = dialog.getDialogPane().lookupButton(action);
        if (yes != null) yes.getStyleClass().add(dangerous ? "kt-dialog-delete-btn" : "kt-dialog-primary-btn");
        if (cancel != null) {
            Node no = dialog.getDialogPane().lookupButton(cancel);
            if (no != null) no.getStyleClass().add("kt-dialog-cancel-btn");
        }
    }

    private static void notice(String heading, String detail, String type) {
        Dialog<ButtonType> dialog = styledDialog(heading, 515);
        ButtonType ok = new ButtonType("Đã hiểu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(ok);
        String icon = "error".equals(type) ? "!" : "warning".equals(type) ? "!" : "✓";
        VBox body = dialogBody(icon, heading,
                "error".equals(type) ? "Vui lòng kiểm tra thông tin và thử lại."
                : "warning".equals(type) ? "Thao tác chưa được thực hiện." : "Thông báo hệ thống", type);
        body.getChildren().add(wrapLabel(detail, "kt-dialog-message"));
        dialog.getDialogPane().setContent(body);
        configureButtons(dialog, ok, null, false);
        dialog.showAndWait();
    }

    private static String explainError(Exception error) {
        String raw = error.getMessage();
        if (raw == null || raw.isBlank()) return "Không thể hoàn thành thao tác. Vui lòng kiểm tra dữ liệu.";
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("unique constraint") || lower.contains("unique failed"))
            return "Thông tin đã tồn tại trong cơ sở dữ liệu (ví dụ số CCCD bị trùng).";
        if (lower.contains("foreign key constraint") || lower.contains("foreign key failed"))
            return "Bản ghi đang được liên kết với dữ liệu khác nên không thể xóa hoặc cập nhật.";
        if (lower.contains("database is locked"))
            return "Cơ sở dữ liệu đang được chương trình khác sử dụng. Hãy thử lại.";
        if (lower.contains("chua check-out") || lower.contains("chưa check-out"))
            return "Khách đang có lượt thăm chưa xác nhận rời KTX. Vui lòng hoàn tất lượt thăm đó trước.";
        return raw;
    }

    private static void error(String title, Exception e) {
        e.printStackTrace(); // Chi tiet ky thuat o Output cua NetBeans, khong hien SQL tho tren UI.
        notice(title, explainError(e), "error");
    }
    private static void message(String s) { notice("Thông báo", s, "warning"); }

    private static boolean confirm(String title, String description, boolean dangerous) {
        Dialog<ButtonType> dialog = styledDialog(title, 530);
        ButtonType agree = new ButtonType(dangerous ? "Xóa" : "Xác nhận", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(agree, cancel);
        VBox body = dialogBody(dangerous ? "!" : "?", title,
                dangerous ? "Hãy kiểm tra kỹ trước khi xóa." : "Kiểm tra thông tin trước khi tiếp tục.",
                dangerous ? "warning" : "info");
        body.getChildren().add(wrapLabel(description, "kt-dialog-message"));
        dialog.getDialogPane().setContent(body);
        configureButtons(dialog, agree, cancel, dangerous);
        return dialog.showAndWait().orElse(cancel) == agree;
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
        } catch (SQLException e) { error("Không thể tải dữ liệu từ cơ sở dữ liệu",e); }
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
        boolean editing = old != null;
        Dialog<ButtonType> dialog = styledDialog(editing ? "Sửa hồ sơ khách" : "Thêm khách thăm", 720);
        ButtonType save = new ButtonType(editing ? "Lưu thay đổi" : "Thêm khách", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(save, cancel);

        // Duy tri cac truong va du lieu cu, chi thay doi cach sap xep va hien thi.
        TextField cccd = new TextField(editing ? old.soCmt() : "");
        TextField name = new TextField(editing ? old.hoTen() : "");
        DatePicker birth = new DatePicker(editing ? old.ngaySinh() : null);
        cccd.setPromptText("Nhập số CMT/CCCD duy nhất");
        name.setPromptText("Nhập họ và tên khách thăm");
        birth.setPromptText("Chọn ngày sinh (không bắt buộc)");

        GridPane grid = form();
        grid.add(field("Số CMT/CCCD *", cccd), 0, 0);
        grid.add(field("Họ và tên khách *", name), 1, 0);
        grid.add(field("Ngày sinh", birth), 0, 1);

        VBox body = dialogBody(editing ? "✎" : "+",
                editing ? "Cập nhật hồ sơ khách" : "Thêm khách thăm",
                "Lưu thông tin khách vào danh bạ ký túc xá.", "info");
        body.getChildren().addAll(infoStrip("CCCD và họ tên là bắt buộc. Ngày sinh có thể để trống."), grid);
        dialog.getDialogPane().setContent(body);
        configureButtons(dialog, save, cancel, false);

        // Ngan dong Dialog khi nhap thieu thong tin, tranh mat du lieu dang go.
        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (cccd.getText().isBlank() || name.getText().isBlank()) {
                message("Vui lòng nhập đầy đủ số CMT/CCCD và họ tên khách.");
                event.consume();
            }
        });

        if (dialog.showAndWait().orElse(cancel) != save) return;
        try {
            KhachRow k = new KhachRow(editing ? old.idKhach() : 0,
                    cccd.getText().trim(), name.getText().trim(), birth.getValue());
            if (editing) khachDao.update(k); else khachDao.insert(k);
            reloadAll();
        } catch (SQLException e) { error("Không thể lưu hồ sơ khách", e); }
    }
    @FXML private void xoaKhach() {
        KhachRow k = tblKhach.getSelectionModel().getSelectedItem();
        if (k==null) { message("Chọn một khách trước."); return; }
        if (!confirm("Xóa hồ sơ khách",
                "Bạn muốn xóa hồ sơ của " + k.hoTen() + " (CCCD: " + k.soCmt() + ")? "
                + "Nếu khách đã có lịch sử thăm, hệ thống sẽ không cho phép xóa để bảo toàn dữ liệu.", true)) return;
        try { khachDao.delete(k.idKhach()); reloadAll(); }
        catch (SQLException e) { error("Không xóa được. Khách có thể đã có lịch sử thăm (ON DELETE RESTRICT).",e); }
    }

    @FXML private void checkIn() {
        KhachRow k = cboKhach.getValue(); SinhVienOption s = cboSinhVien.getValue();
        if (k==null||s==null) { message("Chọn khách và sinh viên được thăm trước khi check-in."); return; }
        if (!confirm("Xác nhận khách đến", "Ghi nhận " + k.hoTen()
                + " đến thăm " + s.hoTen() + " ngay lúc này?", false)) return;
        try { luotDao.checkIn(k.idKhach(),s.maSv()); reloadAll(); }
        catch (SQLException e) { error("Không check-in được",e); }
    }
    @FXML private void checkOut() {
        LuotDenThamRow v = tblLuot.getSelectionModel().getSelectedItem();
        if (v==null) { message("Chọn lượt đến thăm trong bảng trước."); return; }
        if (v.thoiGianDen()==null||v.thoiGianDi()!=null) { message("Chỉ check-out được lượt đã check-in và chưa ra về."); return; }
        if (!confirm("Xác nhận khách rời KTX", "Xác nhận khách " + v.tenKhach()
                + " đã rời KTX? Hệ thống sẽ ghi nhận thời gian rời KTX.", false)) return;
        try { luotDao.checkOut(v.idLuot()); reloadAll(); }
        catch (SQLException e) { error("Không check-out được",e); }
    }
    @FXML private void suaLuot() {
        LuotDenThamRow v = tblLuot.getSelectionModel().getSelectedItem();
        if (v == null) { message("Hãy chọn một lượt thăm trong bảng trước khi sửa."); return; }

        Dialog<ButtonType> dlg = styledDialog("Sửa lượt đến thăm #" + v.idLuot(), 750);
        ButtonType save = new ButtonType("Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().addAll(save, cancel);

        ComboBox<KhachRow> k = new ComboBox<>(FXCollections.observableArrayList(khachData));
        k.setMaxWidth(Double.MAX_VALUE);
        khachData.stream().filter(x -> x.idKhach() == v.idKhach()).findFirst().ifPresent(k::setValue);
        ComboBox<SinhVienOption> sv = new ComboBox<>(cboSinhVien.getItems());
        sv.setMaxWidth(Double.MAX_VALUE);
        sv.getItems().stream().filter(x -> x.maSv().equals(v.maSv())).findFirst().ifPresent(sv::setValue);
        DatePicker ngay = new DatePicker(v.ngayDen());
        TextField den = new TextField(v.thoiGianDen() == null ? "" : EDIT_FMT.format(v.thoiGianDen()));
        TextField di = new TextField(v.thoiGianDi() == null ? "" : EDIT_FMT.format(v.thoiGianDi()));
        den.setPromptText("yyyy-MM-dd HH:mm hoặc để trống");
        di.setPromptText("yyyy-MM-dd HH:mm hoặc để trống");

        GridPane grid = form();
        grid.add(field("Khách đến thăm *", k), 0, 0);
        grid.add(field("Sinh viên được thăm *", sv), 1, 0);
        grid.add(field("Ngày đến *", ngay), 0, 1);
        grid.add(field("Thời gian đến", den), 1, 1);
        grid.add(field("Thời gian đi", di), 0, 2);

        VBox body = dialogBody("✎", "Cập nhật lượt đến thăm #" + v.idLuot(),
                "Điều chỉnh khách, sinh viên được thăm và thời gian ra/vào.", "info");
        body.getChildren().addAll(
                infoStrip("Nhập thời gian theo mẫu yyyy-MM-dd HH:mm. Có thể để trống thời gian đến/đi theo dữ liệu hiện có."),
                grid);
        dlg.getDialogPane().setContent(body);
        configureButtons(dlg, save, cancel, false);

        // Kiem tra tai form de neu sai, Dialog van mo va giu nguyen du lieu nguoi dung vua nhap.
        Node saveButton = dlg.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                validateVisit(k, sv, ngay, den, di);
            } catch (IllegalArgumentException | DateTimeException ex) {
                message(ex.getMessage());
                event.consume();
            }
        });

        if (dlg.showAndWait().orElse(cancel) != save) return;
        try {
            LocalDateTime timeDen = parseOptionalTime(den.getText());
            LocalDateTime timeDi = parseOptionalTime(di.getText());
            luotDao.updateVisit(v.idLuot(), k.getValue().idKhach(), sv.getValue().maSv(),
                    ngay.getValue(), timeDen, timeDi);
            reloadAll();
        } catch (SQLException | IllegalArgumentException | DateTimeException e) {
            error("Không thể lưu lượt đến thăm", e);
        }
    }

    private static LocalDateTime parseOptionalTime(String text) {
        return text == null || text.isBlank() ? null : LocalDateTime.parse(text.trim(), EDIT_FMT);
    }

    private static void validateVisit(ComboBox<KhachRow> k, ComboBox<SinhVienOption> sv,
                                      DatePicker ngay, TextField den, TextField di) {
        if (k.getValue() == null || sv.getValue() == null || ngay.getValue() == null)
            throw new IllegalArgumentException("Vui lòng chọn khách, sinh viên được thăm và ngày đến.");
        LocalDateTime timeDen;
        LocalDateTime timeDi;
        try {
            timeDen = parseOptionalTime(den.getText());
            timeDi = parseOptionalTime(di.getText());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Thời gian phải có định dạng yyyy-MM-dd HH:mm, ví dụ 2026-11-13 14:30.");
        }
        if (timeDen == null && timeDi != null)
            throw new IllegalArgumentException("Không thể có thời gian đi nếu chưa có thời gian đến.");
        if (timeDen != null && !timeDen.toLocalDate().equals(ngay.getValue()))
            throw new IllegalArgumentException("Ngày đến phải trùng với ngày trong thời gian đến.");
        if (timeDi != null && timeDi.isBefore(timeDen))
            throw new IllegalArgumentException("Thời gian đi không được trước thời gian đến.");
    }
    @FXML private void xoaLuot() {
        LuotDenThamRow v=tblLuot.getSelectionModel().getSelectedItem();
        if (v==null) { message("Chọn một lượt thăm trước."); return; }
        if (!confirm("Xóa lượt đến thăm",
                "Xóa vĩnh viễn lượt #" + v.idLuot() + " của khách " + v.tenKhach()
                + "? Thao tác này sẽ làm thay đổi số liệu thống kê khách thăm.", true)) return;
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
}
