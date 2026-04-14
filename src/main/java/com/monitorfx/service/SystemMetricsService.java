/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.monitorfx.service;

/**
 *
 * @author HP
 */

import com.monitorfx.model.SystemMetrics;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * Surveillance RAM + CPU.
 */
public class SystemMetricsService {

    private final OperatingSystemMXBean operatingSystemMXBean;
    private final java.lang.management.OperatingSystemMXBean baseOperatingSystemMXBean;
    private final RuntimeMXBean runtimeMXBean;

    public SystemMetricsService() {
        this.operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.baseOperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    }

    public SystemMetrics captureSnapshot() {
        long totalMemory = operatingSystemMXBean.getTotalMemorySize();
        long freeMemory = operatingSystemMXBean.getFreeMemorySize();
        long usedMemory = Math.max(0L, totalMemory - freeMemory);

        double memoryUsageRatio = totalMemory == 0 ? 0.0 : (double) usedMemory / totalMemory;
        double cpuUsageRatio = Math.max(0.0, operatingSystemMXBean.getCpuLoad());

        return new SystemMetrics(
                totalMemory,
                usedMemory,
                freeMemory,
                memoryUsageRatio,
                cpuUsageRatio,
                baseOperatingSystemMXBean.getAvailableProcessors(),
                runtimeMXBean.getUptime() / 1000,
                baseOperatingSystemMXBean.getName(),
                baseOperatingSystemMXBean.getVersion(),
                baseOperatingSystemMXBean.getArch()
        );
    }
}

