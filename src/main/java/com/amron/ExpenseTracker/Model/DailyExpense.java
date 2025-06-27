package com.amron.ExpenseTracker.Model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.util.Date;
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
    private String subCategories;//TODO rename in table
    private Double actualAmount;
    private String monthYear;
    private String weekNum;//TODO rename in table
    private String account;
    private String notes;
    private String quarterYear;
    private String budgetCategory;
    @JsonIgnore
    private String rawCategories;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
