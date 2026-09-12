package org.zstack.sdk;



public class ZsDatasetImageImportInventory  {

    public java.lang.String spaceUuid;
    public void setSpaceUuid(java.lang.String spaceUuid) {
        this.spaceUuid = spaceUuid;
    }
    public java.lang.String getSpaceUuid() {
        return this.spaceUuid;
    }

    public long imported;
    public void setImported(long imported) {
        this.imported = imported;
    }
    public long getImported() {
        return this.imported;
    }

    public java.util.List rejected;
    public void setRejected(java.util.List rejected) {
        this.rejected = rejected;
    }
    public java.util.List getRejected() {
        return this.rejected;
    }

    public java.util.List items;
    public void setItems(java.util.List items) {
        this.items = items;
    }
    public java.util.List getItems() {
        return this.items;
    }

}
