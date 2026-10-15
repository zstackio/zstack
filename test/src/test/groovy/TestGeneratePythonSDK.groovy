import org.junit.Test
import org.zstack.testlib.PythonSdkGenerator

import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/4/9.
 */
class TestGeneratePythonSDK {
    @Test
    void test() {
        new PythonSdkGenerator().generate(System.getProperty("user.home"))
    }

    @Test
    void generatedSdkUsesRepeatedEncodedQueryParameters() {
        File output = File.createTempDir("python-sdk-wire", "test")
        try {
            new PythonSdkGenerator().generate(output.absolutePath)

            String script = '''
import importlib.util
import sys
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs, urlsplit

spec = importlib.util.spec_from_file_location("generated", sys.argv[1])
sdk = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sdk)
seen = []

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        seen.append((parse_qs(urlsplit(self.path).query, keep_blank_values=True),
                     self.headers.get("Authorization")))
        body = b"{}"
        self.send_response(200)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)
    def log_message(self, *args):
        pass

server = HTTPServer(("127.0.0.1", 0), Handler)
threading.Thread(target=server.serve_forever, daemon=True).start()
sdk.configure(port=server.server_port, read_timeout=2)

class Action(sdk.AbstractAction):
    HTTP_METHOD = "GET"
    PATH = "/memory"
    NEED_SESSION = True
    PARAM_NAME = "memory"
    PARAMS = {"ids": sdk.ParamAnnotation(), "text": sdk.ParamAnnotation()}
    def __init__(self):
        super(Action, self).__init__()
        self.ids = None
        self.text = None
        self.sessionId = None

for ids, text in [(["vm 1", "中文&x"], "a b"), ([], "✓")]:
    action = Action()
    action.ids = ids
    action.text = text
    action.sessionId = "must-not-be-in-query"
    action.call()

class Query(sdk.QueryAction):
    HTTP_METHOD = "GET"
    PATH = "/query"
    NEED_SESSION = True
    PARAM_NAME = "query"
    PARAMS = {"conditions": sdk.ParamAnnotation(required=True),
              "fields": sdk.ParamAnnotation(),
              "sortBy": sdk.ParamAnnotation(),
              "sortDirection": sdk.ParamAnnotation()}
    def __init__(self):
        super(Query, self).__init__()
        self.conditions = ["name=中文", "x=a b"]
        self.fields = ["uuid", "name"]
        self.sortBy = "name"
        self.sortDirection = "asc"

query = Query()
query.sessionId = "session-value"
query.call()
server.shutdown()
server.server_close()

assert seen[0][0]["ids"] == ["vm 1", "中文&x"]
assert seen[0][0]["text"] == ["a b"]
assert seen[0][1] == "OAuth must-not-be-in-query"
assert seen[1][0]["text"] == ["✓"]
assert "ids" not in seen[1][0]
assert seen[2][0]["q"] == ["name=中文", "x=a b"]
assert seen[2][0]["fields"] == ["uuid,name"]
assert seen[2][0]["sort"] == ["+name"]
assert seen[2][1] == "OAuth session-value"
for query, _ in seen:
    assert "sessionId" not in query
    assert "accessKeyId" not in query
    assert "accessKeySecret" not in query
'''
            Process process = ["python3", "-c", script, new File(output, "zssdk.py").absolutePath].execute()
            boolean finished = process.waitFor(15, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
            }
            String stdout = process.inputStream.text
            String stderr = process.errorStream.text
            assert finished && process.exitValue() == 0: "generated SDK HTTP query regression failed\nstdout=${stdout}\nstderr=${stderr}"
        } finally {
            output.deleteDir()
        }
    }
}
