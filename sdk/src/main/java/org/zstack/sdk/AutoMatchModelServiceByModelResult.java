package org.zstack.sdk;

public class AutoMatchModelServiceByModelResult {
    /**
     * UUID of the recommended ModelServiceVO. May be null if FALLBACK step
     * found no Transformers candidate.
     */
    public java.lang.String recommendedServiceUuid;
    public void setRecommendedServiceUuid(java.lang.String recommendedServiceUuid) {
        this.recommendedServiceUuid = recommendedServiceUuid;
    }
    public java.lang.String getRecommendedServiceUuid() {
        return this.recommendedServiceUuid;
    }

    /**
     * Which matching step produced this recommendation:
     * USER_PRESET | FILE_FORMAT | PIPELINE_TAG | FALLBACK
     */
    public java.lang.String matchedByStep;
    public void setMatchedByStep(java.lang.String matchedByStep) {
        this.matchedByStep = matchedByStep;
    }
    public java.lang.String getMatchedByStep() {
        return this.matchedByStep;
    }

    /**
     * Diagnostic evidence for the match decision.
     */
    public java.util.LinkedHashMap evidence;
    public void setEvidence(java.util.LinkedHashMap evidence) {
        this.evidence = evidence;
    }
    public java.util.LinkedHashMap getEvidence() {
        return this.evidence;
    }

}
