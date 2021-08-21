package org.zstack.core.apicost.analyzer.zstack;

import org.zstack.core.apicost.analyzer.entity.*;
import org.zstack.core.apicost.analyzer.service.ApiLogFinder;
import org.zstack.core.apicost.analyzer.service.RStepFinder;
import org.zstack.core.apicost.analyzer.service.StepFinder;
import org.zstack.core.apicost.analyzer.service.StepLogFinder;
import org.zstack.core.apicost.analyzer.utils.Graph;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by huaxin on 2021/7/9.
 */
public class StepGraph extends Graph {

    private String apiId;
    private String startStep;
    private Integer apiLogCount;
    private HashMap<String, StepVO> stepMap;
    private Map<String, Circle> circles;

    private final ApiLogFinder apiLogFinder;
    private final StepLogFinder stepLogFinder;
    private final StepFinder stepFinder;
    private final RStepFinder rStepFinder;

    StepGraph(String apiId, List<String> sidSet, String startStep,
              ApiLogFinder apiLogFinder,
              StepLogFinder stepLogFinder,
              StepFinder stepFinder,
              RStepFinder rStepFinder) {
        super(sidSet);

        this.apiLogFinder = apiLogFinder;
        this.stepFinder = stepFinder;
        this.rStepFinder = rStepFinder;
        this.stepLogFinder = stepLogFinder;

        this.apiId = apiId;
        this.startStep = startStep;
        this.apiLogCount = 1;
        this.stepMap = new HashMap<>();
        this.circles = new HashMap<>();
    }

