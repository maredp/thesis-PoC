package me.dupreez.thesisPoC.redesign.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Cluster {
	private Short id;
	private String name;
	private float complexity;
	private float cohesion;
	private float coupling;
	private Map<Short, Set<Short>> couplingDependencies = new HashMap<>(); // <clusterID, Set<EntityID>>
	private Set<Entity> entities = new HashSet<>(); // entity IDs

	public Cluster() { }

	public Cluster(Short id, String name) {
		this.id = id;
        this.name = name;
	}

	public Cluster(Short id, String name, Set<Entity> entities) {
		this.id = id;
		this.name = name;
		this.entities = entities;
	}

	public Cluster(Cluster c) {
		this.id = c.getID();
		this.name = c.getName();
		this.complexity = c.getComplexity();
		this.cohesion = c.getCohesion();
		this.coupling = c.getCoupling();
		this.couplingDependencies = c.getCouplingDependencies();
		this.entities = c.getEntities();
	}

	public Short getID() {
		return id;
	}

	public void setID(Short id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getComplexity() {
		return complexity;
	}

	public void setComplexity(float complexity) {
		this.complexity = complexity;
	}

	public float getCohesion() {
		return cohesion;
	}

	public void setCohesion(float cohesion) {
		this.cohesion = cohesion;
	}

	public float getCoupling() {
		return coupling;
	}

	public void setCoupling(float coupling) {
		this.coupling = coupling;
	}

	public Map<Short, Set<Short>> getCouplingDependencies() { return couplingDependencies; }

	public void setCouplingDependencies(Map<Short, Set<Short>> couplingDependencies) { this.couplingDependencies = couplingDependencies; }

	public Set<Entity> getEntities() { return entities; }

	public void setEntities(Set<Entity> entities) {
		this.entities = entities;
	}
	public void addEntity(Entity entity) { this.entities.add(entity); }

	public void removeEntity(short entityID) {
		for (Entity entity : getEntities()) {
			if (entity.getID().equals(entityID)) {
				this.entities.remove(entity);
			}
		}
	}

	public boolean containsEntity(short entityID) {
		return getEntities().stream().anyMatch(entity -> entity.getID().equals(entityID));
	}

	public Entity getEntity(String entityName) {
		for (Entity entity : getEntities()) {
			if (entity.getName().equals(entityName)) {
				return entity;
			}
		}
		return null;
	}

	public void clearCouplingDependencies() {this.couplingDependencies.clear(); }

	public void addCouplingDependency(Short toCluster, short entityID) {
		if (this.couplingDependencies.containsKey(toCluster)) {
			this.couplingDependencies.get(toCluster).add(entityID);
		} else {
			Set<Short> touchedEntityIDs = new HashSet<>();
			touchedEntityIDs.add(entityID);
			this.couplingDependencies.put(toCluster, touchedEntityIDs);
		}
	}

	public void addCouplingDependencies(Short toCluster, Set<Short> entityIDs) {
		if (this.couplingDependencies.containsKey(toCluster)) {
			this.couplingDependencies.get(toCluster).addAll(entityIDs);
		} else {
			this.couplingDependencies.put(toCluster, entityIDs);
		}
	}

	public void transferCouplingDependencies(Set<Short> entities, short currentClusterID, short newClusterID) {
		Set<Short> dependencyEntities = this.couplingDependencies.get(currentClusterID);

		if (dependencyEntities == null) return;

		for (short entity : entities)
			if (dependencyEntities.remove(entity) && newClusterID != id)
				addCouplingDependency(newClusterID, entity);
		if (dependencyEntities.isEmpty())
			this.couplingDependencies.remove(currentClusterID);
	}
}
