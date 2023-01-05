package me.dupreez.thesisPoC.redesign.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import me.dupreez.thesisPoC.redesign.domain.metrics.Metric;
import me.dupreez.thesisPoC.redesign.domain.metrics.MetricFactory;
import me.dupreez.thesisPoC.redesign.utils.FunctionalityType;
import me.dupreez.thesisPoC.redesign.utils.LocalTransactionTypes;

import java.util.*;
import java.util.stream.Collectors;

public class FunctionalityRedesign {

    @Override
    public int hashCode() {
        return Objects.hash(new HashSet<>(redesign));
    }

    private String name;
    private List<LocalTransaction> redesign = new ArrayList<>();
    private int pivotTransaction = -1;
    private List<Metric> metrics = new ArrayList<>();

    private boolean metricsCalculated = false;

    private static final String[] queryMetrics = {
            Metric.MetricType.INCONSISTENCY_COMPLEXITY,
            Metric.MetricType.COUPLING,
    };

    private static final String[] sagaMetrics = {
            Metric.MetricType.SYSTEM_COMPLEXITY,
            Metric.MetricType.FUNCTIONALITY_COMPLEXITY,
            Metric.MetricType.COUPLING,
    };

    private Map<String, Integer> orchestrationAddedRI;
    private List<String> operationsApplied = new ArrayList<>();
    private int sequenceChangeCount;
    private int mergedLTsCount;
    private int initialLTsCount;
    private boolean applicableForSC = false;

    public FunctionalityRedesign(){}

    public FunctionalityRedesign(String name){
        this.name = name;
    }

    public FunctionalityRedesign(String name, int pivotTransaction){
        this.name = name;
        this.pivotTransaction = pivotTransaction;
    }

    public FunctionalityRedesign(String name, FunctionalityRedesign redesign){
        this.name = name;
        this.pivotTransaction = redesign.getPivotTransaction();
        this.sequenceChangeCount = redesign.getSequenceChangeCount();
        this.mergedLTsCount = redesign.getMergedLTsCount();
        this.initialLTsCount = redesign.getInitialLTsCount();
        this.operationsApplied =  new ArrayList<>(redesign.getOperationsApplied());
    }

    @JsonIgnore
    public boolean isApplicableForSC() {
        return applicableForSC;
    }

