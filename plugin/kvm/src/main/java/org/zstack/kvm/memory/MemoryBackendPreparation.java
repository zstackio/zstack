package org.zstack.kvm.memory;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Set;

@org.zstack.header.rest.SDK
@JsonAdapter(value = MemoryBackendPreparation.Adapter.class, nullSafe = false)
public final class MemoryBackendPreparation {
    private String candidateId;
    private String expectedHostBootId;
    private String expectedPoolGeneration;
    private long backendCapacityBytes;
    private boolean resetConfirmed;

    public MemoryBackendPreparation() { }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String value) { candidateId = value; }
    public String getExpectedHostBootId() { return expectedHostBootId; }
    public void setExpectedHostBootId(String value) { expectedHostBootId = value; }
    public String getExpectedPoolGeneration() { return expectedPoolGeneration; }
    public void setExpectedPoolGeneration(String value) { expectedPoolGeneration = value; }
    public long getBackendCapacityBytes() { return backendCapacityBytes; }
    public void setBackendCapacityBytes(long value) { backendCapacityBytes = value; }
    public boolean isResetConfirmed() { return resetConfirmed; }
    public void setResetConfirmed(boolean value) { resetConfirmed = value; }

    public static final class Adapter extends TypeAdapter<MemoryBackendPreparation> {
        @Override public MemoryBackendPreparation read(JsonReader in) throws IOException {
            Set<String> seen = StrictMaintenanceJson.begin(in);
            MemoryBackendPreparation value = new MemoryBackendPreparation();
            while (in.hasNext()) {
                switch (StrictMaintenanceJson.nextName(in, seen, "candidateId", "expectedHostBootId",
                        "expectedPoolGeneration", "backendCapacityBytes", "resetConfirmed")) {
                    case "candidateId": value.candidateId = StrictMaintenanceJson.string(in); break;
                    case "expectedHostBootId": value.expectedHostBootId = StrictMaintenanceJson.string(in); break;
                    case "expectedPoolGeneration": value.expectedPoolGeneration = StrictMaintenanceJson.string(in); break;
                    case "backendCapacityBytes": value.backendCapacityBytes = StrictMaintenanceJson.nonNegativeInteger(in); break;
                    case "resetConfirmed": value.resetConfirmed = StrictMaintenanceJson.bool(in); break;
                    default: throw StrictMaintenanceJson.invalid("unknown member");
                }
            }
            StrictMaintenanceJson.end(in, seen, "candidateId", "expectedHostBootId", "expectedPoolGeneration",
                    "backendCapacityBytes", "resetConfirmed");
            return value;
        }

        @Override public void write(JsonWriter out, MemoryBackendPreparation value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            out.beginObject();
            out.name("candidateId").value(value.candidateId);
            out.name("expectedHostBootId").value(value.expectedHostBootId);
            out.name("expectedPoolGeneration").value(value.expectedPoolGeneration);
            out.name("backendCapacityBytes").value(value.backendCapacityBytes);
            out.name("resetConfirmed").value(value.resetConfirmed);
            out.endObject();
        }
    }
}
