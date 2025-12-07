package com.punanito.ctraderbridge.model;

public class AccountRequest {
    private double balance;
    public AccountRequest() {
    }

    public AccountRequest(double accountBalance) {
        this.balance = accountBalance;
    }

    public double getAccountBalance() {
        return balance;
    }
    public void setAccountBalance(double accountBalance) {
        this.balance = accountBalance;
    }

}
