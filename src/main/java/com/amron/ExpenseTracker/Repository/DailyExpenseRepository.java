package com.amron.ExpenseTracker.Repository;

import com.amron.ExpenseTracker.Model.DailyExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyExpenseRepository extends JpaRepository<DailyExpense, Long> {

    boolean existsByDateAndActualAmountAndAccountAndRawCategories(
            LocalDate date, Double actualAmount, String account, String rawCategories);

    List<DailyExpense> findByReportingMonthYear(String reportingMonthYear);

    List<DailyExpense> findByReportingMonthYearAndCategories(String reportingMonthYear, String categories);

    List<DailyExpense> findByReportingMonthYearAndAccount(String reportingMonthYear, String account);

    @Query("SELECT d.categories, SUM(d.actualAmount) FROM DailyExpense d " +
           "WHERE d.reportingMonthYear = :monthYear AND d.transactionType = 'DEBIT' " +
           "GROUP BY d.categories ORDER BY SUM(d.actualAmount) DESC")
    List<Object[]> sumExpensesByCategoryForMonth(@Param("monthYear") String monthYear);

    @Query("SELECT d.reportingMonthYear, d.categories, SUM(d.actualAmount) FROM DailyExpense d " +
           "WHERE d.transactionType = 'DEBIT' " +
           "GROUP BY d.reportingMonthYear, d.categories ORDER BY d.reportingMonthYear, SUM(d.actualAmount) DESC")
    List<Object[]> monthlyTrendByCategory();

    @Query("SELECT d.reportingMonthYear, " +
           "SUM(CASE WHEN d.transactionType = 'CREDIT' AND d.categories NOT IN ('Transfer In') THEN d.actualAmount ELSE 0 END), " +
           "SUM(CASE WHEN d.transactionType = 'DEBIT' AND d.categories NOT IN ('Transfer Out','Credit Card Payment','Investment') THEN d.actualAmount ELSE 0 END) " +
           "FROM DailyExpense d GROUP BY d.reportingMonthYear ORDER BY d.reportingMonthYear")
    List<Object[]> monthlyIncomVsExpense();
}
