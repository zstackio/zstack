package org.zstack.header.network.l2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkDeletionContext {
    public enum Origin {
        WHOLE_L2_SEGMENT_DELETE
    }

    private Origin origin;
    private String operationUuid;
    private String l2NetworkUuid;
    private String rootIssuer;
    private boolean forceDelete;
    private boolean sourceUnavailableCleanup;
    private Long expectedConfigVersion;
    private String expectedOwnedConfigDigest;
    private String expectedDependencySnapshotDigest;
    private List<String> expectedCloudChildUuids = Collections.emptyList();
    private boolean remoteCommitted;
    private String continuationStep;

    public NetworkDeletionContext() {
    }

    public NetworkDeletionContext(Origin origin, String operationUuid, String l2NetworkUuid,
                                  String rootIssuer) {
        this.origin = origin;
        this.operationUuid = operationUuid;
        this.l2NetworkUuid = l2NetworkUuid;
        this.rootIssuer = rootIssuer;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public String getOperationUuid() {
        return operationUuid;
    }

    public void setOperationUuid(String operationUuid) {
        this.operationUuid = operationUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getRootIssuer() {
        return rootIssuer;
    }

    public void setRootIssuer(String rootIssuer) {
        this.rootIssuer = rootIssuer;
    }

    public boolean isWholeL2SegmentDelete() {
        return Origin.WHOLE_L2_SEGMENT_DELETE == origin;
    }

    public boolean isForceDelete() {
        return forceDelete;
    }

    public void setForceDelete(boolean forceDelete) {
        this.forceDelete = forceDelete;
    }

    public boolean isSourceUnavailableCleanup() {
        return sourceUnavailableCleanup;
    }

    public void setSourceUnavailableCleanup(boolean sourceUnavailableCleanup) {
        this.sourceUnavailableCleanup = sourceUnavailableCleanup;
    }

    public Long getExpectedConfigVersion() {
        return expectedConfigVersion;
    }

    public void setExpectedConfigVersion(Long expectedConfigVersion) {
        this.expectedConfigVersion = expectedConfigVersion;
    }

    public String getExpectedOwnedConfigDigest() {
        return expectedOwnedConfigDigest;
    }

    public void setExpectedOwnedConfigDigest(String expectedOwnedConfigDigest) {
        this.expectedOwnedConfigDigest = expectedOwnedConfigDigest;
    }

    public String getExpectedDependencySnapshotDigest() {
        return expectedDependencySnapshotDigest;
    }

    public void setExpectedDependencySnapshotDigest(String expectedDependencySnapshotDigest) {
        this.expectedDependencySnapshotDigest = expectedDependencySnapshotDigest;
    }

    public List<String> getExpectedCloudChildUuids() {
        return expectedCloudChildUuids;
    }

    public void setExpectedCloudChildUuids(List<String> expectedCloudChildUuids) {
        List<String> copy = expectedCloudChildUuids == null
                ? new ArrayList<>() : new ArrayList<>(expectedCloudChildUuids);
        Collections.sort(copy);
        this.expectedCloudChildUuids = Collections.unmodifiableList(copy);
    }

    public boolean isRemoteCommitted() {
        return remoteCommitted;
    }

    public void setRemoteCommitted(boolean remoteCommitted) {
        this.remoteCommitted = remoteCommitted;
    }

    public String getContinuationStep() {
        return continuationStep;
    }

    public void setContinuationStep(String continuationStep) {
        this.continuationStep = continuationStep;
    }
}
