package com.bmt.MyApp.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.bmt.MyApp.models.Transactions;
import com.bmt.MyApp.models.Transactions.TransactionStatus;

@Service
public class TransactionExcelService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] exportTransactionsToExcel(List<Transactions> transactions) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lịch sử giao dịch");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            createTransactionListHeader(sheet, headerStyle);

            int rowIndex = 1;
            for (Transactions transaction : transactions) {
                Row row = sheet.createRow(rowIndex++);
                fillTransactionRow(row, transaction, rowIndex - 1, dataStyle, numberStyle, dateStyle);
            }

            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            createSummarySection(sheet, transactions, rowIndex + 1, headerStyle, numberStyle);

            return convertToByteArray(workbook);
        }
    }

    public byte[] exportSingleTransactionToExcel(Transactions transaction) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Chi tiết giao dịch");

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            int currentRow = 0;

            currentRow = createTitle(sheet, "CHI TIẾT GIAO DỊCH", currentRow, titleStyle);
            currentRow += 2;

            currentRow = createSectionTitle(sheet, "THÔNG TIN CƠ BẢN", currentRow, headerStyle);
            currentRow = createInfoRow(sheet, "Mã giao dịch:", transaction.getVnpTxnRef(), currentRow, headerStyle, dataStyle);
            currentRow = createInfoRow(sheet, "Người dùng:", transaction.getUser().getFullName(), currentRow, headerStyle, dataStyle);
            currentRow = createInfoRow(sheet, "Email:", transaction.getUser().getEmail(), currentRow, headerStyle, dataStyle);
            currentRow = createInfoRow(sheet, "Dịch vụ:", transaction.getService().getName(), currentRow, headerStyle, dataStyle);
            currentRow++;

            currentRow = createSectionTitle(sheet, "THÔNG TIN THANH TOÁN", currentRow, headerStyle);
            currentRow = createInfoRow(sheet, "Số tiền:", formatCurrency(transaction.getAmount()), currentRow, headerStyle, numberStyle);
            currentRow = createInfoRow(sheet, "Đơn vị tiền tệ:", transaction.getCurrencyCode(), currentRow, headerStyle, dataStyle);
            currentRow = createInfoRow(sheet, "Trạng thái:", getStatusText(transaction.getStatus()), currentRow, headerStyle, dataStyle);
            currentRow++;

            currentRow = createSectionTitle(sheet, "THÔNG TIN THỜI GIAN", currentRow, headerStyle);
            currentRow = createInfoRow(sheet, "Ngày tạo:", formatDateTime(transaction.getCreatedAt()), currentRow, headerStyle, dateStyle);
            currentRow = createInfoRow(sheet, "Ngày cập nhật:", formatDateTime(transaction.getUpdatedAt()), currentRow, headerStyle, dateStyle);
            currentRow = createInfoRow(sheet, "Ngày hết hạn:", formatDateTime(transaction.getExpireDate()), currentRow, headerStyle, dateStyle);
            currentRow++;

            if (transaction.getVnpResponseCode() != null || transaction.getVnpTransactionNo() != null) {
                currentRow = createSectionTitle(sheet, "THÔNG TIN VNPAY", currentRow, headerStyle);
                if (transaction.getVnpResponseCode() != null) {
                    currentRow = createInfoRow(sheet, "Mã phản hồi:", transaction.getVnpResponseCode(), currentRow, headerStyle, dataStyle);
                }
                if (transaction.getVnpTransactionNo() != null) {
                    currentRow = createInfoRow(sheet, "Mã giao dịch VNPay:", transaction.getVnpTransactionNo(), currentRow, headerStyle, dataStyle);
                }
                if (transaction.getVnpBankCode() != null) {
                    currentRow = createInfoRow(sheet, "Ngân hàng:", transaction.getVnpBankCode(), currentRow, headerStyle, dataStyle);
                }
                if (transaction.getVnpPayDate() != null) {
                    LocalDateTime payDate = parseVnpPayDate(transaction.getVnpPayDate());
                    currentRow = createInfoRow(sheet, "Ngày thanh toán:", formatDateTime(payDate), currentRow, headerStyle, dateStyle);
                }
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.setColumnWidth(0, 4000);
            sheet.setColumnWidth(1, 6000);

            return convertToByteArray(workbook);
        }
    }

    private void createTransactionListHeader(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"STT", "Mã giao dịch", "Người dùng", "Số tiền", "Trạng thái", "Ngày giao dịch", "Ngày hết hạn"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillTransactionRow(Row row, Transactions transaction, int stt,
                                   CellStyle dataStyle, CellStyle numberStyle, CellStyle dateStyle) {
        row.createCell(0).setCellValue(stt);
        row.getCell(0).setCellStyle(dataStyle);

        row.createCell(1).setCellValue(transaction.getVnpTxnRef() != null ? transaction.getVnpTxnRef() : "");
        row.getCell(1).setCellStyle(dataStyle);

        row.createCell(2).setCellValue(transaction.getUser().getFullName());
        row.getCell(2).setCellStyle(dataStyle);

        row.createCell(3).setCellValue(formatCurrency(transaction.getAmount()));
        row.getCell(3).setCellStyle(numberStyle);

        row.createCell(4).setCellValue(getStatusText(transaction.getStatus()));
        row.getCell(4).setCellStyle(dataStyle);

        row.createCell(5).setCellValue(formatDateTime(transaction.getCreatedAt()));
        row.getCell(5).setCellStyle(dateStyle);

        row.createCell(6).setCellValue(formatDateTime(transaction.getExpireDate()));
        row.getCell(6).setCellStyle(dateStyle);
    }

    private void createSummarySection(Sheet sheet, List<Transactions> transactions,
                                     int startRow, CellStyle headerStyle, CellStyle numberStyle) {
        Row summaryHeaderRow = sheet.createRow(startRow);
        Cell summaryHeaderCell = summaryHeaderRow.createCell(0);
        summaryHeaderCell.setCellValue("TỔNG KẾT");
        summaryHeaderCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, 6));

        long totalTransactions = transactions.size();
        long successCount = transactions.stream().filter(t -> t.getStatus() == TransactionStatus.SUCCESS).count();
        long failedCount = transactions.stream().filter(t -> t.getStatus() == TransactionStatus.FAILED).count();
        BigDecimal totalAmount = transactions.stream()
            .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
            .map(Transactions::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Row row1 = sheet.createRow(startRow + 1);
        row1.createCell(0).setCellValue("Tổng số giao dịch:");
        row1.createCell(1).setCellValue(totalTransactions);
        row1.getCell(1).setCellStyle(numberStyle);

        Row row2 = sheet.createRow(startRow + 2);
        row2.createCell(0).setCellValue("Giao dịch thành công:");
        row2.createCell(1).setCellValue(successCount);
        row2.getCell(1).setCellStyle(numberStyle);

        Row row3 = sheet.createRow(startRow + 3);
        row3.createCell(0).setCellValue("Giao dịch thất bại:");
        row3.createCell(1).setCellValue(failedCount);
        row3.getCell(1).setCellStyle(numberStyle);

        Row row4 = sheet.createRow(startRow + 4);
        row4.createCell(0).setCellValue("Tổng tiền thành công:");
        row4.createCell(1).setCellValue(formatCurrency(totalAmount));
        row4.getCell(1).setCellStyle(numberStyle);
    }

    private int createTitle(Sheet sheet, String title, int rowIndex, CellStyle titleStyle) {
        Row titleRow = sheet.createRow(rowIndex);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 1));
        return rowIndex + 1;
    }

    private int createSectionTitle(Sheet sheet, String title, int rowIndex, CellStyle headerStyle) {
        Row sectionRow = sheet.createRow(rowIndex);
        Cell sectionCell = sectionRow.createCell(0);
        sectionCell.setCellValue(title);
        sectionCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 1));
        return rowIndex + 1;
    }

    private int createInfoRow(Sheet sheet, String label, String value, int rowIndex,
                             CellStyle labelStyle, CellStyle valueStyle) {
        Row infoRow = sheet.createRow(rowIndex);
        Cell labelCell = infoRow.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        Cell valueCell = infoRow.createCell(1);
        valueCell.setCellValue(value != null ? value : "");
        valueCell.setCellStyle(valueStyle);
        return rowIndex + 1;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 VND";
        return String.format("%,.0f VND", amount);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_FORMAT);
    }

    private String getStatusText(TransactionStatus status) {
        if (status == null) return "Không xác định";
        return switch (status) {
            case PENDING -> "Đang xử lý";
            case SUCCESS -> "Thành công";
            case FAILED -> "Thất bại";
            case CANCELLED -> "Đã hủy";
            case EXPIRED -> "Hết hạn";
            default -> "Không xác định";
        };
    }

    private LocalDateTime parseVnpPayDate(String vnpPayDateStr) {
        if (vnpPayDateStr == null || vnpPayDateStr.isEmpty()) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDateTime.parse(vnpPayDateStr, formatter);
    }

    private byte[] convertToByteArray(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
