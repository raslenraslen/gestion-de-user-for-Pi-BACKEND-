package com.gamax.userservice.enums;

import lombok.Getter;

@Getter
public enum BanDuration {
    TEMPORARY_7_DAYS(7, "Banissement 7 jours"),
    TEMPORARY_30_DAYS(30, "Banissement 30 jours"),
    PERMANENT(-1, "Banissement définitif");

    // Getters
    private final int days;
    private final String description;

    BanDuration(int days, String description) {
        this.days = days;
        this.description = description;
    }

}