    /**
     * 构建一个apiLog的步骤图
     */
    private StepGraph buildGraphBySteps(ApiLogVO apiLog) {
        // 取出给定apiLog的所有stepLog
        List<StepLogVO> stepLogs = stepLogFinder.listOrderedStepLogsByApiLogId(apiLog.getId());
        if (stepLogs.size() == 0)
            return null;

        List<String> sidSet = new ArrayList<>();
        for (StepLogVO stepLog: stepLogs)
            sidSet.add(stepLog.getStepId());

        String startStep = stepLogs.get(0).getStepId();
        StepGraph graph = new StepGraph(apiLog.getApiId(), sidSet, startStep,
                apiLogFinder,
                stepLogFinder,
                stepFinder,
                rStepFinder);
        ArrayList<String> sortedSidList = new ArrayList<>();
        ArrayList<BigDecimal> weight = new ArrayList<>();
        for (StepLogVO stepLog: stepLogs) {
            sortedSidList.add(stepLog.getStepId());
            weight.add(new BigDecimal("1.0"));
        }

        weight.remove(weight.size() - 1);
        try {
            graph.setGraphBySidList(sortedSidList, weight);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 找到步骤图中的环并删除
        graph.deleteCircles(startStep);
        return graph;
    }

    /**
     * 删除所有的环
     * @param startStep 从指定步骤开始
     */
    private void deleteCircles(String startStep) {
        BigDecimal[][] graph = this.getGraph();

        if (graph.length == 0)
            return;

        // 这个apiId对应的apiLog计数
        Integer logCount = apiLogFinder.countApiLogGroupByApiId(this.getApiId());
        int startGid = this.getSid2gid().get(startStep);
        ArrayList<ArrayList<Integer>> circles = this.findCircles(startGid);

        Circle circle;
        for (ArrayList<Integer> path: circles) {
            circle = new Circle(path, this.getGid2sid());
            String cid = circle.getId();

            BigDecimal edgeWeight = getCircleWeight(graph, path);
            Integer times = edgeWeight.multiply(new BigDecimal(logCount)).setScale(0, BigDecimal.ROUND_UP).intValue();
            circle.setTimes(times);
            this.getCircles().put(cid, circle);
            this.deleteCircle(graph, path);
        }
    }

    // 更新本次apiLog中包含的需要更新的step
    private void flushStepByApiLog(ApiLogVO apiLog) {
        // 取出该apiLogId的所有stepLog，把要更新的step放到needUpdate里
        List<StepLogVO> stepLogs = stepLogFinder.listOrderedStepLogsByApiLogId(apiLog.getId());

        for (StepLogVO stepLog : stepLogs) {
            BigDecimal stepWait = stepLog.getWait();
            String stepId = stepLog.getStepId();
            if (this.getStepMap().containsKey(stepId)) {
                // 计算meanWait和logCount
                BigDecimal meanWait = this.getStepMap().get(stepId).getMeanWait();
                BigDecimal logCount = new BigDecimal(this.getStepMap().get(stepId).getLogCount());
                BigDecimal newLogCount = logCount.add(new BigDecimal("1"));
                BigDecimal newMeanWait = meanWait.multiply(logCount).add(stepWait).divide(newLogCount, 6, BigDecimal.ROUND_HALF_UP);
                // 更新meanWait和logCount
                this.getStepMap().get(stepId).setMeanWait(newMeanWait);
                this.getStepMap().get(stepId).setLogCount(newLogCount.intValue());
            } else {
                StepVO step = new StepVO();
                step.setStepId(stepLog.getStepId());
                step.setLogCount(1);
                step.setMeanWait(stepWait);
                step.setApiId(apiId);
                step.setName(stepLog.getName());
                this.getStepMap().put(stepId, step);
            }
        }
    }

    private void flushCircle(StepGraph stepGraph) {
        for (Circle c: stepGraph.getCircles().values()) {
            Integer times = 0;
            String cid = c.getId();
            Circle circle = this.getCircles().get(cid);

            // 更新环的总次数
            if (circle == null) {
                this.getCircles().put(cid, c);
                circle = this.getCircles().get(cid);
            } else {
                times = circle.getTimes();
                circle.setTimes(times + c.getTimes());
            }

            // 更新环的平均次数
            BigDecimal newMeanTimes = new BigDecimal(circle.getTimes()).divide(
                    new BigDecimal(this.getApiLogCount()), 6, BigDecimal.ROUND_HALF_UP);
            circle.setMeanTimes(newMeanTimes);

            // 更新环的等待时间
            BigDecimal circleWait = new BigDecimal("0.0");
            for (String stepId: circle.getOrderedSids())
                circleWait = circleWait.add(this.getStepMap().get(stepId).getMeanWait());
            circle.setCircleWait(circleWait);
        }
    }

    /**
     * 找图的主要步骤
     * @return
     */
    public ArrayList<String> findMainPath() {
        BigDecimal[][] graph = this.getGraph();
        if (graph.length == 0)
            return null;

        try {
            int[] maxPath = new int[]{};
            for (int startGid = 0; startGid < graph.length; startGid++) {
                ArrayList<int[]> paths = this.getPathList(startGid);
                if (paths.size() != 1)
                    throw new Exception(String.format("graph have no main path or more than one main path: %s paths.", paths.size()));

                maxPath = paths.get(0).length > maxPath.length ? paths.get(0) : maxPath;
            }
            ArrayList<String> stepPath = new ArrayList<>();
            for (int i: maxPath)
                stepPath.add(this.getGid2sid().get(i));
            return stepPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 更新步骤总图
     */
    public void rebuildGraph() {
        int pageNumber = 1;
        int pageSize = 50;
        List<ApiLogVO> apiLogs = apiLogFinder.listApiLogsByQuery(this.getApiId(),
                ApiLogFinder.NOT_ANALYZED, pageNumber, pageSize);

        this.loadTotalGraph();
        StepGraph stepGraph;
        Graph graph;
        while (apiLogs.size() != 0) {
            for (ApiLogVO apiLog : apiLogs) {
                stepGraph = this.buildGraphBySteps(apiLog);
                if (stepGraph == null)
                    continue;
                // 合并新图graph到旧图totalGraph上
                graph = Graph.unionGraph(this, stepGraph);
                this.setGid2sid(graph.getGid2sid());
                this.setSid2gid(graph.getSid2gid());
                this.setGraph(graph.getGraph());
                this.setApiLogCount(this.getApiLogCount() + 1);
                // 更新step
                this.flushStepByApiLog(apiLog);
                this.flushCircle(stepGraph);
            }

            // 查询下一批次的未分析的apiLogs
            apiLogs = apiLogFinder.listApiLogsByQuery(this.getApiId(), ApiLogFinder.NOT_ANALYZED,
                    ++pageNumber, pageSize);
        }
    }

    /**
     * 保存图到数据库
     */
    public void saveTotalGraph() {
        // 更新或创建step
        for (StepVO step : this.getStepMap().values()) {
            if (null != stepFinder.findOne(step.getStepId()))
                stepFinder.updateByStepId(step.getStepId(), step.getLogCount(), step.getMeanWait());
            else {
                stepFinder.save(step);
            }
        }

        // 删除以前的关系，保存新的关系
        rStepFinder.deleteByApiId(this.getApiId());
        for (String[] edge : this.getEdgeList())
            rStepFinder.save(this.getApiId(), edge[0], edge[1], new BigDecimal("1.0"));

        // 保存环
        for (Circle c: this.getCircles().values()) {
            for (String[] edge: c.getEdges())
                rStepFinder.save(this.getApiId(), edge[0], edge[1], c.getMeanTimes());
        }
    }

    /**
     * 从数据库加载指定的步骤图
     */
    public void loadTotalGraph() {
        // 初始化图
        List<StepVO> allSteps = stepFinder.listAllStepsByApiId(apiId);
        if (allSteps.size() == 0)
            return;

        List<String> sidSet = new ArrayList<>();
        for (StepVO step: allSteps)
            sidSet.add(step.getStepId());
        String startStep = allSteps.get(0).getStepId();
        StepGraph stepGraph = new StepGraph(apiId, sidSet, startStep,
                apiLogFinder,
                stepLogFinder,
                stepFinder,
                rStepFinder);

        // 加载平均等待时间
        for (StepVO step: allSteps)
            stepGraph.getStepMap().put(step.getStepId(), step);

        // 加载边
        List<RStepVO> rSteps = rStepFinder.listAllRStepByApiId(apiId);
        for (RStepVO rStep: rSteps)
            stepGraph.setEdge(rStep.getFromStepId(), rStep.getToStepId(), rStep.getWeight());

        // 复制到当前对象
        this.setGid2sid(stepGraph.getGid2sid());
        this.setSid2gid(stepGraph.getSid2gid());
        this.setGraph(stepGraph.getGraph());
        this.setStepMap(stepGraph.getStepMap());
        this.setStartStep(stepGraph.getStartStep());

        // 删除环
        this.deleteCircles(this.getStartStep());
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public HashMap<String, StepVO> getStepMap() {
        return stepMap;
    }

    public void setStepMap(HashMap<String, StepVO> stepMap) {
        this.stepMap = stepMap;
    }

    public String getStartStep() {
        return startStep;
    }

    public void setStartStep(String startStep) {
        this.startStep = startStep;
    }

    public Map<String, Circle> getCircles() {
        return circles;
    }

    public void setCircles(Map<String, Circle> circles) {
        this.circles = circles;
    }

    public Integer getApiLogCount() {
        return apiLogCount;
    }

    public void setApiLogCount(Integer apiLogCount) {
        this.apiLogCount = apiLogCount;
    }
}
