package com.amron.ExpenseTracker.Model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;

@Setter
@Getter
@Entity
@ToString
@Table(name = "daily_expense")
public class DailyExpense {
    @jakarta.persistence.Id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private LocalDate date;
    private String categories;
    private String subCategories;
    private Double actualAmount;
    private String monthYear;
    private String weekNum;
    private String account;
    private String notes;
    private String quarterYear;
    private String budgetCategory;
    private String rawCategories;
    private String transactionType;

}
