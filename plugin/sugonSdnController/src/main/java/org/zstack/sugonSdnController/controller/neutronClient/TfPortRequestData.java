package org.zstack.sugonSdnController.controller.neutronClient;
public class TfPortRequestData {
    private TfPortRequestResource resource;
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TfPortRequestResource getResource() {
        return resource;
    }

    public void setResource(TfPortRequestResource resource) {
        this.resource = resource;
    }

}
