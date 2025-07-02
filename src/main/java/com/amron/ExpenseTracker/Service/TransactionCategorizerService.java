package com.amron.ExpenseTracker.Service;

import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class TransactionCategorizerService {

    // Transaction type specific rules (highest priority)
    private static final List<TransactionTypeRule> TRANSACTION_TYPE_RULES = List.of(
            new TransactionTypeRule("CREDIT", List.of("SALARY", "PAYROLL", "HONEYWELL"), "Income"),
            new TransactionTypeRule("CREDIT", List.of("INTEREST", "DIVIDEND"), "Income"),
            new TransactionTypeRule("CREDIT", List.of("REFUND", "RVSL", "REVERSAL"), "Refund"),
            new TransactionTypeRule("CREDIT", List.of("GROWW", "GROWW-BSE.GROWWPAY", "GROWWSTOCKS", "GROWW-BSE.GROWWPAY", "GROWWSTOCKS"), "Income"),
            new TransactionTypeRule("DEBIT", List.of("LOAN EMI", "LOAN PAYMENT"), "Loan Payment"),
            new TransactionTypeRule("DEBIT", List.of("CREDIT CARD PAYMENT", "CC PAYMENT", "creditcard"), "Credit Card Payment"),
            new TransactionTypeRule("DEBIT", List.of("TAX", "GST"), "Tax Payment"),
            new TransactionTypeRule("DEBIT", List.of("INSURANCE PREMIUM"), "Insurance"),
            new TransactionTypeRule("DEBIT", List.of("Investmen", "INVEST", "GROWW", "GROWW-BSE.GROWWPAY", "GROWWSTOCKS", "ESMF0001119", "Emergency fun", "Investment"), "Investment")
    );

    // Priority-ordered list of rules (after transaction type rules)
    private static final List<CategoryRule> CATEGORY_RULES = List.of(
            // 1. Specific merchant exact matches
            new CategoryRule(List.of("NOBROKER TECHNOLOGIES"), "Miscellaneous"),
            new CategoryRule(List.of("UBER INDIA SYSTEMS", "OLACABS", "RAPIDO", "CAB"), "Transportation"),
            new CategoryRule(List.of("IRCTC"), "Travel"),
            new CategoryRule(List.of("IKEA INDIA PVT LTD"), "Shopping"),
            new CategoryRule(List.of("BOOKMYSHOW"), "Entertainment"),
            new CategoryRule(List.of("BIGBASKET", "GROFERS", "BLINKIT", "SWIGGY INSTAMART", "BBNOW", "ZOMATA"), "Groceries"),
            new CategoryRule(List.of("POPEYES", "MEGHANA FOODS"), "Food & Dining"),
            new CategoryRule(List.of("DMRC LIMITED"), "Transportation"),
            new CategoryRule(List.of("DECATHLON"), "Sports"),
            new CategoryRule(List.of("AMAZON PAY", "SRIVENKATEAHWARAGRAM", "EKART"), "Shopping"),
            new CategoryRule(List.of("BUNDL TECHNOLOGIES", "SWIGGY"), "Food & Dining"),
            new CategoryRule(List.of("ANI TECHNOLOGIES"), "Transportation"), // Ola
            new CategoryRule(List.of("URBANCOMPANY"), "Personal Care"),
            new CategoryRule(List.of("CAREINSURANCE"), "Insurance"),
            new CategoryRule(List.of("THE MALANKARA ORT", "MOSC MEDICAL"), "Hospital Expense"),
            new CategoryRule(List.of("RD INSTALLMENT", "INDMONEY", "ACH/GROWW"), "Investment"),
            new CategoryRule(List.of("OPTIMUMNUTRITIO", "FITNESS"), "GYM"),

            // 2. POS transaction patterns
            new CategoryRule(List.of("POS/HPCL"), "Fuel"),
            new CategoryRule(List.of("POS/DMART"), "Groceries"),
            new CategoryRule(List.of("POS/LULU"), "Groceries"),
            new CategoryRule(List.of("POS/NANDUS"), "Groceries"),
            new CategoryRule(List.of("POS/POPEYES"), "Food & Dining"),

            // 3. Transaction type patterns
            new CategoryRule(List.of("ATM-CASH", "DC INTL ATM W", "ATM W", "ATW"), "ATM Withdrawal"),
            new CategoryRule(List.of("MBBPAY", "AUTOBPAY", "BILPAY", "BROADBAND", "AIRTEL PAYMENTS BANK"), "Utilities"),
            new CategoryRule(List.of("ECOM PUR"), "Shopping"),
            new CategoryRule(List.of("MB FTB/DADDY", "KUSUMAM JOHN"), "Transfer to Home"),
            new CategoryRule(List.of("UPI/P2M/ENGLISH.BMRC.PAYU"), "Transportation"),

            // 4. MCC code based categorization
            new CategoryRule(List.of("5411"), "Groceries"),
            new CategoryRule(List.of("/5541", "petrol", "petr"), "Fuel"),
            new CategoryRule(List.of("5812", "5814"), "Food & Dining"),
            new CategoryRule(List.of("4121"), "Transportation"),
            new CategoryRule(List.of("6300"), "Insurance"),
            new CategoryRule(List.of("7349"), "Personal Care"),

            // 5. Generic patterns (lowest priority)
            new CategoryRule(List.of("FOOD"), "Food & Dining"),
            new CategoryRule(List.of("GROCER"), "Groceries"),
            new CategoryRule(List.of("SHOPPING"), "Shopping"),
            new CategoryRule(List.of("P2M"), "Merchant Payment"),
            new CategoryRule(List.of("POS"), "Shopping"),
            new CategoryRule(List.of("ACH C"), "Dividend"),
            new CategoryRule(List.of("HARI PRASAD R"), "Rent"),
            new CategoryRule(List.of("SHILPANJALI  H", "LAUNDRY", "IRONING", "RAJESH   BOHARA"), "Housing"),
            new CategoryRule(List.of("ORANGE HEALTH", "HEALTH", "PHARMAC", "PHARMACY"), "Hospital Expense"),
            new CategoryRule(List.of("APPLAMP/BILLDESKPG.APPL", "APPLESERVICES.B", "PLAYSTORE", "SPOTIFY"), "Subscription")
    );

    // Merchant name patterns with flexible matching
    private static final Map<Pattern, String> FLEXIBLE_MERCHANT_PATTERNS = Map.ofEntries(
            Map.entry(Pattern.compile(".*NOBROKER.*"), "Miscellaneous"),
            Map.entry(Pattern.compile(".*UBER.*"), "Transportation"),
            Map.entry(Pattern.compile(".*RAPIDO.*"), "Transportation"),
            Map.entry(Pattern.compile(".*OLA.*"), "Transportation"),
            Map.entry(Pattern.compile(".*BIG.*BASKET.*"), "Groceries"),
            Map.entry(Pattern.compile(".*SWIGGY.*INSTAMART.*"), "Groceries"),
            Map.entry(Pattern.compile(".*BBNOW.*"), "Groceries"),
            Map.entry(Pattern.compile(".*IKEA.*"), "Shopping"),
            Map.entry(Pattern.compile(".*BOOKMYSHOW.*"), "Entertainment"),
            Map.entry(Pattern.compile(".*MEGHANA.*FOOD.*"), "Food & Dining"),
            Map.entry(Pattern.compile(".*POPEYES.*"), "Food & Dining"),
            Map.entry(Pattern.compile(".*DMRC.*"), "Transportation"),
            Map.entry(Pattern.compile(".*DECATHLON.*"), "Sports"),
            Map.entry(Pattern.compile(".*URBAN.*COMPANY.*"), "Personal Care"),
            Map.entry(Pattern.compile(".*MALANKARA.*"), "Hospital Expense"),
            Map.entry(Pattern.compile(".*MEDICAL.*"), "Hospital Expense"),
            Map.entry(Pattern.compile(".*INSURANCE.*"), "Insurance"),
            Map.entry(Pattern.compile(".*SALARY.*"), "Income")

    );

    // Known miscellaneous patterns
    private static final Set<String> KNOWN_MISC_PATTERNS = Set.of(
            "TEST", "NULL", "CHRG", "ADJUSTMENT", "FEE"
    );

    private static final Set<String> DIRECT_MERCHANT_MATCHES = Set.of(
            "SWIGGY", "ZOMATO", "UBER", "OLA", "RAPIDO",
            "BIGBASKET", "GROFERS", "BLINKIT", "DMART"
    );

    private static final List<TransactionTypeRule> TRANFER_RULES = List.of(
            new TransactionTypeRule("CREDIT", List.of("AMRITHA.GEORGE", "AMRITHA GEORGE", "AMRITHA", "RONY", "RONY PETER", "AMR.RON", "RONYMEP", "7012236240"), "Transfer In"),
            new TransactionTypeRule("DEBIT", List.of("AMRITHA.GEORGE", "AMRITHA GEORGE", "AMRITHA", "RONY", "RONY PETER", "AMR.RON", "RONYMEP", "7012236240"), "Transfer Out")
    );

    public String categorizeTransaction(String rawDescription, String transactionType) {
        if (StringUtils.isEmpty(rawDescription)) {
            return "Unknown";
        }

        String upperDesc = rawDescription.toUpperCase().trim();
        String upperTransactionType = transactionType != null ? transactionType.toUpperCase() : "";

        // 1. Check against transaction-type specific rules (highest priority)
        for (TransactionTypeRule rule : TRANSACTION_TYPE_RULES) {
            if (upperTransactionType.equalsIgnoreCase(rule.transactionType)) {
                for (String keyword : rule.keywords) {
                    if (upperDesc.contains(keyword.toUpperCase())) {
                        return rule.category;
                    }
                }
            }
        }

        // 3. Check against all keyword rules
        for (CategoryRule rule : CATEGORY_RULES) {
            for (String keyword : rule.keywords) {
                if (upperDesc.contains(keyword.toUpperCase())) {
                    return rule.category;
                }
            }
        }

        // 4. Extract and check merchant name
        String merchant = extractMerchantName(upperDesc);
        if (merchant != null) {
            // Direct contains check for well-known merchants
            for (String merchantName : DIRECT_MERCHANT_MATCHES) {
                if (merchant.contains(merchantName)) {
                    return getCategoryForMerchant(merchantName);
                }
            }

            // Check flexible merchant patterns
            for (Map.Entry<Pattern, String> entry : FLEXIBLE_MERCHANT_PATTERNS.entrySet()) {
                if (entry.getKey().matcher(merchant).matches()) {
                    return entry.getValue();
                }
            }

            // Check for generic keywords in merchant name
            if (merchant.contains("FOOD")) {
                return "Food & Dining";
            }
            if (merchant.contains("GROC") || merchant.contains("MART")) {
                return "Groceries";
            }
        }
        if (upperDesc.startsWith("POS/")) {
            return "Shopping";
        }
        if (upperDesc.startsWith("UPI/P2M/")) {
            return "Merchant Payment";
        }
        Optional<TransactionTypeRule> transferRule = TRANFER_RULES.stream().filter(a -> a.transactionType.equalsIgnoreCase(transactionType)).findFirst();
        if (transferRule.isPresent()) {
            for (String keyword : transferRule.get().keywords) {
                if (upperDesc.contains(keyword.toUpperCase())) {
                    return transferRule.get().category;
                }
            }
        }
        // 5. Final fallback based on transaction type
        if (upperTransactionType.equals("CREDIT")) {
            return "Income";
        }
        if (KNOWN_MISC_PATTERNS.stream().anyMatch(upperDesc::contains)) {
            return "CHARGES";
        }
        return "Miscellaneous";
    }

    private String getCategoryForMerchant(String merchantName) {
        return switch (merchantName) {
            case "SWIGGY", "ZOMATO" -> "Food & Dining";
            case "UBER", "OLA", "RAPIDO" -> "Transportation";
            case "BIGBASKET", "GROFERS", "BLINKIT", "DMART" -> "Groceries";
            default -> "Merchant Payment";
        };
    }

    private String extractMerchantName(String description) {
        // Pattern for POS transactions: POS/MerchantName/...
        Matcher posMatcher = Pattern.compile("POS/([^/]+)/").matcher(description);
        if (posMatcher.find()) {
            return posMatcher.group(1).trim();
        }

        // Pattern for UPI/P2M transactions: UPI/P2M/.../MerchantName/...
        Matcher upiMatcher = Pattern.compile("UPI/P2M/\\d+/([^/]+)/").matcher(description);
        if (upiMatcher.find()) {
            return upiMatcher.group(1).trim();
        }

        // Pattern for ECOM PUR transactions
        Matcher ecomMatcher = Pattern.compile("ECOM PUR/([^/]+)/").matcher(description);
        if (ecomMatcher.find()) {
            return ecomMatcher.group(1).trim();
        }

        return null;
    }

    private static class TransactionTypeRule {
        String transactionType;
        List<String> keywords;
        String category;

        TransactionTypeRule(String transactionType, List<String> keywords, String category) {
            this.transactionType = transactionType.toUpperCase();
            this.keywords = keywords.stream().map(String::toUpperCase).collect(Collectors.toList());
            this.category = category;
        }
    }

    private static class CategoryRule {
        List<String> keywords;
        String category;

        CategoryRule(List<String> keywords, String category) {
            this.keywords = keywords.stream().map(String::toUpperCase).collect(Collectors.toList());
            this.category = category;
        }
    }
}