package me.dupreez.thesisPoC.redesign.utils;

import me.dupreez.thesisPoC.redesign.domain.LocalTransaction;

public class DataPoint {

    private float a;
    private float b;
    private float c;

    public DataPoint() {}

    public DataPoint(float a, float b, float c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public String toString() {
        return String.format("%.3f %.3f %.3f", a, b, c);
    }

    public float getA() {
        return a;
    }

    public float getB() {
        return b;
    }

    public float getC() {
        return c;
    }

    public void setA(float a) {
        this.a = a;
    }

    public void setB(float b) {
        this.b = b;
    }

    public void setC(float c) {
        this.c = c;
    }
}
