package me.dupreez.thesisPoC.redesign.service;

import me.dupreez.thesisPoC.redesign.domain.Decomposition;
import me.dupreez.thesisPoC.redesign.domain.metrics.*;
import me.dupreez.thesisPoC.redesign.utils.DataPoint;
import me.dupreez.thesisPoC.redesign.utils.FunctionalityType;
import me.dupreez.thesisPoC.redesign.domain.Functionality;
import me.dupreez.thesisPoC.redesign.domain.FunctionalityRedesign;
import me.dupreez.thesisPoC.redesign.domain.LocalTransaction;
import me.dupreez.thesisPoC.redesign.domain.Access;
import me.dupreez.thesisPoC.redesign.utils.PairRI;
import me.dupreez.thesisPoC.redesign.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

import static me.dupreez.thesisPoC.redesign.domain.metrics.Metric.MetricType.*;
import static me.dupreez.thesisPoC.redesign.domain.metrics.Metric.MetricType.INCONSISTENCY_COMPLEXITY;

public class RedesignService {

    private float funcComplexityWeight;
    private float systemComplexityWeight;
    private float queryComplexityWeight;
    private float invocationsWeight;
    private Decomposition decomposition;
    private final Functionality functionality;
    private List<FunctionalityRedesign> redesigns = new ArrayList<>();
    private Set<Integer> redesignsDoneForApplyingOperations = new HashSet<>();
    private Set<FunctionalityRedesign> outerRedesignsForApplyingOperations = new HashSet<>();
    public Decomposition getDecomposition() { return decomposition; }

    public Functionality getFunctionality() {
        return functionality;
    }

    public void addRedesigns(List<FunctionalityRedesign> redesigns) {
        List<FunctionalityRedesign> newList = new ArrayList<>(this.redesigns);
        newList.addAll(redesigns);
        this.redesigns = newList;
    }

    public RedesignService(
            Functionality functionality,
            float funcComplexityWeight,
            float systemComplexityWeight,
            float queryComplexityWeight,
            float invocationsWeight) {
        this.functionality = functionality;
        this.funcComplexityWeight = funcComplexityWeight;
        this.systemComplexityWeight = systemComplexityWeight;
        this.queryComplexityWeight = queryComplexityWeight;
        this.invocationsWeight = invocationsWeight;
    }

    public RedesignService(Decomposition decomposition, Functionality functionality) throws Exception {
        this.decomposition = decomposition;
        this.functionality = functionality;
        FunctionalityType functionalityType = functionality.defineFunctionalityType();
        if (functionalityType == FunctionalityType.QUERY) {
            this.queryComplexityWeight = 0.5f;
            this.invocationsWeight = 0.5f;
        } else {
            this.funcComplexityWeight = 0.33f;
            this.systemComplexityWeight = 0.33f;
            this.invocationsWeight = 0.33f;
        }
        scaleWeights();
    }

    private void scaleWeights() throws Exception {
        FunctionalityRedesign originalRedesign = functionality.getOriginalFunctionalityRedesign();
        originalRedesign.calculateMetrics(decomposition ,functionality);
        originalRedesign.setMetricsCalculated(true);
        originalRedesign.getMetrics().forEach(metric -> {
            switch (metric.getType()) {
                case Metric.MetricType.FUNCTIONALITY_COMPLEXITY:
                    funcComplexityWeight /= metric.getValue();
                    break;
                case Metric.MetricType.SYSTEM_COMPLEXITY:
                    systemComplexityWeight /= metric.getValue();
                    break;
                case Metric.MetricType.INCONSISTENCY_COMPLEXITY:
                    queryComplexityWeight /= metric.getValue();
                    break;
                case Metric.MetricType.COUPLING:
                    invocationsWeight /= metric.getValue();
                    break;
                default:
                    break;
            }
        });
    }

    Comparator<FunctionalityRedesign> redesignComparator = (r1, r2) -> {
        return Float.compare(sumRedesignMetrics(r1), sumRedesignMetrics(r2)); // TODO: check whether this ensures low complexity and high performance first
    };

