package me.dupreez.thesisPoC.redesign.domain.metrics;

import me.dupreez.thesisPoC.redesign.domain.Decomposition;
import me.dupreez.thesisPoC.redesign.domain.Functionality;
import me.dupreez.thesisPoC.redesign.domain.FunctionalityRedesign;
import me.dupreez.thesisPoC.redesign.domain.LocalTransaction;
import me.dupreez.thesisPoC.redesign.utils.FunctionalityType;

import java.util.Set;

public class InconsistencyComplexityMetric extends Metric<Integer> {
    public String getType() {
        return MetricType.INCONSISTENCY_COMPLEXITY;
    }

    // Decomposition Metric
    public void calculateMetric(Decomposition decomposition) {}

    // Functionality Metric
    public void calculateMetric(Decomposition decomposition, Functionality functionality) {}

    // Functionality Redesign Metric
    public void calculateMetric(Decomposition decomposition, Functionality functionality, FunctionalityRedesign functionalityRedesign) {
        if(functionality.getType() != FunctionalityType.QUERY)
            return;

        this.value = 0;

        Set<Short> entitiesRead = functionality.entitiesTouchedInAGivenMode((byte) 1);

        for (Functionality otherFunctionality : decomposition.getFunctionalities().values()) {
            if (!otherFunctionality.getName().equals(functionality.getName()) &&
                    otherFunctionality.getType() == FunctionalityType.SAGA){

                Set<Short> entitiesWritten = otherFunctionality.entitiesTouchedInAGivenMode((byte) 2);
                entitiesWritten.retainAll(entitiesRead);
                Set<Short> clustersInCommon = otherFunctionality.clustersOfGivenEntities(entitiesWritten);

                if(clustersInCommon.size() > 1)
                    this.value += clustersInCommon.size();
            }
        }
    }

    public float calculateForLocalTransaction(Decomposition decomposition, Functionality functionality, LocalTransaction lt) {
        return 0;
    }
}