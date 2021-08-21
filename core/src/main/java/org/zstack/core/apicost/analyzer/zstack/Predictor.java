package org.zstack.core.apicost.analyzer.zstack;

import com.alibaba.fastjson.JSONObject;
import org.zstack.core.apicost.analyzer.configuration.Global;
import org.zstack.core.apicost.analyzer.entity.*;
import org.zstack.core.apicost.analyzer.service.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by huaxin on 2021/7/9.
 */
public class Predictor {

    private HashMap<String, StepGraph> apiGraph;

    private final ApiFinder apiFinder;
    private final ApiLogFinder apiLogFinder;
    private final StepLogFinder stepLogFinder;
    private final StepFinder stepFinder;
    private final RStepFinder rStepFinder;
    private final MsgLogFinder msgLogFinder;

    public Predictor() {
        this.apiFinder = new ApiFinder();
        this.apiLogFinder = new ApiLogFinder();
        this.stepLogFinder = new StepLogFinder();
        this.stepFinder = new StepFinder();
        this.rStepFinder = new RStepFinder();
        this.msgLogFinder = new MsgLogFinder();
        this.apiGraph = new HashMap<>();

        this.updateApiLogStepLog();
        this.buildStepGraph();
    }

    /**
     * 重建所有api的步骤图
     */
    private void buildStepGraph() {
        // 加载已有的api步骤图
        List<ApiVO> apiList = apiFinder.findAll();
        StepGraph stepGraph;
        for (ApiVO api: apiList) {
            stepGraph = new StepGraph(api.getApiId(), new ArrayList<>(), null,
                    apiLogFinder,
                    stepLogFinder,
                    stepFinder,
                    rStepFinder);
            stepGraph.loadTotalGraph();
            this.getApiGraph().put(api.getApiId(), stepGraph);
        }

        // 构建未分析的步骤图
        List<ApiLogVO> apiLogList = apiLogFinder.listApiLogsUnAnalyzed();
        for (ApiLogVO apiLog: apiLogList) {
            String apiId = apiLog.getApiId();
            if (null == apiFinder.findOne(apiId))
                apiFinder.save(apiId, apiLog.getName());
            this.getApiGraph().put(apiId, buildAGraph(apiId));
            apiFinder.updateLastUpdate(apiId, new Timestamp(org.zstack.header.message.DocUtils.date));
            apiLogFinder.updateIsAnalyzed(apiLog.getId(), ApiLogFinder.ANALYZED);
        }
    }



    /**
     * 重建某个api的步骤图
     */
    private StepGraph buildAGraph(String apiId) {
        StepGraph stepGraph = new StepGraph(apiId, new ArrayList<>(), null,
                apiLogFinder,
                stepLogFinder,
                stepFinder,
                rStepFinder);
        stepGraph.loadTotalGraph();
        stepGraph.rebuildGraph();
        stepGraph.saveTotalGraph();
        return stepGraph;
    }

