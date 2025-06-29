package com.amron.ExpenseTracker.Utility;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class DateParser {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = new ArrayList<>();

    static {
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("dd/MM/yy"));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("dd,MM,yyyy"));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy"));

        // Add more formats as needed
    }

    public static LocalDate parseDate(String dateString) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateString, formatter);
                return date;
            } catch (DateTimeParseException e) {
                // Continue to the next format
            }
        }
        throw new IllegalArgumentException("Unparseable date: " + dateString);
    }
}
