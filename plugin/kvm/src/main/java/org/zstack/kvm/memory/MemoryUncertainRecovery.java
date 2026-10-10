package org.zstack.kvm.memory;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Set;

@org.zstack.header.rest.SDK
@JsonAdapter(value = MemoryUncertainRecovery.Adapter.class, nullSafe = false)
public final class MemoryUncertainRecovery {
    private String expectedHostBootId;
    private String expectedPoolGeneration;
    private String drainControlOperationUuid;
    private boolean confirmed;

    public MemoryUncertainRecovery() { }
    public String getExpectedHostBootId() { return expectedHostBootId; }
    public void setExpectedHostBootId(String value) { expectedHostBootId = value; }
    public String getExpectedPoolGeneration() { return expectedPoolGeneration; }
    public void setExpectedPoolGeneration(String value) { expectedPoolGeneration = value; }
    public String getDrainControlOperationUuid() { return drainControlOperationUuid; }
    public void setDrainControlOperationUuid(String value) { drainControlOperationUuid = value; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean value) { confirmed = value; }

    public static final class Adapter extends TypeAdapter<MemoryUncertainRecovery> {
        @Override public MemoryUncertainRecovery read(JsonReader in) throws IOException {
            Set<String> seen = StrictMaintenanceJson.begin(in);
            MemoryUncertainRecovery value = new MemoryUncertainRecovery();
            while (in.hasNext()) {
                switch (StrictMaintenanceJson.nextName(in, seen, "expectedHostBootId", "expectedPoolGeneration",
                        "drainControlOperationUuid", "confirmed")) {
                    case "expectedHostBootId": value.expectedHostBootId = StrictMaintenanceJson.string(in); break;
                    case "expectedPoolGeneration": value.expectedPoolGeneration = StrictMaintenanceJson.string(in); break;
                    case "drainControlOperationUuid": value.drainControlOperationUuid = StrictMaintenanceJson.string(in); break;
                    case "confirmed": value.confirmed = StrictMaintenanceJson.bool(in); break;
                    default: throw StrictMaintenanceJson.invalid("unknown member");
                }
            }
            StrictMaintenanceJson.end(in, seen, "expectedHostBootId", "expectedPoolGeneration",
                    "drainControlOperationUuid", "confirmed");
            return value;
        }

        @Override public void write(JsonWriter out, MemoryUncertainRecovery value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            out.beginObject();
            out.name("expectedHostBootId").value(value.expectedHostBootId);
            out.name("expectedPoolGeneration").value(value.expectedPoolGeneration);
            out.name("drainControlOperationUuid").value(value.drainControlOperationUuid);
            out.name("confirmed").value(value.confirmed);
            out.endObject();
        }
    }
}
