package com.amron.ExpenseTracker.Service;

import com.amron.ExpenseTracker.Model.BankStatementConfig;
import com.amron.ExpenseTracker.Repository.BankStatementConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BankStatementConfigService {
    @Autowired
    BankStatementConfigRepository bankStatementConfigRepository;

    public List<BankStatementConfig> getAllBankStatementConfig() {
        return bankStatementConfigRepository.findAll();
    }

    public BankStatementConfig getBankStatementConfig(String bankName) {
        return getAllBankStatementConfig().stream().filter(x->x.getBankName().toLowerCase().contains(bankName.toLowerCase())).findAny().orElse(null);
    }
}
