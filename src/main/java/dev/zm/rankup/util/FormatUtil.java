package dev.zm.rankup.util;

import java.text.DecimalFormat;

public final class FormatUtil {

    private static final DecimalFormat FULL_FORMAT = new DecimalFormat("#,##0.##");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0");
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};

    private FormatUtil() {
    }

    // Number Formatter Methods

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
        return FULL_FORMAT.format(value) + SUFFIXES[index];
    }

    public static String formatPercentage(double value) {
        return PERCENT_FORMAT.format(value);
    }

    // Progress Bar Methods

    public static String createProgressBar(double progress, int length, String filledChar, String emptyChar, String filledColor, String emptyColor) {
        int filledLength = (int) Math.round(progress * length);
        if (filledLength > length) filledLength = length;
        if (filledLength < 0) filledLength = 0;

        int emptyLength = length - filledLength;

        return filledColor + String.valueOf(filledChar).repeat(filledLength) +
               emptyColor + String.valueOf(emptyChar).repeat(emptyLength);
    }

    public static String createDefaultProgressBar(double progress) {
        return createDefaultProgressBar(progress, 20, "■", "□", "<green>", "<gray>");
    }

    public static String createDefaultProgressBar(double progress, int length, String filledChar, String emptyChar, String filledColor, String emptyColor) {
        return createProgressBar(progress, length, filledChar, emptyChar, filledColor, emptyColor);
    }
}
