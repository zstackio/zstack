package org.zstack.sdk.network.zns;

import org.zstack.sdk.network.zns.ZnsCloudEffectiveOffer;
import org.zstack.sdk.network.zns.ZnsIntegrationProtocolDiagnosis;

public class GetZnsIntegrationEffectiveOfferResult {
    public ZnsCloudEffectiveOffer offer;
    public void setOffer(ZnsCloudEffectiveOffer offer) {
        this.offer = offer;
    }
    public ZnsCloudEffectiveOffer getOffer() {
        return this.offer;
    }

    public boolean notModified;
    public void setNotModified(boolean notModified) {
        this.notModified = notModified;
    }
    public boolean getNotModified() {
        return this.notModified;
    }

    public java.lang.String offerDigest;
    public void setOfferDigest(java.lang.String offerDigest) {
        this.offerDigest = offerDigest;
    }
    public java.lang.String getOfferDigest() {
        return this.offerDigest;
    }

    public long generation;
    public void setGeneration(long generation) {
        this.generation = generation;
    }
    public long getGeneration() {
        return this.generation;
    }

    public ZnsIntegrationProtocolDiagnosis diagnosis;
    public void setDiagnosis(ZnsIntegrationProtocolDiagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }
    public ZnsIntegrationProtocolDiagnosis getDiagnosis() {
        return this.diagnosis;
    }

}
