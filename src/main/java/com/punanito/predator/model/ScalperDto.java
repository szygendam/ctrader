package com.punanito.predator.model;

import java.math.BigDecimal;

public class ScalperDto {
    private String operation;
    private BigDecimal tp;
    private BigDecimal sl;

    public ScalperDto(String operation) {
        this.operation = operation;
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
}
