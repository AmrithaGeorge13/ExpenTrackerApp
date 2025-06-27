package com.amron.ExpenseTracker.Service;

import com.amron.ExpenseTracker.Model.Categories;
import com.amron.ExpenseTracker.Model.DailyExpense;
import com.amron.ExpenseTracker.Repository.DailyExpenseRepository;
import com.amron.ExpenseTracker.Utility.ExcelReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

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

    public String importDailyExpensesFromExcel(String filePath) throws IOException {
        List<Categories> allCategories = categoriesService.getAllCategories();
        List<DailyExpense> dailyExpenses = excelReader.readDailyExpensesFromExcel(filePath,bankStatementConfigService,allCategories);
        //TODO uncomment below line for saving to DB
       // dailyExpenseRepository.saveAll(dailyExpenses);
        return null;
    }

}
