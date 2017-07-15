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

public enum TerminalResponse {

    UNKNOWN(""),
    CONNECTION_EXISTS("Connection already exists"),
    NO_CONNECTION("No such connection"),
    OK("OK"),
    SSH_STARTED("SSH started"),
    SSH_STOPPED("SSH stopped"),
    SSH_START_FAIL("SSH could not be started"),
    VNC_STARTED("VNC started"),
    VNC_STOPPED("VNC stopped"),
    VNC_START_FAIL("VNC could not be started");

    public String responseText;

    TerminalResponse(String response) {
        responseText = response;
    }

    public static TerminalResponse getResponse(String checkStr) {
        for (TerminalResponse terminalResponse : TerminalResponse.values()) {
            if (terminalResponse == UNKNOWN) continue;
            String left = terminalResponse.responseText.toLowerCase();
            String right = checkStr.toLowerCase();
            boolean check = right.contains(left);
            if (terminalResponse.responseText.equalsIgnoreCase(checkStr) || check) {
                return terminalResponse;
            }
        }
        return UNKNOWN;
    }
}
