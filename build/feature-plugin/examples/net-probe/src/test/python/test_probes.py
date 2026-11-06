import http.server
import socket
import threading
import unittest

import probes


def spec(**kw):
    s = {'taskUuid': 't1', 'type': 'TCP', 'target': '127.0.0.1', 'port': 1, 'url': None, 'timeout': 2}
    s.update(kw)
    return s


class _NotFound(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(404)
        self.end_headers()

    def log_message(self, *args):
        pass


class TestValidate(unittest.TestCase):
    def test_rejects_target_with_shell_metacharacters(self):
        self.assertIsNotNone(probes.validate(spec(type='ICMP', target='1.1.1.1;reboot')))
        self.assertIsNotNone(probes.validate(spec(type='TCP', target='$(id)', port=22)))

    def test_rejects_option_like_target(self):
        # ping would take it for an option
        self.assertIsNotNone(probes.validate(spec(type='ICMP', target='-f', port=None)))

    def test_rejects_non_http_url(self):
        self.assertIsNotNone(probes.validate(spec(type='HTTP', target=None, url='file:///etc/passwd')))

    def test_accepts_valid_specs(self):
        self.assertIsNone(probes.validate(spec(type='ICMP', target='172.24.242.7', port=None)))
        self.assertIsNone(probes.validate(spec(type='TCP', target='mn.example.com', port=22)))
        self.assertIsNone(probes.validate(spec(type='HTTP', target=None, url='https://172.24.227.212:5000/')))

    def test_invalid_spec_fails_without_running(self):
        r = probes.run_probe(spec(type='ICMP', target='a b'))
        self.assertFalse(r['success'])
        self.assertIn('target', r['error'])


class TestProbes(unittest.TestCase):
    def test_tcp_open_port_ok(self):
        server = socket.socket()
        server.bind(('127.0.0.1', 0))
        server.listen(1)
        try:
            r = probes.run_probe(spec(port=server.getsockname()[1]))
        finally:
            server.close()
        self.assertTrue(r['success'], r)
        self.assertIsNotNone(r['latencyMs'])

    def test_tcp_closed_port_fails(self):
        s = socket.socket()
        s.bind(('127.0.0.1', 0))
        port = s.getsockname()[1]
        s.close()
        r = probes.run_probe(spec(port=port))
        self.assertFalse(r['success'])
        self.assertTrue(r['error'])

    def test_http_status_recorded(self):
        server = http.server.HTTPServer(('127.0.0.1', 0), _NotFound)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        try:
            r = probes.run_probe(spec(type='HTTP', target=None, port=None,
                                      url='http://127.0.0.1:%d/' % server.server_address[1]))
        finally:
            server.shutdown()
        self.assertFalse(r['success'])
        self.assertEqual(404, r['httpStatus'])

    def test_run_all_keeps_order(self):
        results = probes.run_all([spec(taskUuid='a', target='a b'), spec(taskUuid='b', target='c d')])
        self.assertEqual(['a', 'b'], [r['taskUuid'] for r in results])


if __name__ == '__main__':
    unittest.main()
