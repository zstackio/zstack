package org.zstack.core.apicost.analyzer;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.zstack.core.apicost.analyzer.configuration.Global;
import org.zstack.core.apicost.analyzer.entity.MsgLogVO;
import org.zstack.core.apicost.analyzer.entity.StepLogVO;
import org.zstack.core.apicost.analyzer.service.MsgLogFinder;
import org.zstack.core.apicost.analyzer.zstack.PredictResult;

import java.util.ArrayList;
import java.util.List;

public class ApiSpendTimePredictor {

    private static JSONObject getCurStep(String apiId) throws Exception {
        List<MsgLogVO> msgLogVOList = new MsgLogFinder().listOrderedMsgLogsByApiId(apiId);
        if (msgLogVOList.size() == 0)
            throw new Exception(String.format("apiId=%s not found.", apiId));

        // msgLog to stepLog
        String ApiVO_apiId = msgLogVOList.get(0).getMsgName();
        List<StepLogVO> stepLogs = new ArrayList<>();
        StepLogVO iStep;
        for (MsgLogVO msgLog: msgLogVOList) {
            iStep = new StepLogVO();
            iStep.setStepId(msgLog.getMsgName() + "@" + ApiVO_apiId);
            iStep.setWait(msgLog.getWait());
            iStep.setStartTime(msgLog.getStartTime());
            iStep.setEndTime(msgLog.getReplyTime());
            iStep.setName(msgLog.getMsgName());
            stepLogs.add(iStep);
        }

        String stepMsg = msgLogVOList.get(msgLogVOList.size() - 1).getMsgName();
        String stepId = stepMsg + "@" + ApiVO_apiId;
        JSONObject result = new JSONObject();
        result.put("stepMsg", stepMsg);
        result.put("steps", stepLogs);
        result.put("finishedSteps", msgLogVOList.size());
        result.put("ApiVO.apiId", ApiVO_apiId);
        return result;
    }

    /**
     * 用最大化路径法，给出预测耗时
     * @param apiId api请求的uuid
     * @return PredictResult
     */
    public static PredictResult predictByMaxPath(String apiId) {
        // 返回预计耗时，预计步骤数，已完成步骤数
        JSONObject curStep = null;
        try {
            curStep = getCurStep(apiId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        JSONObject predict = Global.PREDICTOR.predictByMeanCircle(
                curStep.getString("ApiVO.apiId"),
                curStep.getObject("steps", new TypeReference<ArrayList<StepLogVO>>(){}));

        PredictResult predictResult = new PredictResult();
        predictResult.setSpendTime(predict.getBigDecimal("time"));
        predictResult.setCurApiClass(curStep.getString("ApiVO.apiId"));
        predictResult.setCurStepClass(curStep.getString("stepMsg"));
        predictResult.setFinishedSteps(curStep.getInteger("finishedSteps"));
        predictResult.setUnFinishedSteps(predict.getInteger("steps"));
        return predictResult;
    }

}
