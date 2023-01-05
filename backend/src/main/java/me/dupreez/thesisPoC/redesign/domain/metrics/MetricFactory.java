package me.dupreez.thesisPoC.redesign.domain.metrics;

import static me.dupreez.thesisPoC.redesign.domain.metrics.Metric.MetricType.*;

public class MetricFactory {
    private static MetricFactory factory = null;

    public static MetricFactory getFactory() {
        if (factory == null)
            factory = new MetricFactory();
        return factory;
    }

    public Metric getMetric(String metricType) {
        switch (metricType) {
            case COUPLING:
                return new CouplingMetric();
            case SYSTEM_COMPLEXITY:
                return new SystemComplexityMetric();
            case FUNCTIONALITY_COMPLEXITY:
                return new FunctionalityComplexityMetric();
            case INCONSISTENCY_COMPLEXITY:
                return new InconsistencyComplexityMetric();
            default:
                throw new RuntimeException("The type \"" + metricType + "\" is not a valid metric.");
        }
    }
}
