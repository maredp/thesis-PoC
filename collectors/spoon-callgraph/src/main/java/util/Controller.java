package util;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import util.serializer.ControllerSerializer;

import java.util.List;

@JsonSerialize(using = ControllerSerializer.class)
public class Controller {

    private List<Access> a;

    public Controller(List<Access> a) {
        this.a = a;
    }

    public List<Access> getA() {
        return a;
    }

    public void setA(List<Access> a) {
        this.a = a;
    }
}
