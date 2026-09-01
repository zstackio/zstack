package org.zstack.sdk.network.zns;



public class ZnsCloudEffectiveOffer  {

    public int schemaVersion;
    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
    public int getSchemaVersion() {
        return this.schemaVersion;
    }

    public java.lang.String product;
    public void setProduct(java.lang.String product) {
        this.product = product;
    }
    public java.lang.String getProduct() {
        return this.product;
    }

    public java.lang.String clusterUuid;
    public void setClusterUuid(java.lang.String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
    public java.lang.String getClusterUuid() {
        return this.clusterUuid;
    }

    public java.lang.String scopeUuid;
    public void setScopeUuid(java.lang.String scopeUuid) {
        this.scopeUuid = scopeUuid;
    }
    public java.lang.String getScopeUuid() {
        return this.scopeUuid;
    }

    public long generation;
    public void setGeneration(long generation) {
        this.generation = generation;
    }
    public long getGeneration() {
        return this.generation;
    }

    public java.lang.String membershipDigest;
    public void setMembershipDigest(java.lang.String membershipDigest) {
        this.membershipDigest = membershipDigest;
    }
    public java.lang.String getMembershipDigest() {
        return this.membershipDigest;
    }

    public java.lang.String offerDigest;
    public void setOfferDigest(java.lang.String offerDigest) {
        this.offerDigest = offerDigest;
    }
    public java.lang.String getOfferDigest() {
        return this.offerDigest;
    }

    public boolean complete;
    public void setComplete(boolean complete) {
        this.complete = complete;
    }
    public boolean getComplete() {
        return this.complete;
    }

    public java.lang.String generatedAt;
    public void setGeneratedAt(java.lang.String generatedAt) {
        this.generatedAt = generatedAt;
    }
    public java.lang.String getGeneratedAt() {
        return this.generatedAt;
    }

    public java.util.List operations;
    public void setOperations(java.util.List operations) {
        this.operations = operations;
    }
    public java.util.List getOperations() {
        return this.operations;
    }

    public java.util.List events;
    public void setEvents(java.util.List events) {
        this.events = events;
    }
    public java.util.List getEvents() {
        return this.events;
    }

    public java.util.List domains;
    public void setDomains(java.util.List domains) {
        this.domains = domains;
    }
    public java.util.List getDomains() {
        return this.domains;
    }

}
