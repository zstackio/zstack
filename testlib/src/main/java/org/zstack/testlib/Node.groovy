package org.zstack.testlib

import org.zstack.core.Platform
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

/**
 * Created by xing5 on 2017/2/12.
 */
trait Node {
    final CLogger logger = Utils.getLogger(this.getClass())

    Node parent
    List<Object> children = []
    Set<Node> dependencies = []

    void accept(NodeVisitor v) {
        v.visit(this)
    }

    void addChild(Object child) {
        if (child instanceof Node) {
            child.parent = this
            child.dependencies.add(this)
        }

        children.add(child)
    }

    void walkNode(Object n, Closure c) {
        if (n instanceof Node) {
            c(n)

            n.children.each {
                walkNode(it, c)
            }
        } else if (n instanceof ActionNode) {
            n.run()
        } else {
            assert true: "unknown node type ${n.class}"
        }
    }

    void walk(Closure c) {
        walkNode(this, c)
    }

    def find(Node n, String name, Class type) {
        if (type.isAssignableFrom(n.class) && n.name == name) {
            return n
        }

        for (Object c : n.children) {
            if (c instanceof Node) {
                def ret = find(c, name, type)
                if (ret != null) {
                    return ret
                }
            }
        }

        return null
    }

    def find(String name, Class type) {
        return find(this, name, type)
    }

    void resolveDependency(Node n, Set<Node> resolved, List<Node> seen) {
        seen.push(n)
        n.dependencies.each {
            if (!(it instanceof Node)) {
                return
            }

            if (it in seen) {
                seen.push(it)
                def names = seen.collect { sn ->
                    return sn.hasProperty("name") ? sn.name : sn.toString()
                }

                throw new Exception("circle dependencies in environment(): ${names.join(" --> ")}")
            }

            resolveDependency(it, resolved, seen)
        }

        resolved.add(n)
        seen.pop()
    }

    void deploy(String sessionId = null) {
        def sid = sessionId == null ? Test.deployer.envSpec.session?.uuid : sessionId
        assert sid != null : "Not login yet!!! You need either call deploy() with a session uuid or call login() method" +
                " in environment() of the test case"

        def allNodes = []

        walk {
            if (it instanceof CreateAction) {
                it.preOperations.each { it() }
            }

            allNodes.add(it)
        }

        Set<Node> resolvedNodes = new LinkedHashSet<>()
        allNodes.each {
            resolveDependency(it as Node, resolvedNodes, [])
        }

        def names = resolvedNodes.collect { sn ->
            return sn.hasProperty("name") ? sn.name : sn.toString()
        }

        System.out.println("deploying path: ${names.join(" --> ")} ")

        resolvedNodes.each {
            if (!(it instanceof CreateAction)) {
                return
            }

            def uuid = Platform.getUuid()
            Test.deployer.envSpec.specsByUuid[uuid] = it

            def suuid = sid
            if (it instanceof HasSession) {
                if (it.accountName != null) {
                    AccountSpec aspec = Test.deployer.envSpec.find(it.accountName, AccountSpec.class)
                    assert aspec != null: "cannot find the account[$it.accountName] defined in environment()"
                    suuid = aspec.session.uuid
                } else {
                    def n = it.parent
                    while (n != null) {
                        if (!(n instanceof HasSession) || n.accountName == null) {
                            n = n.parent
                        } else {
                            // one of the parent has the accountName set, use it
                            AccountSpec aspec = Test.deployer.envSpec.find(n.accountName, AccountSpec.class)
                            assert aspec != null: "cannot find the account[$n.accountName] defined in environment()"
                            suuid = aspec.session.uuid
                            break
                        }
                    }
                }
            }

            SpecID id = (it as CreateAction).create(uuid, suuid)
            if (id != null) {
                Test.deployer.envSpec.specsByName[id.name] = it
            }
        }

        allNodes.each {
            if (it instanceof CreateAction) {
                it.postOperations.each { it() }
            }
        }
    }
}
