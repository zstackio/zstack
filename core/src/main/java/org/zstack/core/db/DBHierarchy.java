package org.zstack.core.db;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.hierarchy.EntityHierarchy;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

public class DBHierarchy {
    private static CLogger logger = Utils.getLogger(DBHierarchy.class);

    private static Map<Class, Map<Class, Key>> keys = new HashMap<>();
    private static Map<Class, Node> allNodes = new HashMap<>();

    private static class Key {
        String src;
        String dst;

        @Override
        public String toString() {
            return String.format("<%s, %s>", src, dst);
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

        paths.forEach(lst -> {
            KeyFinder keyFinder = new KeyFinder();
            List<String> pstr = new ArrayList<>();

            lst.forEach(n -> {
                Key key = keyFinder.pushNode(n);
                if (key != null) {
                    pstr.add(key.toString());
                }
                pstr.add(n.toString());
            });

            logger.debug("yyyyyyyyyyyyyyyyyyyyyyyyyyy " + StringUtils.join(pstr, " "));
        });
    }

    @StaticInit
    static void staticInit() {
        class NodeResolver {
            EntityHierarchy entityHierarchy;
            Node me;

            public NodeResolver(Class clz) {
                if (allNodes.containsKey(clz)) {
                    me = allNodes.get(clz);
                } else {
                    me = new Node();
                    me.entityClass = clz;
                    allNodes.put(clz, me);

                    entityHierarchy = (EntityHierarchy) clz.getAnnotation(EntityHierarchy.class);

                    if (entityHierarchy == null) {
                        throw new CloudRuntimeException(String.format("missing @EntityHierarchy for class[%s] referred by other" +
                                " entities having @EntityHierarchy", clz));
                    }

                    resolveNeighbours();
                }
            }

            private void resolveNeighbours() {
                if (entityHierarchy.parent() != Object.class) {
                    // non-root node
                    me.neighbours.add(new NodeResolver(entityHierarchy.parent()).resolve());
                    buildKey(me.entityClass, entityHierarchy.parent(), entityHierarchy.myField(), entityHierarchy.targetField());
                }

                for (EntityHierarchy.Friend fat : entityHierarchy.friends()) {
                    me.neighbours.add(new NodeResolver(fat.type()).resolve());
                    buildKey(me.entityClass, fat.type(), fat.myField(), fat.targetField());
                }
            }

            private void buildKey(Class src, Class dst, String srcf, String dstf) {
                Map<Class, Key> second = keys.computeIfAbsent(src, x->new HashMap<>());
                Key key = new Key();
                key.src = srcf;
                key.dst = dstf;
                second.put(dst, key);
            }

            Node resolve() {
                return me;
            }
        }

        BeanUtils.reflections.getTypesAnnotatedWith(EntityHierarchy.class).stream()
                .filter(c -> c.isAnnotationPresent(EntityHierarchy.class))
                .forEach(clz -> new NodeResolver(clz).resolve());
    }
}
