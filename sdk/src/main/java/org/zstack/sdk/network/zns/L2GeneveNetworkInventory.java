package org.zstack.sdk.network.zns;



public class L2GeneveNetworkInventory extends org.zstack.sdk.L2NetworkInventory {

    public java.lang.Integer geneveId;
    public void setGeneveId(java.lang.Integer geneveId) {
        this.geneveId = geneveId;
        if (this.vni == null) {
            this.vni = geneveId;
        }
    }
    public java.lang.Integer getGeneveId() {
        return this.geneveId;
    }

    public java.lang.Integer vni;
    public void setVni(java.lang.Integer vni) {
        this.vni = vni;
    }
    public java.lang.Integer getVni() {
        return this.vni;
    }

}
