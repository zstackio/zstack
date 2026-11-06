'''
kvmagent part of the network probe plugin: runs the probes the management node sends for this host.
'''
import json

from kvmagent import kvmagent
from zstacklib.utils import http
from zstacklib.utils import jsonobject
from . import probes


class NetProbePlugin(kvmagent.KvmAgent):
    PROBE_PATH = '/plugin/net-probe/probe'

    @kvmagent.replyerror
    def probe(self, req):
        cmd = json.loads(req[http.REQUEST_BODY])
        rsp = kvmagent.AgentResponse()
        rsp.results = probes.run_all(cmd.get('probes') or [])
        return jsonobject.dumps(rsp)

    def start(self):
        kvmagent.get_http_server().register_async_uri(self.PROBE_PATH, self.probe)

    def stop(self):
        pass
