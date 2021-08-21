package org.zstack.core.apicost.analyzer.utils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by huaxin on 2021/7/9.
 */
public class Graph {

    private HashMap<String, Integer> sid2gid;
    private HashMap<Integer, String> gid2sid;
    private BigDecimal[][] graph;

    public Graph(List<String> sidList) {
        // 使创建得到的图的id列表保持原来的顺序
        Set<String> sidSet = new HashSet<>(sidList);
        List<String> ids = new ArrayList<>(sidSet);

        this.sid2gid = new HashMap<>();
        this.gid2sid = new HashMap<>();
        this.graph = new BigDecimal[ids.size()][ids.size()];

        for (String id: ids) {
            this.sid2gid.put(id, ids.indexOf(id));
            this.gid2sid.put(ids.indexOf(id), id);
        }
        for (BigDecimal[] ints : this.graph)
            Arrays.fill(ints, new BigDecimal("0.0"));
    }

    /**
     * 用节点字符串id设置边
     */
    public void setEdge(String fromSid, String toSid, BigDecimal weight) {
        this.graph[sid2gid.get(fromSid)][sid2gid.get(toSid)] = weight.add(this.graph[sid2gid.get(fromSid)][sid2gid.get(toSid)]);
    }

    /**
     * 把图g1加到图graph上
     */
    private static void graphAddG1(Graph graph, Graph g1) {
        for (int i = 0; i < g1.getGraph().length; i++)
            for (int j = 0; j < g1.getGraph()[i].length; j++) {
                BigDecimal w1 = g1.getGraph()[i][j];
                graph.setEdge(g1.getGid2sid().get(i), g1.getGid2sid().get(j), w1);
            }
    }

    /**
     * 合并两个图g1,g2
     */
    public static Graph unionGraph(Graph g1, Graph g2) {
        HashSet<String> ids = new HashSet<>(g1.getSid2gid().keySet());
        ids.addAll(g2.getSid2gid().keySet());
        Graph graph = new Graph(new ArrayList<>(ids));
        Graph.graphAddG1(graph, g1);
        Graph.graphAddG1(graph, g2);
        return graph;
    }

    /**
     * 按给定id列表设置边
     */
    public void setGraphBySidList(ArrayList<String> sidList, ArrayList<BigDecimal> weightList) throws Exception {
        if (weightList.size() != sidList.size() - 1)
            throw new Exception(String.format("the weight of %s edges are not clear.", sidList.size() - weightList.size()));
        for (int i = 0; i < sidList.size() - 1; i++)
            this.setEdge(sidList.get(i), sidList.get(i+1), weightList.get(i));
    }

    // 求图上某个结点的子结点
    private ArrayList<Integer> getGraphSubId(int startGid) throws Exception {
        if (this.getGraph().length == 0)
            throw new Exception(String.format("find children of node: %s, while graph is empty.", startGid));

        ArrayList<Integer> subIds = new ArrayList<>();
        for (int i = 0; i < this.getGraph()[startGid].length; i++) {
            if (this.getGraph()[startGid][i].compareTo(new BigDecimal("0.0")) > 0)
                subIds.add(i);
        }
        return subIds;
    }

    // 构造一个带路径的结点
    private JSONObject getNode(Integer curGid, ArrayList<Integer> parentPath) {
        ArrayList<Integer> curPath = new ArrayList<>(parentPath);
        curPath.add(curGid);
        JSONObject node = new JSONObject();
        node.put("id", curGid);
        node.put("path", curPath);
        return node;
    }

    /**
     * 从指定结点，遍历整个图
     * @param startGid < 0 则按 startGid = 0 计算
     * @return 所有路径
     */
    public ArrayList<int[]> getPathList(int startGid) throws Exception {
        startGid = Math.max(startGid, 0);
        JSONObject startNode = this.getNode(startGid, new ArrayList<>());

        Stack<JSONObject> toVisit = new Stack<>();
        toVisit.push(startNode);

        ArrayList<int[]> pathList = new ArrayList<>();
        // 路径里，要把当前起始节点也加上
        ArrayList<Integer> curPath;
        // 深度优先遍历图
        ArrayList<Integer> toAdd;
        while (!toVisit.empty()) {
            JSONObject curNode = toVisit.pop();
            Integer curId = curNode.getInteger("id");
            curPath = curNode.getObject("path", new TypeReference<List<Integer>>(){});

            toAdd = this.getGraphSubId(curId);
            for (Integer t : toAdd) {
                JSONObject nextNode = this.getNode(t, curPath);
                toVisit.push(nextNode);
            }

            // 判断路径是否成环
            if (new HashSet<>(curPath).size() != curPath.size())
                throw new Exception(String.format("Group contains a loop %s.", curPath.toString()));
            // 走到叶子节点，完成一条路径
            if (toAdd.isEmpty()) {
                int[] path = new int[curPath.size()];
                for (int i = 0; i < curPath.size(); i++)
                    path[i] = curPath.get(i);
                pathList.add(path);
            }
        }
        return pathList;
    }