    private FunctionalityRedesign findOptimalRedesign(List<FunctionalityRedesign> redesigns) {
        if (redesigns.isEmpty()) { return null; }
        FunctionalityRedesign optimalRedesign = redesigns.get(0);
        float optimalValue = sumRedesignMetrics(optimalRedesign);
        for (int i = 0; i < redesigns.size(); i++) {
            FunctionalityRedesign redesign = redesigns.get(i);
            float value = sumRedesignMetrics(redesign);
            if (value < optimalValue) {
                optimalRedesign = redesign;
                optimalValue = value;
            }
        }

        return optimalRedesign;
    }

    // Linear weighted sum method
    private float sumRedesignMetrics(FunctionalityRedesign redesign) {
        return redesign.getMetrics().stream().map(metric -> {
            float weight;
            switch (metric.getType()) {
                case Metric.MetricType.FUNCTIONALITY_COMPLEXITY:
                    weight = funcComplexityWeight;
                    break;
                case Metric.MetricType.SYSTEM_COMPLEXITY:
                    weight = systemComplexityWeight;
                    break;
                case Metric.MetricType.INCONSISTENCY_COMPLEXITY:
                    weight = queryComplexityWeight;
                    break;
                case Metric.MetricType.COUPLING:
                    weight = invocationsWeight;
                    break;
                default:
                    weight = 0f;
            }
            return weight * metric.getValue();
        }).reduce(0f, Float::sum);
    }

    public void resetFunctionality(FunctionalityRedesign initialRedesign) {
        LinkedHashSet<FunctionalityRedesign> newRedesigns = new LinkedHashSet<>();
        newRedesigns.add(functionality.getOriginalFunctionalityRedesign());
        if (initialRedesign != null) {
            newRedesigns.add(initialRedesign);
        }
        functionality.setFunctionalityRedesigns(newRedesigns);
        outerRedesignsForApplyingOperations = new HashSet<>();
        redesignsDoneForApplyingOperations = new HashSet<>();
    }

    public void doRedesign(
            boolean applyOperations,
            boolean characterizeLTs,
            boolean orchestration,
            boolean hashing,
            boolean saveSolutionSet,
            int limit,
            FunctionalityRedesign initialRedesign) throws Exception {
        FunctionalityRedesign bestRedesign;
        if (initialRedesign != null) {
            bestRedesign = initialRedesign;
        } else {
            bestRedesign = functionality.getOriginalFunctionalityRedesign();
        }
        redesigns.add(bestRedesign);

        // Apply the functionality redesign operations
        if (applyOperations) {
            List<FunctionalityRedesign> sagaRedesignsForApplyingOperations = createSagaRedesignsForApplyingOperations(hashing, saveSolutionSet, limit);
            addRedesigns(sagaRedesignsForApplyingOperations);
            bestRedesign = sagaRedesignsForApplyingOperations.get(0);

            if (!functionality.getFunctionalityRedesigns().contains(bestRedesign)) {
                bestRedesign.setName("Optimal for applying operations");
                functionality.getFunctionalityRedesigns().add(bestRedesign);
            }
            if (saveSolutionSet) {
                writeOuterRedesignMetricsToFile();
            }
        }

        // Characterize the local transactions by choosing pivot transactions
        if (characterizeLTs) {
            bestRedesign = createSagaRedesignsForLTsCharacterization().get(0);

            if (!functionality.getFunctionalityRedesigns().contains(bestRedesign)) {
                bestRedesign.setName("Optimal for LTs characterization");
                functionality.getFunctionalityRedesigns().add(bestRedesign);
            }
        }

        // In case of an orchestration, add an orchestrator to the clusters of the best redesign
        if (orchestration) {
            calculateAddedInvocationsForEachOrchestrationCluster();
        }
    }

