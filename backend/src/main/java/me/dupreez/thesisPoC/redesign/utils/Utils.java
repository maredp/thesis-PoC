package me.dupreez.thesisPoC.redesign.utils;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.dupreez.thesisPoC.redesign.domain.Cluster;
import me.dupreez.thesisPoC.redesign.domain.Decomposition;
import me.dupreez.thesisPoC.redesign.domain.Functionality;

import java.io.*;
import java.util.*;

public class Utils {

    public static Map<String, Set<Cluster>> getFunctionalitiesClusters(
            Map<Short, Short> entityIDToClusterID,
            Map<Short, Cluster> clusters,
            Collection<Functionality> functionalities
    ) {
        Map<String, Set<Cluster>> functionalitiesClusters = new HashMap<>();

        for (Functionality functionality : functionalities) {
            String functionalityName = functionality.getName();

            Set<Cluster> functionalityClusters = new HashSet<>();

            for (short entityID : functionality.getEntities().keySet()) {
                Cluster cluster = clusters.get(entityIDToClusterID.get(entityID));
                functionalityClusters.add(cluster);
            }

            functionalitiesClusters.put(
                    functionalityName,
                    functionalityClusters
            );
        }

        return functionalitiesClusters;
    }

    public static Map<Short, Set<Functionality>> getClustersFunctionalities(
            Map<Short, Short> entityIDToClusterID,
            Map<Short, Cluster> clusters,
            Collection<Functionality> functionalities
    ) {
        Map<Short, Set<Functionality>> clustersFunctionalities = new HashMap<>();

        for (Functionality functionality : functionalities) {
            for (short entityID : functionality.getEntities().keySet()) {
                Cluster cluster = clusters.get(entityIDToClusterID.get(entityID));

                Set<Functionality> clusterFunctionalities = clustersFunctionalities.getOrDefault(cluster.getID(), new HashSet<>());
                clusterFunctionalities.add(functionality);
                clustersFunctionalities.put(cluster.getID(), clusterFunctionalities);
            }
        }

        return clustersFunctionalities;
    }

    public static void storeFunctionalityToShowAsJsonFile(String filepath, Decomposition decomposition, int limit) {
        try {
            File filePath = new File(filepath);
            if (!filePath.exists()) {
                filePath.mkdirs();
            }

            ObjectMapper mapper = new ObjectMapper();
            Functionality functionality = decomposition.getFunctionalityToShow();
            String fileName = limit > 0 ? functionality.getName() + ".json (limit = " + limit + ")" : functionality.getName() + ".json";
//            mapper.writerWithDefaultPrettyPrinter().writeValue(new FileOutputStream(filepath + fileName), decomposition);

            // Setup a pretty printer with an indenter (indenter has 4 spaces in this case)
            DefaultPrettyPrinter.Indenter indenter =
                    new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            printer.indentObjectsWith(indenter);
            printer.indentArraysWith(indenter);
            mapper.writer(printer).writeValue(new FileOutputStream(filepath + fileName), decomposition);

            System.out.println("File " + fileName + " created at: " + filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeDataPointsToFile(List<DataPoint> dataPoints, String functionalityName) throws Exception {
        FileWriter fileWriter = new FileWriter(Constants.PLOTS_PATH+functionalityName+".txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        dataPoints.forEach(point -> printWriter.println(point.toString()));
        printWriter.close();
    }

}
