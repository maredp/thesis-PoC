package me.dupreez.thesisPoC.redesign.domain.metrics;

import me.dupreez.thesisPoC.redesign.domain.Decomposition;
import me.dupreez.thesisPoC.redesign.utils.FunctionalityType;
import me.dupreez.thesisPoC.redesign.utils.LocalTransactionTypes;
import me.dupreez.thesisPoC.redesign.utils.Utils;
import me.dupreez.thesisPoC.redesign.domain.Cluster;
import me.dupreez.thesisPoC.redesign.domain.Functionality;
import me.dupreez.thesisPoC.redesign.domain.FunctionalityRedesign;
import me.dupreez.thesisPoC.redesign.domain.LocalTransaction;
import me.dupreez.thesisPoC.redesign.domain.Access;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FunctionalityComplexityMetric extends Metric<Integer> {
    public String getType() {
        return MetricType.FUNCTIONALITY_COMPLEXITY;
    }

    // Decomposition Metric
    public void calculateMetric(Decomposition decomposition) {}

    // Functionality Metric
    public void calculateMetric(Decomposition decomposition, Functionality functionality) {}

    public void calculateMetric(
            Decomposition decomposition,
            Functionality functionality,
            FunctionalityRedesign functionalityRedesign
    ){
        this.value = 0;
        this.ltToMetricValueMap = new HashMap<>();
        for (LocalTransaction lt : functionalityRedesign.getRedesign()) {
            float result = calculateForLocalTransaction(decomposition, functionality, lt);
            this.ltToMetricValueMap.put(lt.getId(), result);
            this.value += result;
        }
    }

    public float calculateForLocalTransaction(
            Decomposition decomposition,
            Functionality functionality,
            LocalTransaction lt
    ) {
        if(functionality.getType() != FunctionalityType.SAGA) {
            return 0;
        }

        Map<String, Set<Cluster>> functionalitiesClusters = decomposition.getFunctionalitiesClusters();
        float result = 0;
        for(Access access : lt.getClusterAccesses()) {
            short entity = access.getEntityID();
            byte mode = access.getMode();

            // Functionality complexity cost of write
            if(mode >= 2 && lt.getType() == LocalTransactionTypes.COMPENSATABLE) { // 2 -> W, 3 -> RW
                result++;
            }

            // Functionality complexity cost of read
            if (mode != 2) { // 2 -> W - we want all the reads
                for (Functionality otherFunctionality : decomposition.getFunctionalities().values()) {
                    if (!otherFunctionality.getName().equals(functionality.getName()) &&
                            otherFunctionality.containsEntity(entity) &&
                            otherFunctionality.getEntities().get(entity) >= 2 &&
                            functionalitiesClusters.get(otherFunctionality.getName()).size() > 1) {
                        result++;
                    }
                }
            }
        }

        return result;
    }
}
