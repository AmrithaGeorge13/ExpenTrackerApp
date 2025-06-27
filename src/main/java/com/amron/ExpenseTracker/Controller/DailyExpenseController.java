package com.amron.ExpenseTracker.Controller;

import com.amron.ExpenseTracker.Service.DailyExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/dailyExpenses")
public class DailyExpenseController {
    private final DailyExpenseService dailyExpenseService;

    @GetMapping("/import")
    public String importDailyExpenses(@RequestParam String filePath) {
        String message = null;
        try {
            message = dailyExpenseService.importDailyExpensesFromExcel(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return message;
    }
}
