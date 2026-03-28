package com.punanito.predator.model;

public class ScalperDto {
    private String operation;
    private double tp;
    private double sl;

    public ScalperDto(String operation) {
        this.operation = operation;
    }

    public String getOperation() {

        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public double getTp() {

        return tp;
    }

    public void setTp(double tp) {
        this.tp = tp;
    }

    public double getSl() {
        return sl;
    }

    public void setSl(double sl) {
        this.sl = sl;
    }
}
