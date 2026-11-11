package org.zstack.kvm;
import org.zstack.header.message.MessageReply;
import java.util.Map;
public class GetVmSkipTraceSnapshotReply extends MessageReply {
    public int version = 1;
    public String incarnation;
    public long revision;
    public Map<String,String> references;
    public Map<String,Long> orphanedAt;
    public long uninitializedLeftAt;
}
