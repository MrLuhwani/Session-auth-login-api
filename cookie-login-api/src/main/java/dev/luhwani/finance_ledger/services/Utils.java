package dev.luhwani.finance_ledger.services;

import java.util.regex.Pattern;

public final class Utils {

    private static final Pattern pattern = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private Utils() {
    }

    public static boolean validEmail(String email) {
        return pattern.matcher(email).matches();
    }
}
