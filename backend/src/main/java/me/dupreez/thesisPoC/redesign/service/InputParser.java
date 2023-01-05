package me.dupreez.thesisPoC.redesign.service;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class InputParser {

    protected ObjectMapper mapper = new ObjectMapper();

    protected static byte parseEntityAccessType(String entityAccessType) throws Exception {
        switch(entityAccessType) {
            case "R": return 1;
            case "W": return 2;
            case "RW": return 3;
            case "WR": return 4;
            default: throw new Exception(String.format("Cannot parse entity access type of %s", entityAccessType));
        }
    }
}
