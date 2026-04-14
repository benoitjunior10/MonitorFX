/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.monitorfx.util;

/**
 *
 * @author HP
 */

import java.time.Duration;

/**
 * Utilitaires de formatage.
 */
public final class Formatters {

    private static final long GIGABYTE = 1024L * 1024L * 1024L;

    private Formatters() {
    }

    public static String formatBytesToGigabytes(long bytes) {
        double value = (double) bytes / GIGABYTE;
        return String.format("%.2f Go", value);
    }

    public static String formatPercent(double ratio) {
        return String.format("%.1f %%", ratio * 100.0);
    }

    public static String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String formatUptime(long uptimeSeconds) {
        long days = uptimeSeconds / 86_400;
        long hours = (uptimeSeconds % 86_400) / 3_600;
        long minutes = (uptimeSeconds % 3_600) / 60;
        long seconds = uptimeSeconds % 60;

        if (days > 0) {
            return String.format("%dj %02dh %02dm %02ds", days, hours, minutes, seconds);
        }
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    public static String formatBooleanState(boolean value) {
        return value ? "Actif" : "Terminé";
    }
}

