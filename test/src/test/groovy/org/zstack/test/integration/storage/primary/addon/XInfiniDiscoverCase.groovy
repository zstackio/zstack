package org.zstack.test.integration.storage.primary.addon

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.zstack.header.storage.addon.primary.AddonInfo
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.xinfini.XInfiniAddonInfo
import org.zstack.xinfini.XInfiniStorageController

import java.util.concurrent.atomic.AtomicReference

class XInfiniDiscoverCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    @Override
    void test() {
        env.create {
            testFirstDiscoveryUsesConfiguredNode()
            testUnavailableNodeFailsThroughCompletion()
            testCompleteFirstDiscovery()
        }
    }

    private void withServer(int status, boolean populated = false, Closure body) {
        List<String> requests = Collections.synchronizedList([])
        HttpServer server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        server.createContext('/', { HttpExchange exchange ->
            String path = exchange.requestURI.path
            requests.add(path)
            Map responseData = status != 200 ? [message: 'node unavailable'] :
                    (populated ? discoveryResponse(exchange) :
                            (path.endsWith('/cluster') ? [uuid: 'discovery-cluster'] : [items: []]))
            String response = JSONObjectUtil.toJsonString(responseData)
            byte[] bytes = response.getBytes('UTF-8')
            exchange.responseHeaders.set('Content-Type', 'application/json')
            exchange.sendResponseHeaders(status, bytes.length)
            exchange.responseBody.withCloseable { it.write(bytes) }
        } as HttpHandler)
        server.start()
        try {
            String config = JSONObjectUtil.toJsonString([
                    token: 'discovery-test-token',
                    nodes: [[ip: '127.0.0.1', port: server.address.port]],
                    pools: []
            ])
            body(config, requests)
        } finally {
            server.stop(0)
        }
    }

    private Map discover(String config) {
        AtomicReference<ErrorCode> error = new AtomicReference<>()
        AtomicReference<AddonInfo> result = new AtomicReference<>()
        new XInfiniStorageController(config).connect(config, null,
                new ReturnValueCompletion<AddonInfo>(null) {
                    @Override
                    void success(AddonInfo info) {
                        result.set(info)
                    }

                    @Override
                    void fail(ErrorCode code) {
                        error.set(code)
                    }
                })
        assert (result.get() == null) != (error.get() == null)
        return [result: result.get(), error: error.get()]
    }

    void testFirstDiscoveryUsesConfiguredNode() {
        withServer(200) { String config, List<String> requests ->
            ErrorCode error = discover(config).error
            assert error != null
            assert error.details.contains('no node found')
            assert requests.size() == 2
            assert requests[0].endsWith('/cluster')
            assert requests[1].endsWith('/nodes')
        }
    }

    void testUnavailableNodeFailsThroughCompletion() {
        withServer(503) { String config, List<String> requests ->
            ErrorCode error = discover(config).error
            assert error != null
            assert error.details.contains('no connected node found')
            assert requests.size() == 1
            assert requests[0].endsWith('/cluster')
        }
    }

    private Map discoveryResponse(HttpExchange exchange) {
        String path = exchange.requestURI.path
        if (path.endsWith('/cluster')) {
            return [uuid: 'discovery-cluster']
        }
        if (path.endsWith('/nodes')) {
            return [items: [[metadata: [state: [state: 'active']],
                             spec: [id: 1, admin_ip: '127.0.0.1', role_afa_admin: true]]]]
        }
        if (path.endsWith('/pools')) {
            return [items: [[metadata: [state: [state: 'active']],
                             spec: [id: 7, name: 'discovery-pool', default_bs_policy_id: 9,
                                    created_at: '2024-05-09T14:56:17+08:00',
                                    updated_at: '2024-05-09T14:56:17+08:00']]]]
        }
        if (path.endsWith('/bs-policies/9')) {
            return [spec: [data_replica_type: 'replica', data_replica_num: 3]]
        }
        assert path.endsWith('/samples/query')
        return [data: [result: [[value: exchange.requestURI.query.contains('actual_kbytes') ? 1000 : 100]]]]
    }

    void testCompleteFirstDiscovery() {
        withServer(200, true) { String config, List<String> requests ->
            Map outcome = discover(config)
            assert outcome.error == null
            XInfiniAddonInfo info = outcome.result as XInfiniAddonInfo
            assert info.nodes.size() == 1
            assert info.nodes[0].ip == '127.0.0.1'
            assert info.nodes[0].status.toString() == 'Connected'
            assert info.pools.size() == 1
            assert info.pools[0].id == 7
            assert info.pools[0].name == 'discovery-pool'
            assert info.pools[0].redundancyPolicy == 'replica'
            assert info.pools[0].replicatedSize == 3
            assert info.pools[0].totalCapacity == 1024000L
            assert info.pools[0].availableCapacity == 768000L
            assert requests.any { it.endsWith('/bs-policies/9') }
            assert requests.count { it.endsWith('/samples/query') } == 2
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
