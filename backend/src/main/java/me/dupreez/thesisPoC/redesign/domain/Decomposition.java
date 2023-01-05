package me.dupreez.thesisPoC.redesign.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.dupreez.thesisPoC.redesign.domain.metrics.MetricFactory;
import me.dupreez.thesisPoC.redesign.utils.serializers.DecompositionSerializer;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import me.dupreez.thesisPoC.redesign.domain.metrics.Metric;

import java.util.*;

@JsonSerialize(using = DecompositionSerializer.class)
public class Decomposition {

    private Map<Short, Cluster> clusters = new HashMap<>();

    private Map<String, Functionality> functionalities = new HashMap<>(); // <functionalityName, Functionality>
    private Map<String, Set<Cluster>> functionalitiesClusters = new HashMap<>(); // <functionalityName, Clusters>

    private Map<String, Entity> entities = new HashMap<>();

    private Map<Short, Short> entityIDToClusterID = new HashMap<>();

    private String name;
    private String codebaseName;
    private Functionality functionalityToShow;
    private List<Metric> metrics = new ArrayList<>();

    public String getCodebaseName() { return this.codebaseName; }

    public void setCodebaseName(String codebaseName) { this.codebaseName = codebaseName; }

    public Functionality getFunctionalityToShow() { return  this.functionalityToShow; }

    public void setFunctionalityToShow(Functionality functionality) { this.functionalityToShow = functionality; }

    public String getName() { return this.name; }

    public void setName(String name) {
        this.name = name;
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

    public Map<String, Entity> getEntities() { return entities; }

    public void addEntity(Entity entity, Cluster cluster) {
        entities.put(entity.getName(), entity);
        entityIDToClusterID.put(entity.getID(), cluster.getID());
        cluster.addEntity(entity);
    }

    public Entity getEntity(Short entityID) {
        for (Map.Entry<String, Entity> entity : this.entities.entrySet())
            if (entity.getValue().getID().equals(entityID))
                return entity.getValue();

        throw new Error("Entity with ID: " + entityID + " not found");
    }

    public Entity getEntity(String entityName) {
        return this.entities.get(entityName);
    }

    public Map<Short, Short> getEntityIDToClusterID() {
        return entityIDToClusterID;
    }

    public Map<Short, Cluster> getClusters() { return this.clusters; }

    public void setClusters(Map<Short, Cluster> clusters) { this.clusters = clusters; }

    public Map<String, Functionality> getFunctionalities() { return functionalities; }

    public void setFunctionalities(Map<String, Functionality> functionalities) { this.functionalities = functionalities; }

    public void addFunctionality(Functionality functionality) { this.functionalities.put(functionality.getName(), functionality); }

    public Map<String, Set<Cluster>> getFunctionalitiesClusters() { return this.functionalitiesClusters; }
    public void setFunctionalitiesClusters(Map<String, Set<Cluster>> functionalitiesClusters) { this.functionalitiesClusters = functionalitiesClusters; }

    public boolean functionalityExists(String functionalityName) { return this.functionalities.containsKey(functionalityName); }

    public Functionality getFunctionality(String functionalityName) {
        Functionality c = this.functionalities.get(functionalityName);

        if (c == null) throw new Error("Functionality with name: " + functionalityName + " not found");

        return c;
    }

    public boolean clusterNameExists(String clusterName) {
        for (Map.Entry<Short, Cluster> cluster :this.clusters.entrySet())
            if (cluster.getValue().getName().equals(clusterName))
                return true;
        return false;
    }

    public void addCluster(Cluster cluster) {
        Cluster c = this.clusters.putIfAbsent(cluster.getID(), cluster);

        if (c != null) throw new Error("Cluster with ID: " + cluster.getID() + " already exists");
    }

    public Cluster getCluster(Short clusterID) {
        Cluster c = this.clusters.get(clusterID);

        if (c == null) throw new Error("Cluster with ID: " + clusterID + " not found");

        return c;
    }

    public Cluster getCluster(String clusterName) {
        for (Map.Entry<Short, Cluster> cluster :this.clusters.entrySet())
            if (cluster.getValue().getName().equals(clusterName))
                return cluster.getValue();
        return null;
    }

    public int maxClusterSize() {
        int max = 0;

        for (Cluster cluster : this.clusters.values()) {
            if (cluster.getEntities().size() > max)
                max = cluster.getEntities().size();
        }

        return max;
    }

    public static Set<LocalTransaction> getAllLocalTransactions(
            DirectedAcyclicGraph<LocalTransaction, DefaultEdge> localTransactionsGraph
    ) {
        return localTransactionsGraph.vertexSet();
    }

    public Cluster findClusterByEntity(Entity entity) throws Exception {
        Short clusterID = entityIDToClusterID.get(entity.getID());
        if (clusterID != null) {
            Cluster cluster = clusters.get(clusterID);
            if (cluster != null) {
                return cluster;
            } else {
                throw new Exception(String.format("Cannot find cluster for entity %s", entity.getName()));
            }
        } else {
            throw new Exception(String.format("Cannot find clusterID for entity %s", entity.getName()));
        }
    }

    public Metric searchMetricByType(String metricType) {
        for (Metric metric: this.getMetrics())
            if (metric.getType().equals(metricType))
                return metric;
        return null;
    }
}
