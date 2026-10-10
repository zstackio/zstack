package org.zstack.kvm.memory;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Set;

@org.zstack.header.rest.SDK
@JsonAdapter(value = MemoryZramPoolPreparation.Adapter.class, nullSafe = false)
public final class MemoryZramPoolPreparation {
    private String expectedHostBootId;
    private String expectedPoolGeneration;
    private boolean resetConfirmed;

    public MemoryZramPoolPreparation() { }
    public String getExpectedHostBootId() { return expectedHostBootId; }
    public void setExpectedHostBootId(String value) { expectedHostBootId = value; }
    public String getExpectedPoolGeneration() { return expectedPoolGeneration; }
    public void setExpectedPoolGeneration(String value) { expectedPoolGeneration = value; }
    public boolean isResetConfirmed() { return resetConfirmed; }
    public void setResetConfirmed(boolean value) { resetConfirmed = value; }

    public static final class Adapter extends TypeAdapter<MemoryZramPoolPreparation> {
        @Override public MemoryZramPoolPreparation read(JsonReader in) throws IOException {
            Set<String> seen = StrictMaintenanceJson.begin(in);
            MemoryZramPoolPreparation value = new MemoryZramPoolPreparation();
            while (in.hasNext()) {
                switch (StrictMaintenanceJson.nextName(in, seen, "expectedHostBootId", "expectedPoolGeneration", "resetConfirmed")) {
                    case "expectedHostBootId": value.expectedHostBootId = StrictMaintenanceJson.string(in); break;
                    case "expectedPoolGeneration": value.expectedPoolGeneration = StrictMaintenanceJson.string(in); break;
                    case "resetConfirmed": value.resetConfirmed = StrictMaintenanceJson.bool(in); break;
                    default: throw StrictMaintenanceJson.invalid("unknown member");
                }
            }
            StrictMaintenanceJson.end(in, seen, "expectedHostBootId", "expectedPoolGeneration", "resetConfirmed");
            return value;
        }

        @Override public void write(JsonWriter out, MemoryZramPoolPreparation value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            out.beginObject();
            out.name("expectedHostBootId").value(value.expectedHostBootId);
            out.name("expectedPoolGeneration").value(value.expectedPoolGeneration);
            out.name("resetConfirmed").value(value.resetConfirmed);
            out.endObject();
        }
    }
}
