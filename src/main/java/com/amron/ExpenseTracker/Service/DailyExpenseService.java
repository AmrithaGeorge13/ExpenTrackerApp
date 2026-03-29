package com.amron.ExpenseTracker.Service;

import com.amron.ExpenseTracker.Model.Categories;
import com.amron.ExpenseTracker.Model.DailyExpense;
import com.amron.ExpenseTracker.Repository.DailyExpenseRepository;
import com.amron.ExpenseTracker.Utility.ExcelReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DailyExpenseService {
    @Autowired
    DailyExpenseRepository dailyExpenseRepository;
    @Autowired
    ExcelReader excelReader;
    @Autowired
    BankStatementConfigService bankStatementConfigService;
    @Autowired
    CategoriesService categoriesService;

    public Map<String, Object> importDailyExpensesFromExcel(String filePath) throws IOException {
        List<Categories> allCategories = categoriesService.getAllCategories();
        List<DailyExpense> parsed = excelReader.readDailyExpensesFromExcel(filePath, bankStatementConfigService, allCategories);

        List<DailyExpense> toSave = new ArrayList<>();
        int skipped = 0;
        for (DailyExpense e : parsed) {
            if (dailyExpenseRepository.existsByDateAndActualAmountAndAccountAndRawCategories(
                    e.getDate(), e.getActualAmount(), e.getAccount(), e.getRawCategories())) {
                skipped++;
            } else {
                toSave.add(e);
            }
        }
        dailyExpenseRepository.saveAll(toSave);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", toSave.size());
        result.put("skipped_duplicates", skipped);
        result.put("total_parsed", parsed.size());
        return result;
    }

    public List<DailyExpense> getExpensesByMonth(String reportingMonthYear) {
        return dailyExpenseRepository.findByReportingMonthYear(reportingMonthYear);
    }

    public List<DailyExpense> getExpensesByMonthAndCategory(String reportingMonthYear, String category) {
        return dailyExpenseRepository.findByReportingMonthYearAndCategories(reportingMonthYear, category);
    }

    public List<Map<String, Object>> getCategorySummary(String reportingMonthYear) {
        List<Object[]> rows = dailyExpenseRepository.sumExpensesByCategoryForMonth(reportingMonthYear);
        return rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", r[0]);
            m.put("totalAmount", r[1]);
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMonthlyTrend() {
        List<Object[]> rows = dailyExpenseRepository.monthlyTrendByCategory();
        return rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("reportingMonthYear", r[0]);
            m.put("category", r[1]);
            m.put("totalAmount", r[2]);
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMonthlyIncomeVsExpense() {
        List<Object[]> rows = dailyExpenseRepository.monthlyIncomVsExpense();
        return rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("reportingMonthYear", r[0]);
            m.put("totalIncome", r[1]);
            m.put("totalExpense", r[2]);
            return m;
        }).collect(Collectors.toList());
    }

    public List<DailyExpense> getAllExpenses() {
        return dailyExpenseRepository.findAll();
    }
}
