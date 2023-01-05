package me.dupreez.thesisPoC.redesign.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

import me.dupreez.thesisPoC.redesign.utils.LocalTransactionTypes;

@JsonInclude(JsonInclude.Include.USE_DEFAULTS)
public class LocalTransaction {

    @Override
    public int hashCode() {
        return id * Objects.hash(clusterAccesses, new HashSet<>(remoteInvocations));
    }

    private String name;
    private int id;
    private short clusterID;
    private LinkedHashSet<Access> clusterAccesses;
    private List<Integer> remoteInvocations;
    private LocalTransactionTypes type = LocalTransactionTypes.COMPENSATABLE;

    @JsonIgnore
    private boolean applicableForSC = false;

    public LocalTransaction(){}

    public LocalTransaction(
        int id,
        short clusterID,
        LinkedHashSet<Access> clusterAccesses,
        List<Integer> remoteInvocations,
        String name
    ){
        this.id = id;
        this.name = name;
        this.clusterID = clusterID;
        this.clusterAccesses = clusterAccesses;
        this.remoteInvocations = remoteInvocations;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public short getClusterID() {
        return clusterID;
    }

    public void setClusterID(short clusterID) {
        this.clusterID = clusterID;
    }

    public LinkedHashSet<Access> getClusterAccesses() {
        return clusterAccesses;
    }

    public void setClusterAccesses(LinkedHashSet<Access> clusterAccesses) {
        this.clusterAccesses = clusterAccesses;
    }

    public List<Integer> getRemoteInvocations() {
        return remoteInvocations;
    }

    public void setRemoteInvocations(List<Integer> remoteInvocations) {
        this.remoteInvocations = remoteInvocations;
    }

    public void addRemoteInvocations(int remoteInvocation) {
        this.remoteInvocations.add(remoteInvocation);
    }

    public LocalTransactionTypes getType() {
        return type;
    }

    public void setType(LocalTransactionTypes type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isApplicableForSC() { return applicableForSC; }

    public void setApplicableForSC(boolean applicableForSC) {
        this.applicableForSC = applicableForSC;
    }

    public boolean containsLock() {
        for (Access access : getClusterAccesses()) {
            if (access.getMode() > 1) {
                return true;
            }
        }
        return false;
    }

    public boolean containsRead() {
        for (Access access : getClusterAccesses()) {
            if (access.getMode() == 1 || access.getMode() == 3) {
                return true;
            }
        }
        return false;
    }

    public LocalTransactionTypes getTypeFromAccesses() {
        return containsLock() ? LocalTransactionTypes.COMPENSATABLE : LocalTransactionTypes.RETRIABLE;
    }

    public void pruneAccesses() {
        Map<Short, Access> prevEntityAccesses = new LinkedHashMap<>();
        LinkedHashSet<Access> newAccesses = new LinkedHashSet<>();

        for (Access access : getClusterAccesses()) {
            Short entity = access.getEntityID();
            byte accessType = access.getMode();
            String location;

            Access prevAccess = prevEntityAccesses.get(entity);
            if (prevAccess==null) {
                prevEntityAccesses.put(entity, access);
            } else if (prevAccess.getMode() == 1 && accessType == 2) {
                location = access.getLocation() != null ? "R: " + prevAccess.getLocation() + " W: " + access.getLocation() : null;
                prevEntityAccesses.put(entity, new Access(entity, (byte) 3, location));
            } else if (prevAccess.getMode() == 2 && accessType == 1) {
                location = access.getLocation() != null ? "W: " + prevAccess.getLocation() + " R: " + access.getLocation() : null;
                prevEntityAccesses.put(entity, new Access(entity, (byte) 4, location));
            } else if (prevAccess.getMode() == 4 && accessType == 2) {
                location = access.getLocation() != null ? "R: " + prevAccess.getLocation().split(" R: ")[1] + " W: " + access.getLocation() : null;
                prevEntityAccesses.put(entity, new Access(entity, (byte) 3, location));
            }
        }

        for (Map.Entry<Short, Access> entry : prevEntityAccesses.entrySet()) {
            Byte accessType = entry.getValue().getMode();
            String location = entry.getValue().getLocation();
            if (accessType == 4) {
                accessType = 2;
                if (location != null) {
                    location = location.split(" R: ")[0].substring(3);
                }
            }
            Access newAccess = new Access(entry.getKey(), accessType, location);
            newAccesses.add(newAccess);
        }

        setClusterAccesses(newAccesses);
    }
}
