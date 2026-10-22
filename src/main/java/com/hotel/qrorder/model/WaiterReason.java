package com.hotel.qrorder.model;

public enum WaiterReason {
    GENERAL("Waiter ko bulaya hai"),
    WATER("Paani chahiye"),
    BILL("Bill chahiye"),
    HELP("Madad chahiye");

    private final String text;
    WaiterReason(String text) { this.text = text; }
    public String getText() { return text; }
}
