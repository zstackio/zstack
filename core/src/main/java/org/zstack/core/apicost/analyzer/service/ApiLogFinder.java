package org.zstack.core.apicost.analyzer.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.apicost.analyzer.entity.ApiLogVO;
import org.zstack.core.apicost.analyzer.entity.ApiLogVO_;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huaxin on 2021/7/9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApiLogFinder {

    public static int ANALYZED = 1;
    public static int NOT_ANALYZED = 0;

    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public List<ApiLogVO> listApiLogs(int pageNum, int pageSize) {
        return dbf.listAll((pageNum - 1) * pageSize, pageSize, ApiLogVO.class);
    }

    @Transactional
    public List<ApiLogVO> listApiLogsByQuery(String apiId, int isAnalyzed,
                                             int pageNum, int pageSize) {
        return Q.New(ApiLogVO.class)
                .eq(ApiLogVO_.apiId, apiId)
                .eq(ApiLogVO_.isAnalyzed, isAnalyzed)
                .start((pageNum - 1) * pageSize)
                .limit(pageSize).list();
    }

    @Transactional
    public List<ApiLogVO> listApiLogsUnAnalyzed() {
        return Q.New(ApiLogVO.class).eq(ApiLogVO_.isAnalyzed, NOT_ANALYZED).list();
    }

    @Transactional
    public void updateIsAnalyzed(Long apiLogId, int isAnalyzed) {
        ApiLogVO apiLog = dbf.findById(apiLogId, ApiLogVO.class);
        apiLog.setIsAnalyzed(isAnalyzed);
        dbf.update(apiLog);
    }

    @Transactional
    public ApiLogVO findByOriginApiId(String originApiId) {
        return Q.New(ApiLogVO_.class).eq(ApiLogVO_.originApiId, originApiId).find();
    }

    @Transactional
    public ApiLogVO save(String apiId, String name, String originApiId) {
        ApiLogVO apiLog = new ApiLogVO();
        apiLog.setApiId(apiId);
        apiLog.setName(name);
        apiLog.setOriginApiId(originApiId);
        apiLog.setUuid(Platform.getUuid());
        apiLog.setIsAnalyzed(NOT_ANALYZED);
        return dbf.persist(apiLog);
    }

    @Transactional
    public Integer countApiLogGroupByApiId(String apiId) {
        return Q.New(ApiLogVO.class)
                .eq(ApiLogVO_.apiId, apiId)
                .list().size();
    }

}
