package me.dupreez.thesisPoC.redesign.domain.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import me.dupreez.thesisPoC.redesign.domain.Decomposition;
import me.dupreez.thesisPoC.redesign.domain.Functionality;
import me.dupreez.thesisPoC.redesign.domain.FunctionalityRedesign;
import me.dupreez.thesisPoC.redesign.domain.LocalTransaction;

import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CouplingMetric.class, name = Metric.MetricType.COUPLING),
        @JsonSubTypes.Type(value = SystemComplexityMetric.class, name = Metric.MetricType.SYSTEM_COMPLEXITY),
        @JsonSubTypes.Type(value = FunctionalityComplexityMetric.class, name = Metric.MetricType.FUNCTIONALITY_COMPLEXITY),
        @JsonSubTypes.Type(value = InconsistencyComplexityMetric.class, name = Metric.MetricType.INCONSISTENCY_COMPLEXITY)
})
public abstract class Metric<Type> {
    protected float value;
    protected Map<Integer, Float> ltToMetricValueMap;

    public Map<Integer, Float> getLtToMetricValueMap() { return ltToMetricValueMap; }

    public void setLtToMetricValueMap(Map<Integer, Float> ltToMetricValueMap) { this.ltToMetricValueMap = ltToMetricValueMap; }

    // Some metrics might not have metrics for functionalities
    public abstract void calculateMetric(Decomposition decomposition, Functionality functionality) throws Exception;

    // Some metrics might not have metrics for functionalities redesigns
    public abstract void calculateMetric(Decomposition decomposition, Functionality functionality, FunctionalityRedesign functionalityRedesign) throws Exception;

    // Some metrics might not have metrics for local transactions
    public abstract float calculateForLocalTransaction(Decomposition decomposition, Functionality functionality, LocalTransaction lt);

    @JsonIgnore
    public abstract String getType();

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public void calculateMetric(Decomposition decomposition) {
    }

    public static class MetricType {
        public static final String COUPLING = "Coupling";
        public static final String SYSTEM_COMPLEXITY = "System Complexity";
        public static final String FUNCTIONALITY_COMPLEXITY = "Functionality Complexity";
        public static final String INCONSISTENCY_COMPLEXITY = "Inconsistency Complexity";
    }
}