    private void writeOuterRedesignMetricsToFile() throws Exception {
        List<DataPoint> dataPoints = new ArrayList<>();
        outerRedesignsForApplyingOperations.forEach(redesign -> {
            DataPoint dataPoint = new DataPoint();
            redesign.getMetrics().forEach(metric -> {
                switch (metric.getType()) {
                    case Metric.MetricType.FUNCTIONALITY_COMPLEXITY:
                        dataPoint.setA(metric.getValue());
                        break;
                    case Metric.MetricType.SYSTEM_COMPLEXITY:
                        dataPoint.setB(metric.getValue());
                        break;
                    case Metric.MetricType.COUPLING:
                        dataPoint.setC(metric.getValue());
                        break;
                    default:
                        break;
                }
            });
            dataPoints.add(dataPoint);
        });
        Utils.writeDataPointsToFile(dataPoints, functionality.getName());
    }

    public List<FunctionalityRedesign> createSagaRedesignsForApplyingOperations(
            boolean hashing,
            boolean saveSolutionSet,
            int limit
    ) throws Exception {
        // make all initial redesigns applicable for sequence change operations and
        // apply functionality redesign operations recursively
        List<FunctionalityRedesign> sagaRedesigns = new ArrayList<>(redesigns);
        for (FunctionalityRedesign initialRedesign : redesigns) {
            initialRedesign.getRedesign().forEach(lt->lt.setApplicableForSC(true));
            initialRedesign.setApplicableForSC(true);
            FunctionalityRedesign redesign;
            if (limit > 0) {
                redesign = redesignFunctionalityForApplyingOperationsFast(initialRedesign, hashing, saveSolutionSet, limit);
            } else {
                redesign = redesignFunctionalityForApplyingOperations(initialRedesign, hashing, saveSolutionSet);
            }

            if (redesign!=null) {
                sagaRedesigns.add(redesign);
            }
        }

        // calculate metrics
        calculateMetricsForRedesigns(sagaRedesigns);

        // order the redesigns by ascending complexity and coupling
        sagaRedesigns.sort(redesignComparator);

        return sagaRedesigns;
    }

    private FunctionalityRedesign redesignFunctionalityForApplyingOperations(
            FunctionalityRedesign initialRedesign,
            boolean hashing,
            boolean saveSolutionSet
    ) throws Exception {
        if (hashing) {
            if (redesignsDoneForApplyingOperations.contains(initialRedesign.hashCode())) {
                return null;
            }
            redesignsDoneForApplyingOperations.add(initialRedesign.hashCode());
        }

        List<FunctionalityRedesign> redesigns = applyOperations(initialRedesign);
        List<FunctionalityRedesign> recursiveRedesigns = new ArrayList<>();
        for (FunctionalityRedesign redesign : redesigns) {
            // calculate metrics for every redesign
            try {
                redesign.calculateMetrics(decomposition, functionality);
            } catch (Exception e) {
                System.out.println(String.format("Calculating metrics for redesign error"));
            }

            // recursively apply operations for every redesign
            FunctionalityRedesign recursiveRedesign = redesignFunctionalityForApplyingOperations(redesign, hashing, saveSolutionSet);
            if (recursiveRedesign!=null) {
                recursiveRedesigns.add(recursiveRedesign);
            } else {
                recursiveRedesigns.add(redesign);
                if (saveSolutionSet) {
                    outerRedesignsForApplyingOperations.add(redesign);
                }
            }
        }

        return findOptimalRedesign(recursiveRedesigns);
    }

    private FunctionalityRedesign redesignFunctionalityForApplyingOperationsFast(
            FunctionalityRedesign initialRedesign,
            boolean hashing,
            boolean saveSolutionSet,
            int limit
    ) throws Exception {
        if (hashing) {
            if (redesignsDoneForApplyingOperations.contains(initialRedesign.hashCode())) {
                return null;
            }
            redesignsDoneForApplyingOperations.add(initialRedesign.hashCode());
        }

        List<FunctionalityRedesign> redesigns = applyOperations(initialRedesign);

        // calculate metrics for every redesign
        for (FunctionalityRedesign redesign : redesigns) {
            try {
                redesign.calculateMetrics(decomposition, functionality);
            } catch (Exception e) {
                System.out.println(String.format("Calculating metrics for redesign error"));
            }
        }
        redesigns.sort(redesignComparator);
        redesigns = redesigns
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        List<FunctionalityRedesign> recursiveRedesigns = new ArrayList<>();
        for (FunctionalityRedesign redesign : redesigns) {
            FunctionalityRedesign recursive = redesignFunctionalityForApplyingOperationsFast(redesign, hashing, saveSolutionSet, limit);
            if (recursive!=null) {
                recursiveRedesigns.add(recursive);
            } else {
                recursiveRedesigns.add(redesign);
                if (saveSolutionSet) {
                    outerRedesignsForApplyingOperations.add(redesign);
                }
            }
        }

        return findOptimalRedesign(recursiveRedesigns);
    }

