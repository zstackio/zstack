package org.zstack.sugonSdnController.controller.neutronClient;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.zstack.core.db.Q;
import org.zstack.sdnController.header.SdnControllerConstant;
import org.zstack.sdnController.header.SdnControllerVO;
import org.zstack.sdnController.header.SdnControllerVO_;
import org.zstack.sugonSdnController.controller.SugonSdnControllerGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TfPortClient {
    private static final CLogger logger = Utils.getLogger(TfPortClient.class);

    public TfPortResponse createPort(String l2Id, String l3Id, String mac, String ip, String accountId,String vmInventeryId) {
        TfPortRequestBody portRequestBodyEO = new TfPortRequestBody();
        TfPortRequestData portRequestDataEO = new TfPortRequestData();
        TfPortRequestContext portRequestContextEO = new TfPortRequestContext();
        portRequestContextEO.setOperation("CREATE");
        portRequestContextEO.setIs_admin("True");
        portRequestContextEO.setTenant_id(accountId);
        TfPortRequestResource requestPortResourceEntity = new TfPortRequestResource();
        requestPortResourceEntity.setNetworkId(l2Id);
        requestPortResourceEntity.setSubnetId(l3Id);
        requestPortResourceEntity.setTenantId(accountId);
        requestPortResourceEntity.setMacAddress(mac);
        requestPortResourceEntity.setDeviceId(vmInventeryId);
        TfPortIpEntity ipEntity = new TfPortIpEntity();
        ipEntity.setIpAddress(ip);
        ipEntity.setSubnetId(l3Id);
        List<TfPortIpEntity> ipEntities = new ArrayList<>();
        ipEntities.add(ipEntity);
        requestPortResourceEntity.setFixdIps(ipEntities);
        portRequestDataEO.setResource(requestPortResourceEntity);
        portRequestBodyEO.setData(portRequestDataEO);
        portRequestBodyEO.setContext(portRequestContextEO);
        return getResponsePortEntity(portRequestBodyEO);
    }

    public TfPortResponse deletePort(String portId, String accountId) {
        TfPortRequestBody portRequestBodyEO = new TfPortRequestBody();
        TfPortRequestData portRequestDataEO = new TfPortRequestData();
        TfPortRequestContext portRequestContextEO = new TfPortRequestContext();
        portRequestContextEO.setOperation("DELETE");
        portRequestContextEO.setIs_admin("True");
        portRequestContextEO.setTenant_id(accountId);
        portRequestDataEO.setId(portId);
        portRequestBodyEO.setData(portRequestDataEO);
        portRequestBodyEO.setContext(portRequestContextEO);
        return getResponsePortEntity(portRequestBodyEO);
    }

    private TfPortResponse getResponsePortEntity(TfPortRequestBody portRequestBodyEO) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Gson gson = gsonBuilder.create();
        String requestStr = gson.toJson(portRequestBodyEO);
        Type type = new TypeToken<TfPortResponse>() {
        }.getType();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .build();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(mediaType, requestStr);
        SdnControllerVO sdn = Q.New(SdnControllerVO.class).eq(SdnControllerVO_.vendorType, SdnControllerConstant.TF_CONTROLLER).find();
        String url = "http://" + sdn.getIp() + ":" + SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT + "/neutron/port";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);
        try {
            Response httpResponse = call.execute();
            String res = (Objects.requireNonNull(httpResponse.body()).contentLength() > 0) ? Objects.requireNonNull(httpResponse.body()).string() : "{\"code\":200}";
            TfPortResponse portResponse = gson.fromJson(res, type);
            portResponse.setCode(httpResponse.code());
            httpResponse.close();
            return portResponse;
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException("failed invoking tf " + portRequestBodyEO.getContext().getOperation().toLowerCase() + " port ");
        }
    }
}
