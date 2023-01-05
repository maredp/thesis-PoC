package me.dupreez.thesisPoC.redesign.domain.metrics;

import me.dupreez.thesisPoC.redesign.domain.LocalTransaction;
import me.dupreez.thesisPoC.redesign.domain.Decomposition;
import me.dupreez.thesisPoC.redesign.domain.Functionality;
import me.dupreez.thesisPoC.redesign.domain.FunctionalityRedesign;

import java.util.List;
import java.util.stream.Collectors;

public class CouplingMetric extends Metric<Float> {
    public String getType() {
        return MetricType.COUPLING;
    }

    public void calculateMetric(Decomposition decomposition) {}

    public void calculateMetric(Decomposition decomposition, Functionality functionality) {}

    public void calculateMetric(Decomposition decomposition, Functionality functionality, FunctionalityRedesign functionalityRedesign) throws Exception {
        this.value = 0;
        for (LocalTransaction lt : functionalityRedesign.getRedesign()) {
            for (Integer ri : lt.getRemoteInvocations()) {
                LocalTransaction invocatedLT = null;
                for (LocalTransaction lt1 : functionalityRedesign.getRedesign()) {
                    if (lt1.getId() == ri) {
                        invocatedLT = lt1;
                        break;
                    }
                }
                if (invocatedLT == null) {
                    throw new Exception(String.format("LT with id %s cannot be found", ri));
                } else if (invocatedLT.getClusterID() != lt.getClusterID()) {
                    this.value++;
                }
            }

//            this.value += calculateForLocalTransaction(decomposition, functionality, lt);
        }
    }

    public float calculateForLocalTransaction(Decomposition decomposition, Functionality functionality, LocalTransaction lt) {
//        return lt.getRemoteInvocations().isEmpty() ? 0 : 1;
//        return lt.getRemoteInvocations().size();
        return 0;
    }
}