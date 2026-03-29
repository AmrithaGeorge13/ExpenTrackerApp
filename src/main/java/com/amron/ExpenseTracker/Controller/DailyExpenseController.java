package com.amron.ExpenseTracker.Controller;

import com.amron.ExpenseTracker.Model.DailyExpense;
import com.amron.ExpenseTracker.Service.DailyExpenseService;
import com.amron.ExpenseTracker.Service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/dailyExpenses")
public class DailyExpenseController {

    private static final String DEFAULT_EXPORT_PATH =
            "C:\\Users\\ASUS\\Downloads\\Daily_Expense_Categorised.xlsx";

    private final DailyExpenseService dailyExpenseService;
    private final ExcelExportService excelExportService;

    /** Import all statements from a folder. Returns imported/skipped counts. */
    @GetMapping("/import")
    public Map<String, Object> importDailyExpenses(@RequestParam String filePath) {
        try {
            return dailyExpenseService.importDailyExpensesFromExcel(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to import from path: " + filePath, e);
        }
    }

    /** Get all expenses for a reporting month, e.g. ?reportingMonthYear=MARCH+2026 */
    @GetMapping
    public List<DailyExpense> getExpenses(
            @RequestParam(required = false) String reportingMonthYear,
            @RequestParam(required = false) String category) {
        if (reportingMonthYear != null && category != null) {
            return dailyExpenseService.getExpensesByMonthAndCategory(reportingMonthYear, category);
        }
        if (reportingMonthYear != null) {
            return dailyExpenseService.getExpensesByMonth(reportingMonthYear);
        }
        return dailyExpenseService.getAllExpenses();
    }

    /** Category-wise spend totals (DEBIT only) for a given reporting month. */
    @GetMapping("/summary")
    public List<Map<String, Object>> getCategorySummary(@RequestParam String reportingMonthYear) {
        return dailyExpenseService.getCategorySummary(reportingMonthYear);
    }

    /** Month-by-month spend broken down by category — use for PowerBI trend charts. */
    @GetMapping("/monthly-trend")
    public List<Map<String, Object>> getMonthlyTrend() {
        return dailyExpenseService.getMonthlyTrend();
    }

    /** Monthly income vs expense summary (excludes transfers and investments from expense). */
    @GetMapping("/income-vs-expense")
    public List<Map<String, Object>> getIncomeVsExpense() {
        return dailyExpenseService.getMonthlyIncomeVsExpense();
    }

    /**
     * Refresh the PowerBI Excel file at the default path (or a custom path).
     * Call this after importing new statements to update the dashboard data source.
     */
    @GetMapping("/export")
    public Map<String, Object> exportForPowerBI(
            @RequestParam(defaultValue = DEFAULT_EXPORT_PATH) String outputPath) {
        try {
            excelExportService.exportToExcel(outputPath);
            return Map.of("status", "success", "exportedTo", outputPath);
        } catch (IOException e) {
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }
}
