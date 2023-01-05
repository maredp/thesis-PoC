package me.dupreez.thesisPoC.redesign.utils;

import me.dupreez.thesisPoC.redesign.domain.LocalTransaction;

public class PairRI {

    private final LocalTransaction a;
    private final LocalTransaction b;

    public PairRI(LocalTransaction a, LocalTransaction b) {
        this.a = a;
        this.b = b;
    }

    public String toString() {
        return "Pair[" + a + "," + b + "]";
    }

    public LocalTransaction getA() {
        return a;
    }

    public LocalTransaction getB() {
        return b;
    }
}
