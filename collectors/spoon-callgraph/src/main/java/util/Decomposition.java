package util;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import util.serializer.ControllerSerializer;
import util.serializer.DecompositionSerializer;

import java.util.HashMap;
import java.util.List;

@JsonSerialize(using = DecompositionSerializer.class)
public class Decomposition {

    private HashMap<String, List<String>> c;

    public Decomposition(HashMap<String, List<String>> c) {
        this.c = c;
    }

    public HashMap<String, List<String>> getC() {
        return c;
    }

    public void setC(HashMap<String, List<String>> c) {
        this.c = c;
    }
}
