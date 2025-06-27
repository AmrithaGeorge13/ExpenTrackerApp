package com.amron.ExpenseTracker.Repository;

import com.amron.ExpenseTracker.Model.DailyExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyExpenseRepository extends JpaRepository<DailyExpense,Long> {
}
