package dev.zm.rankup.util;

public final class ProgressBar {

    private ProgressBar() {
    }

    public static String create(double progress, int length, String filledChar, String emptyChar, String filledColor, String emptyColor) {
        int filledLength = (int) Math.round(progress * length);
        if (filledLength > length) filledLength = length;
        if (filledLength < 0) filledLength = 0;

        int emptyLength = length - filledLength;

        return filledColor + String.valueOf(filledChar).repeat(filledLength) +
               emptyColor + String.valueOf(emptyChar).repeat(emptyLength);
    }

    public static String createDefault(double progress) {
        return createDefault(progress, 20, "■", "□", "<green>", "<gray>");
    }

    public static String createDefault(double progress, int length, String filledChar, String emptyChar, String filledColor, String emptyColor) {
        return create(progress, length, filledChar, emptyChar, filledColor, emptyColor);
    }
}
