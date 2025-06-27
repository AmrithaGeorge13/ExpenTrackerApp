package com.amron.ExpenseTracker.Repository;

import com.amron.ExpenseTracker.Model.BankStatementConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankStatementConfigRepository extends JpaRepository<BankStatementConfig, Integer> {
}
