package com.aleksandar.threedforgemarket.service.report;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ExcelReportWriter {

    public XSSFWorkbook createWorkbook() {
        return new XSSFWorkbook();
    }

    public void addSheet(
            Workbook workbook,
            String sheetName,
            List<String> headers,
            List<List<?>> rows
    ) {
        Sheet sheet = workbook.createSheet(sheetName);
        ReportStyles styles = new ReportStyles(workbook);

        writeHeader(sheet, headers, styles.headerStyle());
        writeRows(sheet, rows, styles);
        sheet.createFreezePane(0, 1);
        autoSizeColumns(sheet, headers.size());
    }

    public void addMessageSheet(Workbook workbook, String sheetName, String message) {
        Sheet sheet = workbook.createSheet(sheetName);
        ReportStyles styles = new ReportStyles(workbook);
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue(message);
        cell.setCellStyle(styles.headerStyle());
        sheet.autoSizeColumn(0);
    }

    public byte[] toByteArray(Workbook workbook) {
        try (workbook; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate report right now.", exception);
        }
    }

    private void writeHeader(Sheet sheet, List<String> headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);

        for (int column = 0; column < headers.size(); column++) {
            Cell cell = headerRow.createCell(column);
            cell.setCellValue(headers.get(column));
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeRows(Sheet sheet, List<List<?>> rows, ReportStyles styles) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            List<?> values = rows.get(rowIndex);

            for (int column = 0; column < values.size(); column++) {
                writeCell(row.createCell(column), values.get(column), styles);
            }
        }
    }

    private void writeCell(Cell cell, Object value, ReportStyles styles) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
            cell.setCellStyle(styles.moneyStyle());
            return;
        }

        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }

        if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
            return;
        }

        if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime);
            cell.setCellStyle(styles.dateTimeStyle());
            return;
        }

        cell.setCellValue(String.valueOf(value));
        cell.setCellStyle(styles.textStyle());
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
            int currentWidth = sheet.getColumnWidth(column);
            int cappedWidth = Math.min(Math.max(currentWidth + 512, 3000), 12000);
            sheet.setColumnWidth(column, cappedWidth);
        }
    }

    private record ReportStyles(
            CellStyle headerStyle,
            CellStyle dateTimeStyle,
            CellStyle moneyStyle,
            CellStyle textStyle
    ) {
        private ReportStyles(Workbook workbook) {
            this(
                    headerStyle(workbook),
                    dateTimeStyle(workbook),
                    moneyStyle(workbook),
                    textStyle(workbook)
            );
        }

        private static CellStyle headerStyle(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            style.setFont(font);
            style.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return style;
        }

        private static CellStyle dateTimeStyle(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            CreationHelper creationHelper = workbook.getCreationHelper();
            style.setDataFormat(creationHelper.createDataFormat().getFormat("dd mmm yyyy, hh:mm"));
            return style;
        }

        private static CellStyle moneyStyle(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            style.setDataFormat(dataFormat.getFormat("€#,##0.00"));
            return style;
        }

        private static CellStyle textStyle(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setWrapText(true);
            return style;
        }
    }
}
