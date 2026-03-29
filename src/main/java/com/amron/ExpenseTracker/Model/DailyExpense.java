package com.amron.ExpenseTracker.Model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Setter
@Getter
@Entity
@ToString
@Table(name = "daily_expense")
public class DailyExpense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private LocalDate date;
    private String categories;
    private String subCategories;
    private Double actualAmount;
    private String monthYear;
    /** Salary/income credited on day >= 22 is attributed to the following month for reporting. */
    private String reportingMonthYear;
    private String weekNum;
    private String account;
    private boolean jointAccount;
    private String notes;
    private String quarterYear;
    private String budgetCategory;
    private String rawCategories;
    private String transactionType;

}
