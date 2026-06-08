package dev.zm.rankup.util;

import java.text.DecimalFormat;

public final class NumberFormatter {

    private static final DecimalFormat FULL_FORMAT = new DecimalFormat("#,##0.##");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0");
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};

    private NumberFormatter() {
    }

    public static String format(double number) {
        return FULL_FORMAT.format(number);
    }

    public static String formatShort(double number) {
        if (number < 1000) {
            return format(number);
        }
        int index = 0;
        double value = number;
        while (value >= 1000 && index < SUFFIXES.length - 1) {
            value /= 1000;
            index++;
        }
        // Use PERCENT_FORMAT for keeping one decimal if it's not a whole number,
        // or just format normally, here we just use FULL_FORMAT to avoid trailing zeros
        return FULL_FORMAT.format(value) + SUFFIXES[index];
    }

    public static String formatPercentage(double value) {
        return PERCENT_FORMAT.format(value);
    }
}
