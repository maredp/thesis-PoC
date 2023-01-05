package me.dupreez.thesisPoC.redesign.domain;

import java.util.Objects;

public class Access {
    private short entityID;
    private byte mode; // "R" -> 1, "W" -> 2, "RW" -> 3, "WR" -> 4

    private String location;

    public Access(short entityID, byte mode, String location) {
        this.entityID = entityID;
        this.mode = mode;
        this.location = location;
    }

    public short getEntityID() { return entityID; }
    public void setEntityID(short entityID) { this.entityID = entityID; }

    public byte getMode() { return mode; }
    public void setMode(byte mode) { this.mode = mode; }

    public String getLocation() {
        return location;
    }

    @Override
	public boolean equals(final Object other) {
        if (other instanceof Access) {
            Access that = (Access) other;
            return this.entityID == that.entityID && this.mode == that.mode;
        }
        
        return false;
    }

    @Override
    public String toString() {
        String suffix = location == null ? "" : ',' + location;
        return "[" + entityID + ',' + mode + suffix + ']';
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityID, mode);
    }
}