    private List<FunctionalityRedesign> applyOperations(FunctionalityRedesign initialRedesign) throws Exception {
        List<FunctionalityRedesign> redesigns = new ArrayList<>();

        for (LocalTransaction lt1 : initialRedesign.getRedesign()) {
            List<Integer> remoteInvocationsLT1 = lt1.getRemoteInvocations();
            for (LocalTransaction lt2 : initialRedesign.getRedesign()) {
                if (lt1 != lt2) {
                    // apply sequence change operation if possible
                    if (initialRedesign.isApplicableForSC() && remoteInvocationsLT1.contains(lt2.getId())) {
                        for (LocalTransaction lt3 : initialRedesign.getRedesign()) {
                            if (lt1.isApplicableForSC() || lt2.isApplicableForSC() || lt3.isApplicableForSC()) {
                                if (lt1 != lt3 && lt2 != lt3 && initialRedesign.executesBefore(lt3, lt2) &&
                                        // no local transaction in the invocation chain between lt3 and lt2 requires data from lt2
                                        !initialRedesign.checkIfAnyLocalTransactionInChainRequiresData(lt3, lt2)
                                ) {
                                    FunctionalityRedesign redesign = initializeFunctionalityAndTransactions(initialRedesign);
                                    try {
                                        redesign.sequenceChange(lt2.getId(), lt3.getId());
                                        redesign.setMetrics(newMetricsAfterSC(initialRedesign, lt1, lt2, lt3));
                                        redesign.setMetricsCalculated(true);
                                        redesign.addToSequenceChangeCount(1);
                                        redesign.addToOperationsApplied(String.format("[SC %s - %s]", lt2.getId(), lt3.getId()));
                                        redesigns.add(redesign);
                                    } catch (Exception e) {
                                        System.out.println("Sequence change error: " + e.getMessage());
                                    }
                                }
                            }
                        }
                    }

                    // apply LT merge operation if possible
                    if (lt1.getClusterID() == lt2.getClusterID()) {
                        // checks if adjacent in case 1 or case 2
                        if (remoteInvocationsLT1.contains(lt2.getId()) ||
                                initialRedesign.getRedesign().stream().anyMatch(lt ->
                                        lt.getRemoteInvocations().contains(lt1.getId()) && lt.getRemoteInvocations().contains(lt2.getId())
                                )
                        ) {
                            FunctionalityRedesign redesign = initializeFunctionalityAndTransactions(initialRedesign);
                            LocalTransaction newLT = redesign.mergeTwoLocalTransactions(lt1.getId(), lt2.getId());
                            redesign.setMetrics(newMetricsAfterLTM(initialRedesign, remoteInvocationsLT1, lt1, lt2, newLT, redesign));
                            redesign.setMetricsCalculated(true);
                            redesign.addToMergedLTsCount(1);
                            redesign.addToOperationsApplied(String.format("[LTM %s - %s]", lt1.getId(), lt2.getId()));
                            // every merged redesign is applicable for sequence change
                            redesign.setApplicableForSC(true);
                            redesigns.add(redesign);
                        }
                    }
                }
            }
        }

        return redesigns;
    }