    /**
     * 通过最大化路径方法，给出预测耗时
     * @param apiId 对象化了的api的apiId
     * @param curPath 当前已经执行了的步骤集合
     * @return
     * {
     *      steps: (int) 期望步骤数,
     *      time: (BigDecimal) 期望时间
     * }
     */
    public JSONObject predictByMaxPath(String apiId, List<StepLogVO> curPath) {
        // 计算已经走过的步骤的耗时与期望之差
        BigDecimal waitDiff4curPath = new BigDecimal("0");
        HashMap<String, BigDecimal> curPathWaited = new HashMap<>();
        for (StepLogVO step: curPath) {
            BigDecimal iWait = step.getWait();
            String stepId = step.getStepId();
            if (curPathWaited.containsKey(stepId))
                curPathWaited.put(stepId, curPathWaited.get(stepId).add(iWait));
            else
                curPathWaited.put(stepId, iWait);
        }
        for (String stepId: curPathWaited.keySet()) {
            BigDecimal diff = this.expectWaitOfStep(apiId, stepId).subtract(curPathWaited.get(stepId));
            if (diff.compareTo(new BigDecimal("0")) > 0)
                waitDiff4curPath = waitDiff4curPath.add(diff);
        }

        // 计算将要走的步骤的耗时期望
        String curStepId = curPath.get(curPath.size() - 1).getStepId();
        try {
            StepGraph stepGraph = this.getStepGraph(apiId);
            HashMap<String, Integer> sid2gid = stepGraph.getSid2gid();
            HashMap<Integer, String> gid2sid = stepGraph.getGid2sid();
            ArrayList<int[]> pseudoPathList = stepGraph.getPathList(sid2gid.get(curStepId));

            // 将要走的步骤中，不能出现已经走过的步骤(由于环的存在，在将要走的步骤中出现已经走过的步骤，是可能的)
            ArrayList<int[]> pathList = new ArrayList<>();
            // 所以需要将这些步骤去掉
            ArrayList<Integer> piList;
            for (int[] p: pseudoPathList) {
                piList = new ArrayList<>();
                for (int j : p) {
                    if (!curPathWaited.containsKey(gid2sid.get(j)))
                        piList.add(j);
                }
                int[] pi = new int[piList.size()];
                for (int i = 0; i < piList.size(); i++)
                    pi[i] = piList.get(i);
                pathList.add(pi);
            }

            ArrayList<BigDecimal> expectWaitedList = new ArrayList<>();
            ArrayList<BigDecimal> expectWaitedPath;
            for (int[] path : pathList) {
                if (path.length == 0) continue;

                // 从waited里面拿到每个步骤的期望耗时
                expectWaitedPath = new ArrayList<>();
                for (int i : path)
                    expectWaitedPath.add(this.expectWaitOfStep(apiId, gid2sid.get(i)));
                // 期望耗时求和
                BigDecimal pathWaited = new BigDecimal("0.0");
                for (BigDecimal e : expectWaitedPath)
                    pathWaited = pathWaited.add(e);
                expectWaitedList.add(pathWaited);
            }

            // 求出最大期望
            BigDecimal maxWaited = expectWaitedList.size() > 0 ? expectWaitedList.get(0) : new BigDecimal("0.0");
            int pathId = 0;
            for (int i = 0; i < expectWaitedList.size(); i++)
                if (maxWaited.compareTo(expectWaitedList.get(i)) < 0) {
                    maxWaited = expectWaitedList.get(i);
                    pathId = i;
                }

            JSONObject result = new JSONObject();
            // 时间期望为已执行步骤的期望和实际执行之差，与未执行步骤的期望之和
            result.put("time", maxWaited.add(waitDiff4curPath));
            result.put("steps", pathList.get(pathId).length);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将环路平均循环次数作为依据，计算所有步骤的耗时之和
     * @param apiId 对象化了的api的apiId
     * @param curPath 当前已经执行了的步骤集合
     * @return
     * {
     *      steps: (int) 期望步骤数,
     *      time: (BigDecimal) 期望时间
     * }
     */
    public JSONObject predictByMeanCircle(String apiId, List<StepLogVO> curPath) {
        StepGraph stepGraph = this.getStepGraph(apiId);
        Map<String, BigDecimal> expectStepCount = new HashMap<>();

        BigDecimal count;
        // 把环上的平均次数加上去
        for (Circle c: stepGraph.getCircles().values()) {
            for (String stepId: c.getOrderedSids()) {
                count = c.getMeanTimes();
                if (expectStepCount.containsKey(stepId))
                    count = count.add(expectStepCount.get(stepId));
                expectStepCount.put(stepId, count);
            }
        }
        // 把主干路径上的平均次数加上去
        ArrayList<String> mainPath = stepGraph.findMainPath();
        for (String stepId: mainPath) {
            count = new BigDecimal("1.0");
            if (expectStepCount.containsKey(stepId))
                count = expectStepCount.get(stepId).add(count);
            expectStepCount.put(stepId, count);
        }
        // 统计当前路径的各个步骤循环次数
        Map<String, Integer> curStepCount = new HashMap<>();
        Integer curCount;
        for (StepLogVO stepLog: curPath) {
            String stepId = stepLog.getStepId();
            curCount = 1;
            if (curStepCount.containsKey(stepId))
                curCount += curStepCount.get(stepId);
            curStepCount.put(stepId, curCount);
        }
        // 步骤循环次数之差
        Map<String, BigDecimal> diffStepCount = new HashMap<>();
        for (String stepId: expectStepCount.keySet()) {
            if (!curStepCount.containsKey(stepId)) {
                diffStepCount.put(stepId, expectStepCount.get(stepId));
                continue;
            }
            BigDecimal diff = expectStepCount.get(stepId).subtract(new BigDecimal(curStepCount.get(stepId)));
            diffStepCount.put(stepId, diff.compareTo(new BigDecimal("0.0")) > 0 ? diff : new BigDecimal("0.0"));
        }
        // 从步骤次数中计算步骤的总等待时间
        BigDecimal wait = new BigDecimal("0.0");
        int steps = 0;
        for (String stepId: diffStepCount.keySet()) {
            wait = wait.add(diffStepCount.get(stepId)
                            .multiply(stepGraph.getStepMap().get(stepId).getMeanWait()));
            steps += diffStepCount.get(stepId).setScale(0, BigDecimal.ROUND_UP).intValue();
        }

        JSONObject result = new JSONObject();
        // 时间期望为已执行步骤的期望和实际执行之差，与未执行步骤的期望之和
        result.put("time", wait);
        result.put("steps", steps);
        return result;
    }

    private BigDecimal expectWaitOfStep(String apiId, String stepId) {
        StepGraph stepGraph = this.getStepGraph(apiId);
        return stepGraph.getStepMap().get(stepId).getMeanWait();
    }

    // 更新步骤图
    private StepGraph getStepGraph(String apiId) {
        // TODO: 此处请求数据时，才去拿最新的步骤图，是否有更好的方法？

        // 把msgLog中的数据转到apiLog和stepLog中
        this.updateApiLogStepLog();

        // 如果api的lastUpdate超时了，就更新，否则就不更新
        boolean needUpdateStepGraph = new Date().getTime() >
                apiFinder.findOne(apiId).getLastUpdate().getTime() + (1000 * Global.STEP_GRAPH_UPDATE_PERIOD);
        if (needUpdateStepGraph) {
            this.apiGraph.put(apiId, buildAGraph(apiId));
            apiFinder.updateLastUpdate(apiId, new Timestamp(org.zstack.header.message.DocUtils.date));
        }

        return this.getApiGraph().get(apiId);
    }

    // 重新计算msgLog中的wait
    private void reComputeMsgLogWaitOfOneApiId(String originApiId) {
        // 按时间顺序，取出这个apiId的所有msg
        List<MsgLogVO> msgLogs = msgLogFinder.listOrderedMsgLogsByApiId(originApiId);
        List<MsgLogVO> updatedMsgLogs = new ArrayList<>();

        // 重新计算每个步骤的实际耗时：该步骤的应答时间减去其一级子步骤的耗时
        MsgLogVO toComputeLog, directChild, iLog;
        List<MsgLogVO> children;
        for (int i = 0; i < msgLogs.size(); i++) {
            toComputeLog = msgLogs.get(i);
            directChild = null;
            children = new ArrayList<>();
            for (int j = i + 1; j < msgLogs.size(); j++) {
                iLog = msgLogs.get(j);
                if (iLog.getStartTime() > toComputeLog.getReplyTime())
                    break;

                if (directChild != null && iLog.getStartTime() < directChild.getReplyTime())
                    continue;

                directChild = iLog;
                children.add(iLog);
            }

            BigDecimal wait = toComputeLog.getWait();
            BigDecimal childrenWait = new BigDecimal("0");
            for (MsgLogVO c: children)
                childrenWait = childrenWait.add(c.getWait());
            toComputeLog.setWait(wait.subtract(childrenWait));
            updatedMsgLogs.add(toComputeLog);
        }

        for (MsgLogVO m: updatedMsgLogs)
            msgLogFinder.update(m);
    }

    // 把msgLog中的数据转到apiLog和stepLog中
    private void updateApiLogStepLog() {
        // 取出所有未更新的apiId
        int pageSize = 50;
        int pageNum = 1;
        List<MsgLogVO> msgLogHeaders = msgLogFinder.listNotUpdateApiIds(pageNum, pageSize);
        // 遍历apiId
        while (msgLogHeaders.size() != 0) {
            for (MsgLogVO header: msgLogHeaders) {
                String originApiId = header.getApiId();
                this.reComputeMsgLogWaitOfOneApiId(originApiId);
                this.updateOneApiId(originApiId);
            }
            // 查询下一页msgLog
            msgLogHeaders = msgLogFinder.listNotUpdateApiIds(pageNum++, pageSize);
        }
    }

    // 更新一个原始apiId的msgLog
    private void updateOneApiId(String originApiId) {
        // 按时间顺序，取出这个apiId的所有msg
        List<MsgLogVO> msgLogs = msgLogFinder.listOrderedMsgLogsByApiId(originApiId);

        // 如果第一个msgLog已经更新过了，就把没更新的筛选出来，写入stepLogVO表
        // 如果第一个msgLog没有更新过，就把第一个写入apiLogVO，并且全部写入stepLogVO
        MsgLogVO toApiLog = null;
        List<MsgLogVO> toStepLog = new ArrayList<>();
        if (msgLogs.get(0).getStatus() == MsgLogFinder.UPDATED) {
            for (MsgLogVO msgLog: msgLogs) {
                if (msgLog.getStatus() == MsgLogFinder.NOT_UPDATE)
                    toStepLog.add(msgLog);
            }
        } else {
            toApiLog = msgLogs.get(0);
            toStepLog.addAll(msgLogs);
        }

        ApiLogVO apiLog = null;
        // 写apiLog表
        if (null != toApiLog) {
            // 用msgName即msg的类名做为api的id，而msg身上的apiId，每次调用api都会不一样，只作为附加信息originApiId
            apiLog = apiLogFinder.save(toApiLog.getMsgName(), toApiLog.getMsgName(), originApiId);
            // 把所有写完的msg都改成已更新
            msgLogFinder.updateStatus(toApiLog.getId(), MsgLogFinder.UPDATED);
        }

        // 写stepLog表
        if (null == apiLog)
            apiLog = apiLogFinder.findByOriginApiId(originApiId);
        if (toStepLog.size() != 0) {
            for (MsgLogVO msg: toStepLog) {
                String stepId = msg.getMsgName() + "@" + apiLog.getApiId();
                stepLogFinder.save(stepId, msg.getMsgName(), apiLog.getId(),
                        msg.getStartTime(), msg.getReplyTime(), msg.getWait());
                // 把所有写完的msg都改成已更新
                msgLogFinder.updateStatus(msg.getId(), MsgLogFinder.UPDATED);
            }
        }
    }

    private HashMap<String, StepGraph> getApiGraph() {
        return this.apiGraph;
    }

    public void setApiGraph(HashMap<String, StepGraph> apiGraph) {
        this.apiGraph = apiGraph;
    }
}
