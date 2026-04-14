/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.monitorfx.model;

/**
 *
 * @author HP
 * Modèle de représentation d'un processus applicatif.
 */

public record ProcessRecord(
        long pid,
        String name,
        String user,
        String startTime,
        String cpuDuration,
        String command,
        boolean alive,
        long cpuDurationMillis
) {
}

