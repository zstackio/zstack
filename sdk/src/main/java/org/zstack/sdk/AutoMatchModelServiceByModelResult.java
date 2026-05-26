package org.zstack.sdk;

import org.zstack.sdk.MatchedStep;
import org.zstack.sdk.MatchEvidence;

public class AutoMatchModelServiceByModelResult {
    public java.lang.String recommendedServiceUuid;
    public void setRecommendedServiceUuid(java.lang.String recommendedServiceUuid) {
        this.recommendedServiceUuid = recommendedServiceUuid;
    }
    public java.lang.String getRecommendedServiceUuid() {
        return this.recommendedServiceUuid;
    }

    public MatchedStep matchedByStep;
    public void setMatchedByStep(MatchedStep matchedByStep) {
        this.matchedByStep = matchedByStep;
    }
    public MatchedStep getMatchedByStep() {
        return this.matchedByStep;
    }

    public MatchEvidence evidence;
    public void setEvidence(MatchEvidence evidence) {
        this.evidence = evidence;
    }
    public MatchEvidence getEvidence() {
        return this.evidence;
    }

    public java.lang.String recommendedGpuSpecUuid;
    public void setRecommendedGpuSpecUuid(java.lang.String recommendedGpuSpecUuid) {
        this.recommendedGpuSpecUuid = recommendedGpuSpecUuid;
    }
    public java.lang.String getRecommendedGpuSpecUuid() {
        return this.recommendedGpuSpecUuid;
    }

}