    /**
     * 取出图中的一个环
     * @param startGid 从指定结点开始
     * @return
     */
    public ArrayList<ArrayList<Integer>> findCircles(int startGid) {
        startGid = Math.max(startGid, 0);
        JSONObject startNode = this.getNode(startGid, new ArrayList<>());

        Stack<JSONObject> toVisit = new Stack<>();
        toVisit.push(startNode);

        ArrayList<ArrayList<Integer>> circles = new ArrayList<>();

        // 路径里，要把当前起始节点也加上
        ArrayList<Integer> curPath;
        // 深度优先遍历图
        ArrayList<Integer> toAdd;
        while (!toVisit.empty()) {
            JSONObject curNode = toVisit.pop();
            Integer curId = curNode.getInteger("id");
            curPath = curNode.getObject("path", new TypeReference<ArrayList<Integer>>(){});

            try {
                toAdd = this.getGraphSubId(curId);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            for (Integer t : toAdd) {
                JSONObject nextNode = this.getNode(t, curPath);

                // 成环就不再继续访问这条分支
                ArrayList<Integer> nextPath = nextNode.getObject("path", new TypeReference<ArrayList<Integer>>(){});
                if (new HashSet<>(nextPath).size() != nextPath.size()) {
                    ArrayList<Integer> curCircle = findACircleFromAPath(nextPath);

//                    boolean isNewCircle = true;
//                    for (ArrayList<Integer> c: circles)
//                        if (isSamePath(c, curCircle)) {
//                            isNewCircle = false;
//                            break;
//                        }
//                    if (isNewCircle)
                        circles.add(curCircle);

                    continue;
                }

                toVisit.push(nextNode);
            }
        }
        return circles;
    }

    private static boolean isSamePath(ArrayList<Integer> path1, ArrayList<Integer> path2) {
        if (path1.size() != path2.size())
            return false;

        path1.sort(Comparator.naturalOrder());
        path2.sort(Comparator.naturalOrder());
        for (int i = 0; i < path1.size(); i++)
            if (!path1.get(i).equals(path2.get(i)))
                return false;

        return true;
    }

    // 从path的末尾找到一条circle
    private static ArrayList<Integer> findACircleFromAPath(ArrayList<Integer> nextPath) {
        int endId = nextPath.size() - 1;
        int startId = 0;
        for (int i = 0; i < nextPath.size(); i++) {
            if (nextPath.get(i).equals(nextPath.get(endId))) {
                startId = i;
                break;
            }
        }
        ArrayList<Integer> circle = new ArrayList<>();
        for (int i = startId; i < endId; i++)
            circle.add(nextPath.get(i));
        return circle;
    }

    /**
     * 断裂一条环
     */
    public void breakCircle(List<Integer> loop) {
        this.graph[loop.get(loop.size() - 2)][loop.get(loop.size() - 1)] = new BigDecimal("0.0");
    }

    /**
     * 删除一条环
     */
    public void deleteCircle(BigDecimal[][] graph, List<Integer> loop) {
        BigDecimal minWeight = getCircleWeight(graph, loop);
        int loopSize = loop.size();
        graph[loop.get(loopSize - 1)][loop.get(0)] = graph[loop.get(loopSize - 1)][loop.get(0)].subtract(minWeight);
        for (int i = 0; i < loop.size() - 1; i++)
            graph[loop.get(i)][loop.get(i+1)] = graph[loop.get(i)][loop.get(i+1)].subtract(minWeight);
    }

    // 从环路path中找到权值最小的边
    public static BigDecimal getCircleWeight(BigDecimal[][] graph, List<Integer> path) {
        int circleLength = path.size();
        BigDecimal minWeight = graph[path.get(circleLength - 1)][path.get(0)];
        for (int i = 0; i < circleLength - 1; i++) {
            BigDecimal iWeight = graph[path.get(i)][path.get(i+1)];
            minWeight = minWeight.compareTo(iWeight) > 0 ? iWeight : minWeight;
        }
        return minWeight;
    }

    /**
     * 返回边的列表
     */
    public ArrayList<String[]> getEdgeList() {
        ArrayList<String[]> edges = new ArrayList<>();
        BigDecimal[][] graph = this.getGraph();
        for (int i = 0; i < graph.length; i++) {
            for (int j = 0; j < graph[i].length; j++)
                if (graph[i][j].compareTo(new BigDecimal("0.0")) != 0)
                    edges.add(new String[]{this.getGid2sid().get(i), this.getGid2sid().get(j)});
        }
        return edges;
    }

    public HashMap<String, Integer> getSid2gid() {
        return sid2gid;
    }

    public HashMap<Integer, String> getGid2sid() {
        return gid2sid;
    }

    public BigDecimal[][] getGraph() {
        return graph;
    }

    public void setSid2gid(HashMap<String, Integer> sid2gid) {
        this.sid2gid = sid2gid;
    }

    public void setGid2sid(HashMap<Integer, String> gid2sid) {
        this.gid2sid = gid2sid;
    }

    public void setGraph(BigDecimal[][] graph) {
        this.graph = graph;
    }
}
