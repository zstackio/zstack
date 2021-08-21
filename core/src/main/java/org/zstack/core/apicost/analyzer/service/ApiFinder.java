package org.zstack.core.apicost.analyzer.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.apicost.analyzer.entity.ApiVO;
import org.zstack.core.apicost.analyzer.entity.ApiVO_;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;

import java.sql.Timestamp;
import java.util.List;

/**
 * Created by huaxin on 2021/7/9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApiFinder {

    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public List<ApiVO> findAll() {
        return dbf.listAll(ApiVO.class);
    }

    @Transactional
    public ApiVO findOne(String apiId) {
        return Q.New(ApiVO.class).eq(ApiVO_.apiId, apiId).find();
    }

    @Transactional
    public ApiVO save(String apiId, String name) {
        ApiVO api = new ApiVO();
        api.setUuid(Platform.getUuid());
        api.setApiId(apiId);
        api.setName(name);
        return dbf.persist(api);
    }

    @Transactional
    public void updateLastUpdate(String apiId, Timestamp lastUpdate) {
        ApiVO api = findOne(apiId);
        api.setLastUpdate(lastUpdate);
        dbf.update(api);
    }

}
