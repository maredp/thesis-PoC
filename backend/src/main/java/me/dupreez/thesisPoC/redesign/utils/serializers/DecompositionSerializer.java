package me.dupreez.thesisPoC.redesign.utils.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import me.dupreez.thesisPoC.redesign.domain.*;
import me.dupreez.thesisPoC.redesign.domain.metrics.Metric;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DecompositionSerializer extends StdSerializer<Decomposition> {

	public DecompositionSerializer() {
		this(null);
	}

	public DecompositionSerializer(Class<Decomposition> t) {
		super(t);
	}

	@Override
	public void serialize(
			Decomposition decomposition,
			JsonGenerator jg,
			SerializerProvider provider
	) throws IOException {
		Functionality functionality = decomposition.getFunctionalityToShow();

		if (functionality != null) {
			jg.writeStartObject();
			jg.writeStringField("name", functionality.getName());
			jg.writeObjectField("type", functionality.getType());
			try {
				jg.writeObjectField("entities", translateEntities(decomposition, functionality.getEntities()));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				jg.writeObjectField("entitiesPerCluster", translateEntitiesPerCluster(decomposition, functionality.getEntitiesPerCluster()));
			} catch (Exception e) {
				e.printStackTrace();
			}

			jg.writeArrayFieldStart("functionalityRedesigns");
			functionality.getFunctionalityRedesigns().forEach(fr -> {
				try {
					serializeFunctionalityRedesign(decomposition, functionality, fr, jg);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			jg.writeEndArray();
			jg.writeEndObject();
		} else {
			jg.writeStartObject();
			jg.writeObjectField("functionalities", decomposition.getFunctionalities());
			jg.writeEndObject();
		}
	}

	private void serializeFunctionalityRedesign(
			Decomposition decomposition,
			Functionality functionality,
			FunctionalityRedesign functionalityRedesign,
			JsonGenerator jg
	)
			throws IOException
	{
		jg.writeStartObject();
		jg.writeStringField("name", functionalityRedesign.getName());

		jg.writeArrayFieldStart("redesign");
		try {
			writeEmptyLT(functionality, functionalityRedesign, jg);
			writeLTsInOrder(decomposition, functionalityRedesign, jg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		jg.writeEndArray();

		functionalityRedesign.getMetrics().forEach(metric -> {
			try {
				serializeMetric(metric, jg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		jg.writeStringField("localTransactionsCount", String.valueOf(functionalityRedesign.getLocalTransactionsCount()));
		jg.writeStringField("initialLTsCount", String.valueOf(functionalityRedesign.getInitialLTsCount()));
		jg.writeStringField("sequenceChangeCount", String.valueOf(functionalityRedesign.getSequenceChangeCount()));
		jg.writeStringField("mergedLTsCount", String.valueOf(functionalityRedesign.getMergedLTsCount()));

		jg.writeArrayFieldStart("operationsApplied");
		for (String operationApplied : functionalityRedesign.getOperationsApplied()) {
			jg.writeString(operationApplied);
		}
		jg.writeEndArray();

		jg.writeStringField("pivotTransaction", String.valueOf(functionalityRedesign.getPivotTransaction()));
		if (functionalityRedesign.getOrchestrationAddedRI() != null) {
			jg.writeObjectField("orchestrationAddedRI", functionalityRedesign.getOrchestrationAddedRI());
		}
		jg.writeEndObject();
	}

	private void writeEmptyLT(Functionality functionality, FunctionalityRedesign fr, JsonGenerator jg) throws Exception {
		jg.writeStartObject();
		jg.writeStringField("name", functionality.getName());
		jg.writeStringField("id", "-1");
		jg.writeStringField("cluster", functionality.getName());
		jg.writeStringField("accessedEntities", "");
		List<Integer> remoteInvocations = new ArrayList<>();
		remoteInvocations.add(0);
		jg.writeObjectField("remoteInvocations", remoteInvocations);
		jg.writeStringField("type", "COMPENSATABLE");
		jg.writeEndObject();
	}

	private void writeLTsInOrder(
			Decomposition decomposition,
			FunctionalityRedesign functionalityRedesign,
			JsonGenerator jg
	)
			throws Exception
	{
		List<LocalTransaction> nextLTs = new ArrayList<>();
		nextLTs.add(functionalityRedesign.findRootLT());
		while (!nextLTs.isEmpty()) {
			nextLTs.forEach(lt -> {
				try {
					serializeLocalTransaction(decomposition, lt, jg);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});

			nextLTs = nextLTs
					.stream()
					.flatMap(lt -> lt.getRemoteInvocations().stream())
					.flatMap(ri -> functionalityRedesign.getRedesign().stream().filter(lt -> lt.getId() == ri))
					.collect(Collectors.toList());
		}
	}

	private void serializeLocalTransaction(
			Decomposition decomposition,
			LocalTransaction localTransaction,
			JsonGenerator jg
	)
			throws Exception
	{
		jg.writeStartObject();
		String id = String.valueOf(localTransaction.getId());
		String clusterName = String.valueOf(decomposition.getCluster(localTransaction.getClusterID()).getName());
		jg.writeStringField("name", String.format("%s: %s", id, clusterName));
		jg.writeStringField("id", id);
		jg.writeStringField("cluster", clusterName);

		jg.writeArrayFieldStart("accessedEntities");
		for (Access access : localTransaction.getClusterAccesses()) {
			jg.writeStartArray();
			jg.writeString(decomposition.getEntity(access.getEntityID()).getName());
			jg.writeString(translateAccessMode(access.getMode()));
			if (access.getLocation() != null) {
				jg.writeString(access.getLocation());
			}
			jg.writeEndArray();
		}
		jg.writeEndArray();

		jg.writeObjectField("remoteInvocations", localTransaction.getRemoteInvocations());
		jg.writeStringField("type", localTransaction.getType().toString());
		jg.writeEndObject();
	}

	private Map<String, String> translateEntities(
			Decomposition decomposition,
			Map<Short, Byte> entities
	)
			throws Exception
	{
		Map<String, String> translatedEntities = new HashMap<>();
		for (Short entityID : entities.keySet()) {
			Entity entity = decomposition.getEntity(entityID);
			translatedEntities.put(entity.getName(), translateAccessMode(entities.get(entityID)));
		}

		return translatedEntities;
	}

	private Map<String, Set<String>> translateEntitiesPerCluster(
			Decomposition decomposition,
			Map<Short, Set<Short>> entitiesPerCluster) {
		Map<String, Set<String>> translatedEntitiesPerCluster = new HashMap<>();
		for (Short clusterID : entitiesPerCluster.keySet()) {
			Cluster cluster = decomposition.getCluster(clusterID);
			Set<String> entityNames = entitiesPerCluster.get(clusterID)
					.stream()
					.map(entityID -> decomposition.getEntity(entityID).getName())
					.collect(Collectors.toSet());
			translatedEntitiesPerCluster.put(cluster.getName(), entityNames);
		}

		return translatedEntitiesPerCluster;
	}

	private String translateAccessMode(Byte accessMode) throws Exception {
		switch (accessMode) {
			case 1:
				return "R";
			case 2:
				return "W";
			case 3:
				return "RW";
			case 4:
				return "WR";
			default:
				throw new Exception(String.format("Access mode %s could not be translated", accessMode));
		}
	}

	private void serializeMetric(Metric metric, JsonGenerator jg) throws Exception {
		String field;
		switch (metric.getType()) {
			case Metric.MetricType.COUPLING:
				field = "coupling";
				break;
			case Metric.MetricType.FUNCTIONALITY_COMPLEXITY:
				field = "functionalityComplexity";
				break;
			case Metric.MetricType.SYSTEM_COMPLEXITY:
				field = "systemComplexity";
				break;
			case Metric.MetricType.INCONSISTENCY_COMPLEXITY:
				field = "inconsistencyComplexity";
				break;
			default:
				throw new Exception(String.format("Metric with type %s could not be serialized", metric.getType()));
		}
		jg.writeStringField(field, String.valueOf(metric.getValue()));
	}
}
