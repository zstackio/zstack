package org.zstack.core.apicost.analyzer.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.apicost.analyzer.entity.RStepVO;
import org.zstack.core.apicost.analyzer.entity.RStepVO_;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by huaxin on 2021/7/9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RStepFinder {

    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public List<RStepVO> findAll() {
        return dbf.listAll(RStepVO.class);
    }

    @Transactional
    public List<RStepVO> findByApiId(String apiId) {
        return Q.New(RStepVO.class).eq(RStepVO_.apiId, apiId).list();
    }

    @Transactional
    public List<RStepVO> listAllRStepByApiId(String apiId) {
        return Q.New(RStepVO.class).eq(RStepVO_.apiId, apiId).list();
    }

    @Transactional
    public RStepVO save(RStepVO s) {
        return dbf.persist(s);
    }

    @Transactional
    public RStepVO save(String apiId, String fromStepId, String toStepId, BigDecimal weight) {
        RStepVO rStep = new RStepVO();
        rStep.setApiId(apiId);
        rStep.setUuid(Platform.getUuid());
        rStep.setFromStepId(fromStepId);
        rStep.setToStepId(toStepId);
        rStep.setWeight(weight);
        return this.save(rStep);
    }

    @Transactional
    public void deleteByApiId(String apiId) {
        List<RStepVO> rStepVOs = findByApiId(apiId);
        if (null != rStepVOs && 0 != rStepVOs.size())
            dbf.removeCollection(rStepVOs, RStepVO.class);
    }

}
