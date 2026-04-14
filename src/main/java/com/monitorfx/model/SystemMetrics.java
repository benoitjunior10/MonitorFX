/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.monitorfx.model;

/**
 *
 * @author HP
 * Modèle des métriques système.
 */

public record SystemMetrics(
        long totalMemoryBytes,
        long usedMemoryBytes,
        long freeMemoryBytes,
        double memoryUsageRatio,
        double cpuUsageRatio,
        int logicalProcessors,
        long uptimeSeconds,
        String operatingSystem,
        String operatingSystemVersion,
        String architecture
) {
}
