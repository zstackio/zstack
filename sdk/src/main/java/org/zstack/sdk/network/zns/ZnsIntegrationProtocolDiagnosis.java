package org.zstack.sdk.network.zns;



public class ZnsIntegrationProtocolDiagnosis  {

    public java.lang.Boolean peerManifestSupported;
    public void setPeerManifestSupported(java.lang.Boolean peerManifestSupported) {
        this.peerManifestSupported = peerManifestSupported;
    }
    public java.lang.Boolean getPeerManifestSupported() {
        return this.peerManifestSupported;
    }

    public java.lang.String peerVersion;
    public void setPeerVersion(java.lang.String peerVersion) {
        this.peerVersion = peerVersion;
    }
    public java.lang.String getPeerVersion() {
        return this.peerVersion;
    }

    public java.lang.String compatibilityProofDigest;
    public void setCompatibilityProofDigest(java.lang.String compatibilityProofDigest) {
        this.compatibilityProofDigest = compatibilityProofDigest;
    }
    public java.lang.String getCompatibilityProofDigest() {
        return this.compatibilityProofDigest;
    }

    public java.util.List capabilities;
    public void setCapabilities(java.util.List capabilities) {
        this.capabilities = capabilities;
    }
    public java.util.List getCapabilities() {
        return this.capabilities;
    }

    public java.util.List transitions;
    public void setTransitions(java.util.List transitions) {
        this.transitions = transitions;
    }
    public java.util.List getTransitions() {
        return this.transitions;
    }

}
