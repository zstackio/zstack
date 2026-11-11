package org.zstack.kvm;

import java.util.*;

/** In-memory references. Snapshot installation and event application share one monitor. */
class VmSkipTraceRegistry {
    static class Change {
        final String incarnation, messageId, vmUuid;
        final long revision;
        final boolean skip;
        Change(String incarnation,long revision,String messageId,String vmUuid,boolean skip) {
            this.incarnation=incarnation;this.revision=revision;this.messageId=messageId;this.vmUuid=vmUuid;this.skip=skip;
        }
    }
    static class Source {
        String incarnation;
        long revision;
        boolean ready, legacy;
        final Map<String,String> references=new HashMap<>();
        final List<Change> pending=new ArrayList<>();
        final Set<String> retired=new HashSet<>();
    }
    private final String local;
    private final Map<String,Source> sources=new HashMap<>();
    private final Map<String,Long> orphanedAt=new HashMap<>();
    private long uninitializedLeftAt;
    VmSkipTraceRegistry(String local) {
        this.local=local;
        Source source=new Source();source.incarnation=UUID.randomUUID().toString();source.ready=true;sources.put(local,source);
    }
    synchronized Source join(String node) {return sources.computeIfAbsent(node,key->new Source());}
    synchronized Change localChange(String id,String vm,boolean skip) {
        Source source=sources.get(local);
        if(skip&&Objects.equals(source.references.get(id),vm)||!skip&&!source.references.containsKey(id))return null;
        if(skip)source.references.put(id,vm);else vm=source.references.remove(id);
        return new Change(source.incarnation,++source.revision,id,vm,skip);
    }
    synchronized boolean legacy(String node,Source expected) {
        Source source=sources.get(node);
        // Only an explicit unsupported RPC response from this membership may downgrade.
        if(source==null || source!=expected || source.incarnation!=null || !source.pending.isEmpty())return false;
        source.legacy=true;source.ready=true;return true;
    }
    synchronized void receiveLegacy(String node,String api,String vm,boolean skip) {
        if(local.equals(node))return;
        Source source=sources.get(node);
        if(source==null || source.incarnation!=null || !source.pending.isEmpty())return;
        String key=api!=null ? "legacy-api:"+api : vm!=null ? "legacy-vm:"+vm : null;
        if(key==null || skip && vm==null)return;
        if(skip)source.references.put(key,vm);else source.references.remove(key);
    }
    synchronized void receive(String node,Change change) {
        if(local.equals(node))return; // already applied before publishing
        Source source=sources.get(node);
        if(source==null||source.retired.contains(change.incarnation))return;
        if(change.incarnation==null||change.messageId==null) {source.ready=false;return;}
        if(source.ready&&Objects.equals(source.incarnation,change.incarnation)&&change.revision<=source.revision)return;
        if(source.legacy) {source.legacy=false;source.ready=false;}
        source.pending.add(change);
        if(source.pending.size()>1024) {source.pending.clear();source.ready=false;return;}
        if(source.ready)drain(source);
    }
    private void drain(Source source) {
        source.pending.sort(Comparator.comparingLong(c->c.revision));
        Iterator<Change> iterator=source.pending.iterator();
        while(iterator.hasNext()) {
            Change change=iterator.next();
            if(!Objects.equals(change.incarnation,source.incarnation)) {source.ready=false;return;}
            if(change.revision<=source.revision) {iterator.remove();continue;}
            if(change.revision!=source.revision+1) {source.ready=false;return;}
            if(change.skip)source.references.put(change.messageId,change.vmUuid);else source.references.remove(change.messageId);
            source.revision=change.revision;iterator.remove();
        }
    }
    synchronized GetVmSkipTraceSnapshotReply snapshot() {
        Source source=sources.get(local);
        GetVmSkipTraceSnapshotReply reply=new GetVmSkipTraceSnapshotReply();reply.incarnation=source.incarnation;
        reply.revision=source.revision;reply.references=new HashMap<>(source.references);reply.orphanedAt=new HashMap<>(orphanedAt);
        reply.uninitializedLeftAt=uninitializedLeftAt;
        return reply;
    }
    synchronized boolean install(String node,Source expected,GetVmSkipTraceSnapshotReply snapshot) {
        Source source=sources.get(node);
        // A reply from before nodeLeft/nodeJoin must never revive the departed source.
        if(source==null||source!=expected||snapshot.version!=1||snapshot.incarnation==null||snapshot.references==null)return false;
        if(source.retired.contains(snapshot.incarnation))return false;
        if(Objects.equals(source.incarnation,snapshot.incarnation)&&snapshot.revision<source.revision)return false;
        if(source.incarnation!=null&&!source.incarnation.equals(snapshot.incarnation))source.retired.add(source.incarnation);
        source.legacy=false;source.incarnation=snapshot.incarnation;source.revision=snapshot.revision;
        source.references.clear();source.references.putAll(snapshot.references);
        source.pending.removeIf(change->{
            if(!snapshot.incarnation.equals(change.incarnation)) {source.retired.add(change.incarnation);return true;}
            return false;
        });
        if(snapshot.orphanedAt!=null)snapshot.orphanedAt.forEach((vm,at)->orphanedAt.merge(vm,at,Math::min));
        uninitializedLeftAt=Math.max(uninitializedLeftAt,snapshot.uninitializedLeftAt);
        source.ready=true;drain(source);return source.ready;
    }
    synchronized void left(String node,long now) {
        if(local.equals(node))return;
        Source source=sources.remove(node);
        if(source==null)return;
        if(!source.ready)uninitializedLeftAt=now;
        Set<String> vms=new HashSet<>(source.references.values());
        for(Change change:source.pending)if(change.skip)vms.add(change.vmUuid);
        // A new departure starts the existing orphan grace period. Snapshot
        // replay uses the original timestamp and never starts another period.
        for(String vm:vms)orphanedAt.put(vm,now);
    }
    synchronized Map<String,Source> pendingSources() {
        Map<String,Source> result=new HashMap<>();sources.forEach((node,source)->{if(!source.ready)result.put(node,source);});return result;
    }
    synchronized boolean ready() {return sources.values().stream().allMatch(source->source.ready);}
    synchronized boolean ready(long now,long ttl) {return ready()&&(uninitializedLeftAt==0||now-uninitializedLeftAt>=ttl);}
    synchronized Set<String> skipped(long now,long ttl) {
        orphanedAt.entrySet().removeIf(entry->now-entry.getValue()>=ttl);
        Set<String> result=new HashSet<>(orphanedAt.keySet());
        for(Source source:sources.values()) {
            result.addAll(source.references.values());
            // An incomplete snapshot still supplies conservative positive evidence.
            for(Change change:source.pending)if(change.skip)result.add(change.vmUuid);
        }
        return result;
    }
}
