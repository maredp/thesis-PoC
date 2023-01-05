package util;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import util.serializer.AccessSerializer;

@JsonSerialize(using = AccessSerializer.class)
public class Access {
    private String mode;
    private String entityName;

    private String location;

    public Access(String mode, String entityName, String location) {
        this.mode = mode;
        this.entityName = entityName;
        this.location = location;
    }

    public Access(String mode, String entityName) {
        this.mode = mode;
        this.entityName = entityName;
        this.location = "";
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
