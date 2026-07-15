package dev.ipseucz.koraquest.model;

import java.util.Locale;

public enum Difficulty {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    private final String key;

    Difficulty(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Difficulty from(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Difficulty is missing");
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "easy", "de", "dễ" -> EASY;
            case "medium", "tb", "trungbinh", "trung_binh", "trung bình" -> MEDIUM;
            case "hard", "kho", "khó" -> HARD;
            default -> throw new IllegalArgumentException("Unknown difficulty: " + input);
        };
    }
}
