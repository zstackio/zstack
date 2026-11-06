'''
Probes of the network probe plugin. Standard library only, and nothing goes through a shell.
'''
import re
import socket
import ssl
import subprocess
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

# an IPv4 address or a host name; a leading '-' would be read by ping as an option
TARGET = re.compile(r'^[A-Za-z0-9][A-Za-z0-9.-]{0,252}$')
PING_LOSS = re.compile(r'([\d.]+)% packet loss')
PING_AVG = re.compile(r'= [\d.]+/([\d.]+)/')
MAX_WORKERS = 16


def validate(spec):
    """The reason the probe cannot run, or None."""
    timeout = spec.get('timeout')
    if not isinstance(timeout, int) or not 1 <= timeout <= 30:
        return 'invalid timeout %s' % timeout
    kind = spec.get('type')
    if kind in ('ICMP', 'TCP'):
        if not TARGET.match(spec.get('target') or ''):
            return 'invalid target %s' % spec.get('target')
        port = spec.get('port')
        if kind == 'TCP' and (not isinstance(port, int) or not 1 <= port <= 65535):
            return 'invalid port %s' % port
        return None
    if kind == 'HTTP':
        url = spec.get('url') or ''
        if not url.startswith(('http://', 'https://')) or any(c.isspace() for c in url):
            return 'invalid url %s' % url
        return None
    return 'unknown type %s' % kind


def _ping(spec):
    timeout = spec['timeout']
    out = subprocess.run(['ping', '-c', '3', '-W', str(timeout), spec['target']], stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT, universal_newlines=True, timeout=timeout * 3 + 2).stdout
    loss = PING_LOSS.search(out)
    avg = PING_AVG.search(out)
    result = {'lossPercent': float(loss.group(1)) if loss else 100.0,
              'latencyMs': float(avg.group(1)) if avg else None}
    result['success'] = result['lossPercent'] < 100
    if not result['success']:
        result['error'] = out.strip().splitlines()[-1] if out.strip() else 'no reply'
    return result


def _tcp(spec):
    start = time.time()
    with socket.create_connection((spec['target'], spec['port']), timeout=spec['timeout']):
        return {'success': True, 'latencyMs': round((time.time() - start) * 1000, 3)}


def _http(spec):
    # demo environments mostly use self-signed certificates
    context = ssl._create_unverified_context()
    start = time.time()
    try:
        with urllib.request.urlopen(urllib.request.Request(spec['url'], method='GET'), timeout=spec['timeout'],
                                    context=context) as resp:
            status = resp.status
    except urllib.error.HTTPError as e:
        status = e.code
    result = {'success': status < 400, 'httpStatus': status, 'latencyMs': round((time.time() - start) * 1000, 3)}
    if status >= 400:
        result['error'] = 'HTTP %d' % status
    return result


PROBES = {'ICMP': _ping, 'TCP': _tcp, 'HTTP': _http}


def run_probe(spec):
    result = {'taskUuid': spec.get('taskUuid'), 'success': False, 'latencyMs': None, 'lossPercent': None,
              'httpStatus': None, 'error': None}
    reason = validate(spec)
    if reason:
        result['error'] = reason
        return result
    try:
        result.update(PROBES[spec['type']](spec))
    except Exception as e:
        result['error'] = str(e) or type(e).__name__
    return result


def run_all(specs):
    """Runs the probes of one call concurrently; the results keep the order of specs."""
    if not specs:
        return []
    with ThreadPoolExecutor(max_workers=min(MAX_WORKERS, len(specs))) as pool:
        return list(pool.map(run_probe, specs))
