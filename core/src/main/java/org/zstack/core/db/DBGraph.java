package org.zstack.core.db;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vo.EntityGraph;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

public class DBGraph {
    private static CLogger logger = Utils.getLogger(DBGraph.class);

    private static Map<Class, Map<Class, Key>> keys = new HashMap<>();
    private static Map<Class, Node> allNodes = new HashMap<>();

    private static final int PARENT_WEIGHT = 1;
    private static final int FRIEND_WEIGHT = 2;

    private static class Key {
        String src;
        String dst;
        int weight;

        @Override
        public String toString() {
            return String.format("<%s, %s, %s>", src, dst, weight);
        }
    }

    private static class Node {
        Class entityClass;
        Set<Node> neighbours = new HashSet<>();

        @Override
        public String toString() {
            return entityClass.getSimpleName();
        }
    }

    private static List<List<Node>> findPath(Class src, Class dst) {

        class PathFinder {
            List<List<Node>> paths = new ArrayList<>();

            Stack<Node> path = new Stack<>();

            Node node(Class clz) {
                Node n = allNodes.get(clz);
                if (n == null) {
                    throw new CloudRuntimeException(String.format("cannot find node for class[%s]", clz));
                }
                return n;
            }

            private void find(Node n) {
                //logger.debug("-----> " + n.toString());
                if (path.contains(n)) {
                    // cycle
                    return;
                }

                path.push(n);

                for (Node nb : n.neighbours) {
                    if (nb.entityClass == dst) {
                        List<Node> ret = new ArrayList<>(path);
                        ret.add(nb);
                        //logger.debug(String.format("11111111111111111111111111 %s", ret));
                        paths.add(ret);
                    } else {
                        find(nb);
                    }
                }

                path.pop();
            }

            List<List<Node>> find() {
                find(node(src));
                return paths;
            }
        }

        return new PathFinder().find();
    }

    public static void printPath(Class src, Class dst) {
        List<List<Node>> paths = findPath(src, dst);

        class KeyFinder {
            Node leftNode;
            Node rightNode;

            Key pushNode(Node node) {
                if (rightNode == null) {
                    rightNode = node;
                    return null;
                }

                leftNode = rightNode;
                rightNode = node;

                Map<Class, Key> kmap = keys.get(leftNode.entityClass);
                if (kmap == null) {
                    throw new CloudRuntimeException(String.format("cannot for node[%s] -> node[%s]", leftNode.entityClass, rightNode.entityClass));
                }

                Key key = kmap.get(rightNode.entityClass);
                if (key == null) {
                    throw new CloudRuntimeException(String.format("cannot for node[%s] -> node[%s]", leftNode.entityClass, rightNode.entityClass));
                }

                return key;
            }
        }

        class Path {
            int length;
            String path;
        }

        List<Path> allPaths = new ArrayList<>();

        paths.forEach(lst -> {
            KeyFinder keyFinder = new KeyFinder();
            List<String> pstr = new ArrayList<>();

            Path p = new Path();

            lst.forEach(n -> {
                Key key = keyFinder.pushNode(n);
                if (key != null) {
                    pstr.add(key.toString());
                    p.length += key.weight;
                }
                pstr.add(n.toString());
            });

            p.path = StringUtils.join(pstr, " ");

            allPaths.add(p);
        });

        allPaths.sort(Comparator.comparingInt(p -> p.length));

        allPaths.forEach(p -> logger.debug(String.format("yyyyyyyyyyyyyyyyyy %s: %s", p.length, p.path)));
    }

    @StaticInit
    static void staticInit() {
        class NodeResolver {
            EntityGraph entityGraph;
            Node me;

            public NodeResolver(Class clz) {
                if (allNodes.containsKey(clz)) {
                    me = allNodes.get(clz);
                } else {
                    me = new Node();
                    me.entityClass = clz;
                    allNodes.put(clz, me);

                    entityGraph = (EntityGraph) clz.getAnnotation(EntityGraph.class);

                    if (entityGraph == null) {
                        throw new CloudRuntimeException(String.format("missing @EntityGraph for class[%s] referred by other" +
                                " entities having @EntityGraph", clz));
                    }

                    resolveNeighbours();
                }
            }

            private void resolveNeighbours() {
                for (EntityGraph.Neighbour at : entityGraph.parents()) {
                    me.neighbours.add(new NodeResolver(at.type()).resolve());
                    buildKey(at, at.weight() == -1 ? PARENT_WEIGHT : at.weight());
                }

                for (EntityGraph.Neighbour fat : entityGraph.friends()) {
                    me.neighbours.add(new NodeResolver(fat.type()).resolve());
                    buildKey(fat, fat.weight() == -1 ? FRIEND_WEIGHT : fat.weight());
                }
            }

            private void buildKey(EntityGraph.Neighbour at, int w) {
                Map<Class, Key> second = keys.computeIfAbsent(me.entityClass, x->new HashMap<>());
                Key key = new Key();
                key.src = at.myField();
                key.dst = at.targetField();
                key.weight = w;
                second.put(at.type(), key);
            }

            Node resolve() {
                return me;
            }
        }

        BeanUtils.reflections.getTypesAnnotatedWith(EntityGraph.class).stream()
                .filter(c -> c.isAnnotationPresent(EntityGraph.class))
                .forEach(clz -> new NodeResolver(clz).resolve());
    }
}
