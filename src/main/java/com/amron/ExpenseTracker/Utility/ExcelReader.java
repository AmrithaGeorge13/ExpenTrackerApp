package com.amron.ExpenseTracker.Utility;

import com.amron.ExpenseTracker.Model.BankStatementConfig;
import com.amron.ExpenseTracker.Model.Categories;
import com.amron.ExpenseTracker.Model.DailyExpense;
import com.amron.ExpenseTracker.Service.BankStatementConfigService;
import com.amron.ExpenseTracker.Service.OllamaCategorizationService;
import com.amron.ExpenseTracker.Service.TransactionCategorizerService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Component
public class ExcelReader {
    private static final Map<String, String> BANK_KEYWORDS;

    static {
        BANK_KEYWORDS = new HashMap<>();
        BANK_KEYWORDS.put("axis", "axis");
        BANK_KEYWORDS.put("hdfc", "hdfc");
        BANK_KEYWORDS.put("sbi", "sbi");
        BANK_KEYWORDS.put("icici", "icici");
        BANK_KEYWORDS.put("federal", "federal");
        BANK_KEYWORDS.put("sodexo", "sodexo");
        BANK_KEYWORDS.put("pnb", "pnb");
    }

    private final OllamaCategorizationService categorizationService;
    private final TransactionCategorizerService transactionCategorizerService;
    Logger logger = Logger.getLogger(ExcelReader.class.getName());

    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return ""; // Handle null cells
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "Unknown Cell Type";
        }
    }

    // Utility method to check if the row is invalid or incomplete
    private static boolean isRowInvalid(Row row, int startCol, int expectedColumnCount) {
        if (row == null) {
            return true; // Row is null
        }

        // Check if critical cells (e.g., first cell) are empty
        Cell firstCell = row.getCell(startCol);
        return firstCell == null || firstCell.getCellType() == CellType.BLANK;
    }

    private static int getIndex(String[] columns, String... targets) {
        OptionalInt indexOpt = IntStream.range(0, columns.length).filter(i -> Arrays.stream(targets).anyMatch(target -> target.equalsIgnoreCase(columns[i]))).findFirst();

        if (indexOpt.isPresent()) {
            return indexOpt.getAsInt();
        } else {
            System.out.println("Column not found");
            return -1;
        }
    }

    public String getTransactionType(String debitValue, String creditValue) {
        String transactionType = "";
        boolean hasDebit = !debitValue.isEmpty() && Double.parseDouble(debitValue) > 0;
        boolean hasCredit = !creditValue.isEmpty() && Double.parseDouble(creditValue) > 0;
        if (hasDebit && !hasCredit) {
            return "DEBIT";
        } else if (hasCredit && !hasDebit) {
            return "CREDIT";
        } else if (!hasDebit && !hasCredit) {
            throw new IllegalArgumentException("Invalid row: both debit and credit fields are zero or empty");
        }
        return transactionType;
    }

    public List<DailyExpense> readDailyExpensesFromExcel(String folderPath, BankStatementConfigService bankStatementConfigService, List<Categories> allCategories) throws IOException {
        List<DailyExpense> dailyExpenses = new ArrayList<>();
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String bankName = getBankName(fileName);
                String userName = fileName.split("_")[0];
                System.out.println(fileName);
                BankStatementConfig bankStatementConfig = bankStatementConfigService.getBankStatementConfig(bankName);
                dailyExpenses.addAll(readDailyExpensesForEachExcel(new File(file.getAbsolutePath()), bankStatementConfig, userName, bankName));
            }
        }
        return dailyExpenses;
    }

    /**
     * Method to determine bank name
     *
     * @param fileName: name of file
     * @return bank name
     */
    public String getBankName(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        for (Map.Entry<String, String> entry : BANK_KEYWORDS.entrySet()) {
            if (lowerCaseFileName.contains(entry.getKey())) {
                logger.info("Bank Name found in file: " + entry.getValue());
                return entry.getValue();
            }
        }
        logger.info("Bank Name not found");
        return "";
    }

    private List<DailyExpense> readDailyExpensesForEachExcel(File file, BankStatementConfig bankStatementConfig, String userName, String bankName) throws IOException {
        List<DailyExpense> dailyExpenses = new ArrayList<>();
        FileInputStream fileInputStream = new FileInputStream(file);
        Workbook workbook = null;
        if (file.getName().toLowerCase().endsWith(".xls")) {
            workbook = new HSSFWorkbook(fileInputStream);
        } else if (file.getName().toLowerCase().endsWith(".xlsx")) {
            workbook = new XSSFWorkbook(fileInputStream);
        }
        Sheet sheet = workbook.getSheetAt(0);
        int startRow = bankStatementConfig.getStartRow();
        int startCol = bankStatementConfig.getStartCol();
        int numberOfColumns = bankStatementConfig.getNumberOfColumns();
        // Split sheet_columns by comma and map the columns to the DailyExpense fields
        String[] columns = bankStatementConfig.getSheetColumns().split(",");
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            // Check if row is empty or has insufficient cells
            if (row.getPhysicalNumberOfCells() < numberOfColumns) {
                break;
            }
            if (isRowInvalid(row, startCol, numberOfColumns)) {
                continue;
            }
            // Get the cell index for the date column and retrieve the date value
            int dateColumnIndex = getDateColumNo(columns) + (startCol - 1);
            Cell dateCell = row.getCell(dateColumnIndex);
            String dateValue = getCellValue(dateCell);
            if (dateValue.trim().isEmpty()) break;
            LocalDate transactionDate = DateParser.parseDate(dateValue);

            // Get the cell index for the categories column and retrieve the category value
            int categoriesColumnIndex = getCategoriesColumnNo(columns) + (startCol - 1);
            Cell categoriesCell = row.getCell(categoriesColumnIndex);
            String rawCategories = getCellValue(categoriesCell);

            // Get the cell index for the debit and credit columns and retrieve the respective values
            int debitColumnIndex = getDebitColumnNo(columns) + (startCol - 1);
            int creditColumnIndex = getCreditColumnNo(columns) + (startCol - 1);

            String debitValue = getCellValue(row.getCell(debitColumnIndex)).trim().replaceAll(",", "");
            String creditValue = getCellValue(row.getCell(creditColumnIndex)).trim().replaceAll(",", "");
            String transactionType = getTransactionType(debitValue, creditValue);
            // Determine the actual amount based on whether the debit or credit value is present
            Double actualAmount = transactionType.equalsIgnoreCase("DEBIT") ? Double.valueOf(debitValue) : Double.valueOf(creditValue);

            String categories = transactionCategorizerService.categorizeTransaction(rawCategories, transactionType);
            categories = overrideCategory(rawCategories, categories);
            System.out.println(dailyExpenses);
            DailyExpense dailyExpense = new DailyExpense();
            dailyExpense.setTransactionType(transactionType);
            dailyExpense.setDate(transactionDate);
            dailyExpense.setRawCategories(rawCategories);
            dailyExpense.setCategories(categories);
            dailyExpense.setActualAmount(actualAmount);
            dailyExpense.setWeekNum(deriveWeekNum(transactionDate));
            dailyExpense.setMonthYear(deriveMonthYear(transactionDate));
            dailyExpense.setAccount(userName + " " + bankName);
            dailyExpense.setQuarterYear(deriveQuaterYear(transactionDate));
            dailyExpenses.add(dailyExpense);
        }
        return dailyExpenses;
    }

    private String deriveQuaterYear(LocalDate transactionDate) {
        int quater = (transactionDate.getMonthValue() - 1) / 3 + 1;
        return "Q" + quater + " " + transactionDate.getYear();
    }

    private String deriveMonthYear(LocalDate transactionDate) {
        return transactionDate.getMonth() + " " + transactionDate.getYear();
    }

    private String deriveWeekNum(LocalDate transactionDate) {
        int weekNumber = transactionDate.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        return "Week " + weekNumber;
    }

    private int getDateColumNo(String[] columns) {
        String target1 = "Transaction Date";
        String target2 = "Tran Date";
        String target3 = "Txn Date";
        String target4 = "Date";
        return getIndex(columns, target1, target2, target3, target4);
    }

    private int getCategoriesColumnNo(String[] columns) {
        String target1 = "Description";
        String target2 = "Transaction Remarks";
        String target3 = "Particulars";
        String taregt4 = "Narration";
        String taregt5 = "Details";
        return getIndex(columns, target1, target2, target3, taregt4, taregt5);
    }

    private int getCreditColumnNo(String[] columns) {
        String target1 = "CR";
        String target2 = "Deposit Amt.";
        String target3 = "Deposit Amount (INR )";
        String target4 = "Deposit";
        String target5 = "Credit";
        String target6 = "Deposits";
        return getIndex(columns, target1, target2, target3, target4, target5, target6);
    }

    private int getDebitColumnNo(String[] columns) {
        String target1 = "DR";
        String target2 = "Withdrawal Amt.";
        String target3 = "Withdrawal Amount (INR )";
        String target4 = "Withdrawal";
        String target5 = "Debit";
        String target6 = "Withdrawals";
        return getIndex(columns, target1, target2, target3, target4, target5, target6);
    }

    private String overrideCategory(String rawCategories, String category) {
        if ("UPI-KRISHNAPRIYA  A-KRISHNAMOHANKP12-1@OKSBI-UBIN0934038-626327075512-PAYMENT FROM PHONE".equalsIgnoreCase(rawCategories) ||
                "UPI-KRISHNAPRIYA A-KRISHNAMOHANKP12-3@OKICICI-UTIB0005145-105748832320-UPI".equalsIgnoreCase(rawCategories)) {

            return "Travel";
        }
        if ("FN/SHP/Qnu4aAmcsfrO3S/FEDERAL BANK LTD_RAZORPAY/,1".equalsIgnoreCase(rawCategories)) {

            return "Credit Card Payment";
        }
        return category;
    }

}
