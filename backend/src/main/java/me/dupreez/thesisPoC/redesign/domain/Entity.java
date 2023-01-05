package me.dupreez.thesisPoC.redesign.domain;

public class Entity {
	private Short id;
	private String name;

	public Entity() { }

	public Entity(Short id, String name) {
		this.id = id;
        this.name = name;
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

}
