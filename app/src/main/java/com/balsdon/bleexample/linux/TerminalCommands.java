package com.balsdon.bleexample.linux;

/*************************************************************************
 *
 * QUINTIN BALSDON CONFIDENTIAL
 * ____________________________
 *
 *  Quintin Balsdon 
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Quintin Balsdon and other contributors,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Quintin Balsdon
 * and its suppliers and may be covered by U.K. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Quintin Balsdon.
 */

public class TerminalCommands {
    public static final String GET_IP = "hostname -I";
    public static final String LIST_WIFI = "sudo iw dev wlan0 scan | grep SSID";
    public static final String REBOOT = "sudo reboot";
    public static final String SHUTDOWN = "sudo poweroff";
    public static final String STAT_TEMP = "sudo vcgencmd measure_temp";
    public static final String STAT_VOLATGE = "sudo vcgencmd measure_volts";
}
