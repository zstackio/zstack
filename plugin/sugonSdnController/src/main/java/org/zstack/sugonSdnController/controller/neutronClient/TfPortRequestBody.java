package org.zstack.sugonSdnController.controller.neutronClient;
public class TfPortRequestBody {
    private TfPortRequestData data;

    private TfPortRequestContext context;

    public TfPortRequestData getData() {
        return data;
    }

    public void setData(TfPortRequestData data) {
        this.data = data;
    }

    public TfPortRequestContext getContext() {
        return context;
    }

    public void setContext(TfPortRequestContext context) {
        this.context = context;
    }
}
