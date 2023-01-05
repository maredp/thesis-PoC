package me.dupreez.thesisPoC.redesign.service;

import me.dupreez.thesisPoC.redesign.domain.*;
import me.dupreez.thesisPoC.redesign.utils.LocalTransactionTypes;

import java.io.File;
import java.util.*;

public class InputParserForRedesign extends InputParser {

    public List<FunctionalityRedesign> parseInput(
            Decomposition decomposition,
            Functionality functionality,
            String redesignName,
            String filePath
    )
            throws Exception
    {
        List<FunctionalityRedesign> redesigns = new ArrayList<>();
        boolean ltCharacterization = false;
        for (int i = 0; i < 2; i++) {
            FunctionalityRedesign redesign = new FunctionalityRedesign(
                    ltCharacterization ? redesignName : redesignName + " (default)"
            );

            List<Map<String, Object>> localTransactionsJSON =
                    (List<Map<String, Object>>) mapper.readValue(new File(filePath + "/" + redesignName + ".json"), Map.class)
                            .get("redesign");
            List<LocalTransaction> localTransactions = new ArrayList<>();
            for (Map<String, Object> ltJSON : localTransactionsJSON) {
                int id = Integer.parseInt((String) ltJSON.get("id"));
                if (id >= 0) {
                    Cluster cluster = decomposition.getCluster((String) ltJSON.get("cluster"));
                    List<List<String>> accessesList;
                    try {
                        accessesList = mapper.readValue((String) ltJSON.get("accessedEntities"), List.class);
                    } catch (Exception e) {
                        accessesList = convertObjectToList(ltJSON.get("accessedEntities"));
                    }
                    LinkedHashSet<Access> clusterAccesses = new LinkedHashSet<>();
                    for (List<String> accessList : accessesList) {
                        Entity entity = decomposition.getEntity(accessList.get(0));
                        String location = "";
                        if (accessList.size() > 2){
                            location = accessList.get(2);
                        }
                        Access access = new Access(entity.getID(), parseEntityAccessType(accessList.get(1)), location);
                        clusterAccesses.add(access);
                    }
                    List<Integer> remoteInvocations = (List<Integer>) ltJSON.get("remoteInvocations");
                    String name = (String) ltJSON.get("name");

                    LocalTransaction lt = new LocalTransaction(
                            id,
                            cluster.getID(),
                            clusterAccesses,
                            remoteInvocations,
                            name
                    );
                    if (ltCharacterization) {
                        lt.setType(LocalTransactionTypes.valueOf((String) ltJSON.get("type")));
                    }
                    localTransactions.add(lt);
                }
            }
            redesign.setRedesign(localTransactions);
            redesign.calculateMetrics(decomposition, functionality);
            redesigns.add(redesign);

            ltCharacterization = true;
        }

        return redesigns;
    }

    private static List<List<String>> convertObjectToList(Object obj) {
        List<List<String>> list = new ArrayList<>();
        if (obj.getClass().isArray()) {
            List<Object> list2 = Arrays.asList(obj);
            for (Object o : list2) {
                list.add(Arrays.asList((String[])o));
            }
        } else if (obj instanceof Collection) {
            List<Object> list2 = new ArrayList<>((Collection<Object>)obj);
            for (Object o : list2) {
                list.add(new ArrayList<>((Collection<String>)o));
            }
        }
        return list;
    }
}
