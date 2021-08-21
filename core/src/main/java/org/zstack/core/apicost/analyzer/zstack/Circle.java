package org.zstack.core.apicost.analyzer.zstack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

public class Circle {
    String id;
    ArrayList<Integer> orderedGids;
    ArrayList<String> orderedSids;
    ArrayList<String[]> edges;
    Integer times;
    BigDecimal meanTimes;
    BigDecimal circleWait;

    Circle(ArrayList<Integer> orderedGids, Map<Integer, String> gid2Sid) {
        this.id = "";
        this.orderedSids = new ArrayList<>();
        for (Integer i: orderedGids) {
            this.id = String.format("%s_%s", this.id, i);
            this.orderedSids.add(gid2Sid.get(i));
        }
        this.times = 1;
        this.meanTimes = new BigDecimal("1.0");
        initEdges();
    }

    private void initEdges() {
        this.edges = new ArrayList<>();
        if (orderedSids.size() == 0)
            return;

        for (int i = 0; i < orderedSids.size() - 1; i++)
            edges.add(new String[]{orderedSids.get(i), orderedSids.get(i+1)});

        edges.add(new String[]{orderedSids.get(orderedSids.size() - 1), orderedSids.get(0)});
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<Integer> getOrderedGids() {
        return orderedGids;
    }

    public void setOrderedGids(ArrayList<Integer> orderedGids) {
        this.orderedGids = orderedGids;
    }

    public ArrayList<String> getOrderedSids() {
        return orderedSids;
    }

    public void setOrderedSids(ArrayList<String> orderedSids) {
        this.orderedSids = orderedSids;
    }

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public BigDecimal getMeanTimes() {
        return meanTimes;
    }

    public void setMeanTimes(BigDecimal meanTimes) {
        this.meanTimes = meanTimes;
    }

    public BigDecimal getCircleWait() {
        return circleWait;
    }

    public void setCircleWait(BigDecimal circleWait) {
        this.circleWait = circleWait;
    }

    public ArrayList<String[]> getEdges() {
        return edges;
    }

    public void setEdges(ArrayList<String[]> edges) {
        this.edges = edges;
    }
}