    public void setApplicableForSC(boolean applicableForSC) {
        this.applicableForSC = applicableForSC;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    public void addMetric(Metric metric) {
        this.metrics.add(metric);
    }

    @JsonIgnore
    public boolean isMetricsCalculated() {
        return metricsCalculated;
    }

    public void setMetricsCalculated(boolean metricsCalculated) {
        this.metricsCalculated = metricsCalculated;
    }

    public Metric searchMetricByType(String metricType) {
        for (Metric metric: this.getMetrics())
            if (metric.getType().equals(metricType))
                return metric;
        return null;
    }

    public void calculateMetrics(Decomposition decomposition, Functionality functionality) throws Exception {
        String[] metricList;
        if (functionality.getType() == FunctionalityType.QUERY)
            metricList = queryMetrics;
        else if (functionality.getType() == FunctionalityType.SAGA)
            metricList = sagaMetrics;
        else throw new RuntimeException("Unknown functionality type.");

        for(String metricType: metricList) {
            Metric metric = searchMetricByType(metricType);
            if (metric == null) {
                metric = MetricFactory.getFactory().getMetric(metricType);
                this.addMetric(metric);
            }

            if (!isMetricsCalculated()) {
                metric.calculateMetric(decomposition, functionality, this);
            }
        }
        setMetricsCalculated(true);
    }

    @JsonIgnore
    public int getPivotTransaction() {
        return pivotTransaction;
    }

    public void setPivotTransaction(int pivotTransaction) {
        this.pivotTransaction = pivotTransaction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LocalTransaction> getRedesign() {
        return redesign;
    }

    public void setRedesign(List<LocalTransaction> redesign) {
        this.redesign = redesign;
    }

    //////////////////////////////////
    @JsonIgnore
    public Map<String, Integer> getOrchestrationAddedRI() { return orchestrationAddedRI; }
    public void setOrchestrationAddedRI(Map<String, Integer> orchestrationAddedRI) {
        this.orchestrationAddedRI = orchestrationAddedRI;
    }

    @JsonIgnore
    public int getSequenceChangeCount() { return sequenceChangeCount; }
    public void addToSequenceChangeCount(int amount) { this.sequenceChangeCount += amount; }

    @JsonIgnore
    public int getMergedLTsCount() { return mergedLTsCount; }
    public void addToMergedLTsCount(int amount) { this.mergedLTsCount += amount; }

    public int getLocalTransactionsCount() { return redesign.size(); }

    @JsonIgnore
    public int getInitialLTsCount() { return initialLTsCount; }
    public void setInitialLTsCount(int initialLTsCount) { this.initialLTsCount = initialLTsCount; }

    @JsonIgnore
    public List<String> getOperationsApplied() {
        return operationsApplied;
    }

    public void addToOperationsApplied(String operationApplied) {
        this.operationsApplied.add(operationApplied);
    }

    public List<LocalTransaction> sequenceChange(
        Integer localTransactionID,
        Integer newCaller
    )
        throws Exception
    {
        LocalTransaction oldCallerLT = this.redesign
            .stream()
            .filter(lt -> lt.getRemoteInvocations().contains(localTransactionID))
            .findFirst()
            .orElseThrow(() -> new Exception("Transaction with id " + localTransactionID + " not found"));

        LocalTransaction newCallerLT = this.redesign
            .stream()
            .filter(lt -> lt.getId() == newCaller)
            .findFirst()
            .orElseThrow(() -> new Exception("Transaction with id " + newCaller + " not found"));

        oldCallerLT.getRemoteInvocations().remove(localTransactionID);
        newCallerLT.getRemoteInvocations().add(localTransactionID);

        return this.redesign;
    }

    private LinkedHashSet<Access> constructSequence(LinkedHashMap<Short, Access> hashMapSequence){
        LinkedHashSet<Access> accesses = new LinkedHashSet<>();

        for (Short s : hashMapSequence.keySet()) {
            Access access = new Access(s, hashMapSequence.get(s).getMode(), hashMapSequence.get(s).getLocation());
            accesses.add(access);
        }

        return accesses;
    }


    public void definePivotTransaction(int pivotID) throws Exception {
        this.setPivotTransaction(pivotID);

        List<LocalTransaction> localTransactionsSequence = new ArrayList<>();
        localTransactionsSequence.add(findRootLT());

        List<LocalTransaction> retriableLTs = new ArrayList<>();

        for(int i = 0; i < localTransactionsSequence.size(); i++){
            LocalTransaction lt = localTransactionsSequence.get(i);

            for (Integer id : lt.getRemoteInvocations()) {
                for (LocalTransaction localTransaction : this.redesign) {
                    if(localTransaction.getId() == id)
                        localTransactionsSequence.add(localTransaction);
                }
            }

            if(lt.getId() == pivotID) {
                lt.setType(LocalTransactionTypes.PIVOT);

                for (Integer id : lt.getRemoteInvocations()) {
                    for (LocalTransaction localTransaction : this.redesign) {
                        if(localTransaction.getId() == id)
                            retriableLTs.add(localTransaction);
                    }
                }
            } else {
                lt.setType(LocalTransactionTypes.COMPENSATABLE);
            }
        }

        for (int i = 0; i < retriableLTs.size(); i++) {
            LocalTransaction lt = retriableLTs.get(i);

            for (Integer id : lt.getRemoteInvocations()) {
                for (LocalTransaction localTransaction : this.redesign) {
                    if(localTransaction.getId() == id)
                        retriableLTs.add(localTransaction);
                }
            }

            lt.setType(LocalTransactionTypes.RETRIABLE);
        }
    }

    public LocalTransaction mergeTwoLocalTransactions(Integer lt1ID, Integer lt2ID) throws Exception {
        return mergeTwoLocalTransactions(
                findLocalTransactionByID(lt1ID),
                findLocalTransactionByID(lt2ID)
        );
    }
    private LocalTransaction findLocalTransactionByID(Integer id) throws Exception {
        return redesign
                .stream()
                .filter(lt -> lt.getId() == id)
                .findFirst()
                .orElseThrow(() -> new Exception("Transaction with id " + id + " not found"));
    }

    private LocalTransaction mergeTwoLocalTransactions(
        LocalTransaction lt1,
        LocalTransaction lt2
    ) {
        int min = Math.min(lt1.getId(), lt2.getId());

        LinkedHashMap<Short, Access> ltAccesses = new LinkedHashMap<>();

        Set<Access> lt1ClusterAccesses = lt1.getClusterAccesses();

        for (Access access : lt1ClusterAccesses)
            ltAccesses.put(access.getEntityID(), access);

        Set<Access> lt2ClusterAccesses = lt2.getClusterAccesses();

        for (Access access : lt2ClusterAccesses) {
            short accessedEntityID = access.getEntityID();
            byte accessedMode = access.getMode();

            Access mapAccess =  ltAccesses.get(accessedEntityID);

            if(mapAccess == null) { // map does not contain the entity
                ltAccesses.put(accessedEntityID, access);
            } else if(mapAccess.getMode() != accessedMode){
                String location = null;
                if (mapAccess.getLocation() != null && access.getLocation() != null) {
                    location = "R: " + mapAccess.getLocation() + " W: " + access.getLocation();
                }
                ltAccesses.put(accessedEntityID, new Access(accessedEntityID, (byte) 3, location)); // 3 -> "RW"

            }
        }

        List<Integer> remoteInvocations = new ArrayList<>();
        for(Integer id : lt1.getRemoteInvocations()){
            if(id != lt2.getId())
                remoteInvocations.add(id);
        }

        remoteInvocations.addAll(lt2.getRemoteInvocations());

        for(LocalTransaction lt : this.redesign){
            if(lt.getRemoteInvocations().contains(lt1.getId())){
                lt.getRemoteInvocations().remove((Integer) lt1.getId());
                lt.getRemoteInvocations().add(min);
            }

            if(lt.getRemoteInvocations().contains(lt2.getId())){
                lt.getRemoteInvocations().remove((Integer) lt2.getId());
                if (!lt.getRemoteInvocations().contains(min)) {
                    lt.getRemoteInvocations().add(min);
                }
            }
        }

        LocalTransaction newLT = new LocalTransaction(
            min,
            lt1.getClusterID(),
            constructSequence(ltAccesses),
            remoteInvocations,
            min + ": " + lt1.getClusterID()
        );

        this.redesign.removeIf(x -> x.getId() == lt1.getId() || x.getId() == lt2.getId());

        newLT.pruneAccesses();
        newLT.setApplicableForSC(true);
        this.redesign.add(newLT);
        return newLT;
    }

    public LocalTransaction findRootLT() throws Exception {
        Set<Integer> invocatedLTs = getRedesign().stream().flatMap(lt -> lt.getRemoteInvocations().stream()).collect(Collectors.toSet());
        for (LocalTransaction lt : getRedesign()) {
            if (!invocatedLTs.contains(lt.getId())) {
                return lt;
            }
        }

        return null;
    }

    public LocalTransaction findLastLT() throws Exception {
        LocalTransaction lastLT = null;
        List<LocalTransaction> nextLTs = new ArrayList<>();
        nextLTs.add(findRootLT());
        while (!nextLTs.isEmpty()) {
            lastLT = nextLTs.get(nextLTs.size()-1);
            nextLTs = nextLTs
                    .stream()
                    .flatMap(lt -> lt.getRemoteInvocations().stream())
                    .flatMap(ri -> getRedesign().stream().filter(lt -> lt.getId() == ri))
                    .collect(Collectors.toList());
        }

        return lastLT;
    }

    public boolean executesBefore(LocalTransaction lt1, LocalTransaction lt2) throws Exception {
        List<LocalTransaction> nextLTs = new ArrayList<>();
        nextLTs.add(findRootLT());
        while (!nextLTs.isEmpty()) {
            boolean matchedLT1 = nextLTs.contains(lt1);
            boolean matchedLT2 = nextLTs.contains(lt2);

            if (matchedLT1) {
                return !matchedLT2;
            } else if (matchedLT2) {
                return false;
            }

            nextLTs = nextLTs
                    .stream()
                    .flatMap(lt -> lt.getRemoteInvocations().stream())
                    .flatMap(ri -> getRedesign().stream().filter(lt -> lt.getId() == ri))
                    .collect(Collectors.toList());
        }

        throw new Exception(String.format("LTs are never found in invocation chain"));
    }

    public boolean checkIfAnyLocalTransactionInChainRequiresData(LocalTransaction lt1, LocalTransaction lt2) throws Exception {
        Set<Short> dataProducedByLT2 = lt2.getClusterAccesses()
                .stream()
                .filter(access -> access.getMode() > 1)
                .map(Access::getEntityID)
                .collect(Collectors.toSet());

        List<LocalTransaction> nextLTs = new ArrayList<>();
        nextLTs.add(findRootLT());
        boolean reachedLT1 = false;
        while (!nextLTs.contains(lt2)) {
            if (nextLTs.contains(lt1)) {
                reachedLT1 = true;
            }

            if (reachedLT1) {
                for (LocalTransaction lt : nextLTs) {
                        Set<Short> dataRequiredByLT = lt.getClusterAccesses()
                                .stream()
                                .filter(access -> access.getMode() != 2)
                                .map(Access::getEntityID)
                                .collect(Collectors.toSet());
                        dataRequiredByLT.retainAll(dataProducedByLT2);
                        if (!dataRequiredByLT.isEmpty()) {
                            return true;
                        }
                }
            }

            nextLTs = nextLTs
                    .stream()
                    .flatMap(lt -> lt.getRemoteInvocations().stream())
                    .flatMap(ri -> getRedesign().stream().filter(lt -> lt.getId() == ri))
                    .collect(Collectors.toList());

            if (nextLTs.isEmpty()) {
                throw new Exception(String.format("LTs are never found in invocation chain"));
            }
        }

        return false;
    }
}
