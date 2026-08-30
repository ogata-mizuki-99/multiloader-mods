package com.ogatamizuki.nickname;

import java.util.Optional;

public final class NicknameValidation {
    public static final int MAX_LENGTH = 32;

    private NicknameValidation() {
    }

    public record Result(boolean accepted, String sanitized, Optional<String> errorKey, int errorArg) {
        static Result ok(String sanitized) {
            return new Result(true, sanitized, Optional.empty(), 0);
        }

        static Result reject(String errorKey) {
            return new Result(false, "", Optional.of(errorKey), 0);
        }

        static Result reject(String errorKey, int errorArg) {
            return new Result(false, "", Optional.of(errorKey), errorArg);
        }
    }

    public static Result validate(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Result.ok("");
        }
        if (trimmed.length() > MAX_LENGTH) {
            return Result.reject("nickname.message.too_long", MAX_LENGTH);
        }
        if (containsFormattingCodes(trimmed)) {
            return Result.reject("nickname.message.no_formatting");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isISOControl(c)) {
                return Result.reject("nickname.message.invalid");
            }
        }
        return Result.ok(trimmed);
    }

    private static boolean containsFormattingCodes(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\u00a7' || c == '§') {
                return true;
            }
        }
        return false;
    }
}
