package me.dupreez.thesisPoC.redesign.service;

import me.dupreez.thesisPoC.redesign.domain.*;
import me.dupreez.thesisPoC.redesign.utils.Constants;
import me.dupreez.thesisPoC.redesign.utils.Utils;

import java.io.File;
import java.util.*;

public class InputParserForDecompositionAndFunctionalities extends InputParser {

    public Decomposition parseInput(
            Codebase codebase,
            String decompositionName) throws Exception {
        Short entityID = 0;
        Short clusterID = 0;

        Decomposition decomposition = codebase.getDecompositions().stream()
                .filter(d->d.getName().equals(decompositionName))
                .findFirst()
                .orElse(null);

        if (decomposition!=null) {
            return decomposition;
        }

        decomposition = new Decomposition();
        String filePath = String.format("%s/decomposition/%s.json", codebase.getName(), decompositionName);
        Map<String, List<String>> clusterToEntitiesMap = (Map<String, List<String>>) mapper.readValue(parseFile(filePath), Map.class)
                .get("clusters");

        for (String clusterName : clusterToEntitiesMap.keySet()) {
            List<String> entityNames = clusterToEntitiesMap.get(clusterName);
            Cluster cluster = new Cluster(clusterID++, clusterName);

            for (String entityName : entityNames) {
                Entity entity = new Entity(entityID++, entityName);
                decomposition.addEntity(entity, cluster);
            }
            decomposition.addCluster(cluster);
        }
        decomposition.setName(decompositionName);
        decomposition.setCodebaseName(codebase.getName());

        parseFunctionalities(decomposition);
        Map<String, Set<Cluster>> functionalitiesClusters = Utils.getFunctionalitiesClusters(
                decomposition.getEntityIDToClusterID(),
                decomposition.getClusters(),
                decomposition.getFunctionalities().values());
        decomposition.setFunctionalitiesClusters(functionalitiesClusters);
        codebase.addDecomposition(decomposition);

        return decomposition;
    }

    private void parseFunctionalities(Decomposition decomposition) throws Exception {
        String filePath = String.format("%s/functionalities.json", decomposition.getCodebaseName());
        Map<String, List<List<String>>> functionalityToEntityAccesses = mapper.readValue(parseFile(filePath), Map.class);

        for (String functionalityName : functionalityToEntityAccesses.keySet()) {
            List<List<String>> entityAccesses = functionalityToEntityAccesses.get(functionalityName);
            Functionality functionality = parseFunctionality(decomposition, functionalityName, entityAccesses);
            decomposition.addFunctionality(functionality);
        }
    }

    private Functionality parseFunctionality(
            Decomposition decomposition,
            String functionalityName,
            List<List<String>> entityAccesses) throws Exception {
        List<LocalTransaction> localTransactions = new ArrayList<>();
        Functionality functionality = new Functionality();
        functionality.setName(functionalityName);
        Map<Short, Set<Short>> entitiesPerCluster = new HashMap<>();
        LinkedHashSet<Access> ltAccesses = new LinkedHashSet<>();
        Integer ltID = 0;

        Map<String, Entity> entities = decomposition.getEntities();
        Cluster prevCluster = null;
        for (List<String> entityAccess : entityAccesses) {
            String entityName = entityAccess.get(0);
            String entityAccessMode = entityAccess.get(1);
            String location = null;
            if (entityAccess.size() > 2) {
                location = entityAccess.get(2);
            }

            Entity entity = entities.get(entityName);
            if (entity != null) {
                Cluster cluster = decomposition.findClusterByEntity(entity);
                Access access = new Access(entity.getID(), parseEntityAccessType(entityAccessMode), location);
                functionality.addEntity(access.getEntityID(), access.getMode());

                if (!entitiesPerCluster.containsKey(cluster.getID())) {
                    entitiesPerCluster.put(cluster.getID(), new HashSet<>());
                }
                entitiesPerCluster.get(cluster.getID()).add(entity.getID());

                if (prevCluster != null && !prevCluster.equals(cluster)) {
                    List<Integer> remoteInvocations = new ArrayList<>();
                    remoteInvocations.add(ltID + 1);
                    localTransactions.add(parseLocalTransaction(
                            ltID,
                            prevCluster.getID(),
                            ltAccesses,
                            remoteInvocations,
                            String.format("%s: %s", ltID, prevCluster.getName()))
                    );
                    ltID++;
                    ltAccesses = new LinkedHashSet<>();
                }
                ltAccesses.add(access);
                prevCluster = cluster;
            } else {
                throw new Exception(String.format("Cannot find entity with name %s", entityName));
            }
        }
        if (prevCluster != null) {
            // create and add last lt
            localTransactions.add(parseLocalTransaction(
                    ltID,
                    prevCluster.getID(),
                    ltAccesses,
                    new ArrayList<>(),
                    String.format("%s: %s", ltID, prevCluster.getName()))
            );
        }
        FunctionalityRedesign functionalityRedesign = new FunctionalityRedesign("Original");
        functionalityRedesign.setRedesign(localTransactions);
        functionalityRedesign.setInitialLTsCount(localTransactions.size());
        LinkedHashSet<FunctionalityRedesign> functionalityRedesigns = new LinkedHashSet<>();
        functionalityRedesigns.add(functionalityRedesign);
        functionality.setFunctionalityRedesigns(functionalityRedesigns);
        functionality.setEntitiesPerCluster(entitiesPerCluster);
        functionality.defineFunctionalityType();

        return functionality;
    }

    private LocalTransaction parseLocalTransaction(
            Integer id,
            Short clusterID,
            LinkedHashSet<Access> accesses,
            List<Integer> remoteInvocations,
            String name) {
        LocalTransaction localTransaction = new LocalTransaction(
                id,
                clusterID,
                accesses,
                remoteInvocations,
                name
        );
        localTransaction.pruneAccesses();  // TODO: check if works
        return localTransaction;
    }

    private File parseFile(String filePath) {
        return new File(Constants.COLLECTION_PATH + filePath);
    }

}
