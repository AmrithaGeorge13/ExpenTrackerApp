package com.amron.ExpenseTracker.Service;

import com.amron.ExpenseTracker.Model.DailyExpense;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    @Autowired
    DailyExpenseService dailyExpenseService;

    private static final String[] HEADERS = {
            "ID", "Date", "Account", "Joint Account", "Transaction Type",
            "Category", "Sub Category", "Amount",
            "Month Year", "Reporting Month Year", "Quarter Year", "Week",
            "Raw Description", "Notes"
    };

    /**
     * Exports all expenses to the given file path as .xlsx.
     * The default path is C:\Users\ASUS\Downloads\Daily_Expense_Categorised.xlsx
     */
    public void exportToExcel(String outputPath) throws IOException {
        List<DailyExpense> expenses = dailyExpenseService.getAllExpenses();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Expenses");

            // Header row
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (DailyExpense e : expenses) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(e.getId());
                row.createCell(1).setCellValue(e.getDate() != null ? e.getDate().toString() : "");
                row.createCell(2).setCellValue(e.getAccount() != null ? e.getAccount() : "");
                row.createCell(3).setCellValue(e.isJointAccount() ? "Yes" : "No");
                row.createCell(4).setCellValue(e.getTransactionType() != null ? e.getTransactionType() : "");
                row.createCell(5).setCellValue(e.getCategories() != null ? e.getCategories() : "");
                row.createCell(6).setCellValue(e.getSubCategories() != null ? e.getSubCategories() : "");
                row.createCell(7).setCellValue(e.getActualAmount() != null ? e.getActualAmount() : 0.0);
                row.createCell(8).setCellValue(e.getMonthYear() != null ? e.getMonthYear() : "");
                row.createCell(9).setCellValue(e.getReportingMonthYear() != null ? e.getReportingMonthYear() : "");
                row.createCell(10).setCellValue(e.getQuarterYear() != null ? e.getQuarterYear() : "");
                row.createCell(11).setCellValue(e.getWeekNum() != null ? e.getWeekNum() : "");
                row.createCell(12).setCellValue(e.getRawCategories() != null ? e.getRawCategories() : "");
                row.createCell(13).setCellValue(e.getNotes() != null ? e.getNotes() : "");
            }

            // Auto-size key columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
        }
    }
}