    private List<Metric> newMetricsAfterSC(
            FunctionalityRedesign initialRedesign,
            LocalTransaction lt1,
            LocalTransaction lt2,
            LocalTransaction lt3
    ) {
        List<Metric> newMetrics = new ArrayList<>();
        initialRedesign.getMetrics().forEach(metric -> {
            Metric newMetric;
            switch (metric.getType()) {
                case COUPLING:
                    float result = metric.getValue();
                    if (lt1.getClusterID() != lt2.getClusterID()) { result--; }
                    if (lt3.getClusterID() != lt2.getClusterID()) { result++; }
                    newMetric = new CouplingMetric();
                    newMetric.setValue(result);
                    break;
                case SYSTEM_COMPLEXITY:
                    newMetric = new SystemComplexityMetric();
                    newMetric.setLtToMetricValueMap(metric.getLtToMetricValueMap());
                    newMetric.setValue(metric.getValue());
                    break;
                case FUNCTIONALITY_COMPLEXITY:
                    newMetric = new FunctionalityComplexityMetric();
                    newMetric.setLtToMetricValueMap(metric.getLtToMetricValueMap());
                    newMetric.setValue(metric.getValue());
                    break;
                case INCONSISTENCY_COMPLEXITY:
                    newMetric = new InconsistencyComplexityMetric();
                    newMetric.setValue(metric.getValue());
                    break;
                default:
                    throw new RuntimeException("The type \"" + metric.getType() + "\" is not a valid metric.");
            }
            newMetrics.add(newMetric);
        });
        return newMetrics;
    }

    private List<Metric> newMetricsAfterLTM(
            FunctionalityRedesign initialRedesign,
            List<Integer> remoteInvocationsLT1,
            LocalTransaction lt1,
            LocalTransaction lt2,
            LocalTransaction newLT,
            FunctionalityRedesign redesign
    ) {
        List<Metric> newMetrics = new ArrayList<>();
        initialRedesign.getMetrics().forEach(metric -> {
            float result;
            Metric newMetric = null;
            switch (metric.getType()) {
                case COUPLING:
                    result = metric.getValue();
                    if (!remoteInvocationsLT1.contains(lt2.getId())) { // case 2
                        Short clusterID = null;
                        for (LocalTransaction lt : initialRedesign.getRedesign()) {
                            if (lt.getRemoteInvocations().contains(lt1.getId())) {
                                clusterID = lt.getClusterID();
                                break;
                            }
                        }

                        if (clusterID != lt1.getClusterID()) {
                            result--;
                        }
                    }

                    CouplingMetric couplingMetric = new CouplingMetric();
                    couplingMetric.setValue(result);
                    newMetrics.add(couplingMetric);
                    break;
                case SYSTEM_COMPLEXITY:
                    newMetric = new SystemComplexityMetric();
                    break;
                case FUNCTIONALITY_COMPLEXITY:
                    newMetric = new FunctionalityComplexityMetric();
                    break;
                case INCONSISTENCY_COMPLEXITY:
                    InconsistencyComplexityMetric inconsistencyComplexityMetric = new InconsistencyComplexityMetric();
                    inconsistencyComplexityMetric.calculateMetric(decomposition, functionality, redesign);
                    newMetrics.add(inconsistencyComplexityMetric);
                    break;
                default:
                    throw new RuntimeException("The type \"" + metric.getType() + "\" is not a valid metric.");
            }

            if (Objects.equals(metric.getType(), SYSTEM_COMPLEXITY) || Objects.equals(metric.getType(), FUNCTIONALITY_COMPLEXITY)) {
                Map<Integer, Float> ltToMetricValueMap = new HashMap<>(metric.getLtToMetricValueMap());
                ltToMetricValueMap.remove(lt1.getId());
                ltToMetricValueMap.remove(lt2.getId());
                float resultForNewLT = newMetric.calculateForLocalTransaction(decomposition, functionality, newLT);
                ltToMetricValueMap.put(newLT.getId(), resultForNewLT);
                newMetric.setLtToMetricValueMap(ltToMetricValueMap);
                result = ltToMetricValueMap.values().stream().reduce(0f, Float::sum);

                newMetric.setValue(result);
                newMetrics.add(newMetric);
            }
        });
        return newMetrics;
    }

