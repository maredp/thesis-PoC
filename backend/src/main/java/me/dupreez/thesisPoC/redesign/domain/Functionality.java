package me.dupreez.thesisPoC.redesign.domain;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import me.dupreez.thesisPoC.redesign.utils.FunctionalityType;

@JsonInclude(JsonInclude.Include.USE_DEFAULTS)
public class Functionality {
	private String name;
	private FunctionalityType type;
	private Map<Short, Byte> entities = new HashMap<>(); // <entityID, mode>
	private LinkedHashSet<FunctionalityRedesign> functionalityRedesigns = new LinkedHashSet<>();
	private Map<Short, Set<Short>> entitiesPerCluster = new HashMap<>();

	public Functionality() {}

	public Functionality(String name) {
        this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<Short, Byte> getEntities() {
		return this.entities;
	}

	public void setEntities(Map<Short, Byte> entities) {
		this.entities = entities;
	}

	public void addEntity(short entityID, byte mode) {
		Byte savedMode = this.entities.get(entityID);

		if (savedMode != null) {
			if (savedMode != mode && savedMode != 3) // "RW" -> 3
				this.entities.put(entityID, (byte) 3); // "RW" -> 3
		} else {
			this.entities.put(entityID, mode);
		}
	}

	public boolean containsEntity(short entity) {
		return this.entities.containsKey(entity);
	}

	public Set<FunctionalityRedesign> getFunctionalityRedesigns() { return functionalityRedesigns; }

	public void setFunctionalityRedesigns(LinkedHashSet<FunctionalityRedesign> functionalityRedesigns) {
		this.functionalityRedesigns = functionalityRedesigns;
	}

	@JsonIgnore
	public FunctionalityRedesign getOriginalFunctionalityRedesign() {
		for (FunctionalityRedesign redesign : getFunctionalityRedesigns()) {
			if (redesign.getName().equals("Original")) {
				return redesign;
			}
		}
		return null;
	}

	public FunctionalityType getType() {
		return type;
	}

	public void setType(FunctionalityType type) {
		this.type = type;
	}

	public Set<Short> entitiesTouchedInAGivenMode(byte mode){
		Set<Short> entitiesTouchedInAGivenMode = new HashSet<>();
		for(Short entity : this.entities.keySet()){
			if(this.entities.get(entity) == 3 || this.entities.get(entity) == mode) // 3 -> RW
				entitiesTouchedInAGivenMode.add(entity);
		}
		return entitiesTouchedInAGivenMode;
	}

	public Set<Short> clustersOfGivenEntities(Set<Short> entities){
		Set<Short> clustersOfGivenEntities = new HashSet<>();
		for(Short clusterID : this.entitiesPerCluster.keySet()){
			for(Short entityID : entities){
				if(this.entitiesPerCluster.get(clusterID).contains(entityID))
					clustersOfGivenEntities.add(clusterID);
			}
		}
		return clustersOfGivenEntities;
	}

	public FunctionalityType defineFunctionalityType(){
		if(this.type != null) return this.type;

		if(!this.entities.isEmpty()){
			for(Short entity : this.entities.keySet()){
				if(this.entities.get(entity) >= 2) { // 2 -> W , 3 -> RW
					this.type = FunctionalityType.SAGA;
					return this.type;
				}

			}
			this.type = FunctionalityType.QUERY;
		}
		return this.type;
	}

	public boolean containsEntity(Short entity) {
		return this.entities.containsKey(entity);
	}

	public Map<Short, Set<Short>> getEntitiesPerCluster() {
		return entitiesPerCluster;
	}

	public void setEntitiesPerCluster(Map<Short, Set<Short>> entitiesPerCluster) {
		this.entitiesPerCluster = entitiesPerCluster;
	}

}