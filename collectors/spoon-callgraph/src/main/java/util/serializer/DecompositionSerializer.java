package util.serializer;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import util.Decomposition;

import java.io.IOException;

public class DecompositionSerializer extends StdSerializer<Decomposition> {
    public DecompositionSerializer() {
        this(null);
    }

    public DecompositionSerializer(Class<Decomposition> t) {
        super(t);
    }

    @Override
    public void serialize(
            Decomposition decomposition,
            JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider
    ) throws IOException {
        if (decomposition.getC() != null) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("clusters", decomposition.getC());
            jsonGenerator.writeEndObject();
        }
    }
}