    public List<FunctionalityRedesign> createSagaRedesignsForLTsCharacterization() throws Exception {
        List<FunctionalityRedesign> sagaRedesigns = new ArrayList<>(redesigns);

        for (FunctionalityRedesign initialRedesign : redesigns) {
            for (LocalTransaction lt : initialRedesign.getRedesign()) {
                FunctionalityRedesign redesign = redesignFunctionalityForLTsCharacterization(initialRedesign, lt.getId());

                sagaRedesigns.add(redesign);
            }
            // adds redesign without pivot transaction
            sagaRedesigns.add(redesignFunctionalityForLTsCharacterization(initialRedesign, -1));
        }

        // calculate metrics
        calculateMetricsForRedesigns(sagaRedesigns);

        // order the redesigns by ascending complexity and coupling
        sagaRedesigns.sort(redesignComparator);

        return sagaRedesigns;
    }

    private FunctionalityRedesign redesignFunctionalityForLTsCharacterization(
            FunctionalityRedesign initialRedesign,
            int pivotLT) throws Exception {
        FunctionalityRedesign redesign = initializeFunctionalityAndTransactions(initialRedesign);
        if (pivotLT >= 0) {
            try {
                redesign.definePivotTransaction(pivotLT);
            } catch (Exception e) {
                System.out.println(String.format("Defining pivot transaction error: %s", e.getMessage())); // TODO: expand?
            }
        }

        return redesign;
    }

    private FunctionalityRedesign initializeFunctionalityAndTransactions(FunctionalityRedesign initialRedesign) {
        FunctionalityRedesign newRedesign = new FunctionalityRedesign(functionality.getName(), initialRedesign);
        for (LocalTransaction ltInitial : initialRedesign.getRedesign()) {
            LinkedHashSet<Access> newAccesses = new LinkedHashSet<>();
            ltInitial.getClusterAccesses().forEach(access ->
                    newAccesses.add(new Access(access.getEntityID(), access.getMode(), access.getLocation()))
            );
            LocalTransaction newLT = new LocalTransaction(
                    ltInitial.getId(),
                    ltInitial.getClusterID(),
                    newAccesses,
                    new ArrayList<>(ltInitial.getRemoteInvocations()),
                    ltInitial.getName()
            );
            newLT.setType(ltInitial.getType());
            newRedesign.getRedesign().add(newLT);
        }

        return newRedesign;
    }

    private void calculateAddedInvocationsForEachOrchestrationCluster() throws Exception {
        for (FunctionalityRedesign initialRedesign : functionality.getFunctionalityRedesigns()) {
            Map<String, Integer> orchestrationAddedRI = new HashMap<>();
            List<PairRI> remoteInvocations = new ArrayList<>();

            // Collect all remote invocations
            initialRedesign.getRedesign().forEach(lt1 -> {
                lt1.getRemoteInvocations().forEach(ri -> {
                    initialRedesign.getRedesign().stream()
                            .filter(lt2 -> lt2.getId() == ri)
                            .forEach(lt2 -> remoteInvocations.add(new PairRI(lt1, lt2)));
                });
            });

            LocalTransaction rootLT = initialRedesign.findRootLT();
            LocalTransaction lastLT = initialRedesign.findLastLT();

            functionality.getEntitiesPerCluster().keySet().forEach(orchestratorID -> {
                int addedRI = 0;
                // Check if RI has to be added for first local transaction
                if (rootLT.getClusterID() != orchestratorID) {
                    addedRI++;
                }
                // Check if RI has to be added for last local transaction
                if (lastLT.getClusterID() != orchestratorID) {
                    addedRI++;
                }
                // Check if RIs have to be added for every existing remote invocation
                for (PairRI ri : remoteInvocations) {
                    if (ri.getA().getClusterID() != orchestratorID && ri.getB().getClusterID() != orchestratorID) {
                        addedRI++;
                    }
                }
                orchestrationAddedRI.put(decomposition.getCluster(orchestratorID).getName(), addedRI);
            });

            initialRedesign.setOrchestrationAddedRI(orchestrationAddedRI);
        }
    }

    private void calculateMetricsForRedesigns(List<FunctionalityRedesign> sagaRedesigns) {
        for (FunctionalityRedesign redesign : sagaRedesigns) {
            try {
                redesign.calculateMetrics(decomposition, functionality);
            } catch (Exception e) {
                System.out.println(String.format("Calculating metrics for redesign error"));
            }
        }
    }

}
