package com.amron.ExpenseTracker.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="bank_statement_config")
public class BankStatementConfig {

    @jakarta.persistence.Id
    @org.springframework.data.annotation.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String bankName;
    private String sheetColumns;
    private int numberOfColumns;
    private int startRow;
    private int startCol;

}
