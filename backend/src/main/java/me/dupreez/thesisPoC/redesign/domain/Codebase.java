package me.dupreez.thesisPoC.redesign.domain;

import java.util.HashSet;
import java.util.Set;

public class Codebase {
	private String name;

	private Set<Decomposition> decompositions = new HashSet<>();

	public Codebase(String name) {
        this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Decomposition> getDecompositions() { return this.decompositions; }

	public void addDecomposition(Decomposition decomposition) { this.decompositions.add(decomposition); }

}
