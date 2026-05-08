package com.punanito.predator.model;

import java.math.BigDecimal;

public class ScalperDto {
    private String operation;
    private String reason;
    private BigDecimal tp;
    private BigDecimal sl;

    public ScalperDto(String operation) {
        this.operation = operation;
    }

    public ScalperDto(String operation,String reason) {
        this.operation = operation;
        this.reason = reason;
    }

    public String getOperation() {

        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public BigDecimal getTp() {
        return tp;
    }

    public void setTp(BigDecimal tp) {
        this.tp = tp;
    }

    public BigDecimal getSl() {
        return sl;
    }

    public void setSl(BigDecimal sl) {
        this.sl = sl;
    }

    public String getReason() {
        return reason;
    }
}
