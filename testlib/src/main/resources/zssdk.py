import re
import sys

try:
    import urllib3
except ImportError:
    print 'urlib3 is not installed, run "pip install urlib3"'
    sys.exit(1)

import string
import json
from uuid import uuid4
import time
import threading
import functools
import traceback

CONFIG_HOSTNAME = 'hostname'
CONFIG_PORT = 'port'
CONFIG_POLLING_TIMEOUT = 'default_polling_timeout'
CONFIG_POLLING_INTERVAL = 'default_polling_interval'
CONFIG_WEBHOOK = 'webhook'
CONFIG_READ_TIMEOUT = 'read_timeout'
CONFIG_WRITE_TIMEOUT = 'write_timeout'
CONFIG_CONTEXT_PATH = 'context_path'

HEADER_JOB_UUID = "X-Job-UUID"
HEADER_WEBHOOK = "X-Web-Hook"
HEADER_JOB_SUCCESS = "X-Job-Success"
HEADER_AUTHORIZATION = "Authorization"
OAUTH = "OAuth"
LOCATION = "location"

HTTP_ERROR = "sdk.1000"
POLLING_TIMEOUT_ERROR = "sdk.1001"
INTERNAL_ERROR = "sdk.1002"

__config__ = {}


class SdkError(Exception):
    pass


def _exception_safe(func):
    @functools.wraps(func)
    def wrap(*args, **kwargs):
        try:
            func(*args, **kwargs)
        except:
            print traceback.format_exc()

    return wrap


def _error_if_not_configured():
    if not __config__:
        raise SdkError('call configure() before using any APIs')


def _http_error(status, body=None):
    err = ErrorCode()
    err.code = HTTP_ERROR
    err.description = 'the http status code[%s] indicates a failure happened' % status
    err.details = body
    return {'error': err}


def _error(code, desc, details):
    err = ErrorCode()
    err.code = code
    err.desc = desc
    err.details = details
    return {'error': err}


def configure(
        hostname='127.0.0.1',
        context_path = None,
        port=8080,
        polling_timeout=3600*3,
        polling_interval=1,
        read_timeout=15,
        write_timeout=15,
        web_hook=None
):
    __config__[CONFIG_HOSTNAME] = hostname
    __config__[CONFIG_PORT] = port
    __config__[CONFIG_POLLING_TIMEOUT] = polling_timeout
    __config__[CONFIG_POLLING_INTERVAL] = polling_interval
    __config__[CONFIG_WEBHOOK] = web_hook
    __config__[CONFIG_READ_TIMEOUT] = read_timeout
    __config__[CONFIG_WRITE_TIMEOUT] = write_timeout
    __config__[CONFIG_CONTEXT_PATH] = context_path


class ParamAnnotation(object):
    def __init__(
            self,
            required=False,
            valid_values=None,
            valid_regex_values=None,
            max_length=None,
            min_length=None,
            non_empty=None,
            null_elements=None,
            empty_string=None,
            number_range=None,
            no_trim=False
    ):
        self.required = required
        self.valid_values = valid_values
        self.valid_regex_values = valid_regex_values
        self.max_length = max_length
        self.min_length = min_length
        self.non_empty = non_empty
        self.null_elements = null_elements
        self.empty_string = empty_string
        self.number_range = number_range
        self.no_trim = no_trim


class ErrorCode(object):
    def __init__(self):
        self.code = None
        self.description = None
        self.details = None
        self.cause = None


class Obj(object):
    def __init__(self, d):
        for a, b in d.items():
            if isinstance(b, (list, tuple)):
                setattr(self, a, [Obj(x) if isinstance(x, dict) else x for x in b])
            else:
                setattr(self, a, Obj(b) if isinstance(b, dict) else b)

    def __getattr__(self, item):
        return None


class AbstractAction(object):
    def __init__(self):
        self.apiId = None
        self.sessionId = None
        self.systemTags = None
        self.userTags = None
        self.timeout = None
        self.pollingInterval = None

        self._param_descriptors = {
            'systemTags': ParamAnnotation(),
            'userTags': ParamAnnotation()
        }

        self._param_descriptors.update(self.PARAMS)

    def _check_params(self):
        for param_name, annotation in self._param_descriptors.items():
            value = getattr(self, param_name, None)

            if value is None and annotation.required:
                raise SdkError('missing a mandatory parameter[%s]' % param_name)

            if value is not None and annotation.valid_values and value not in annotation.valid_values:
                raise SdkError('invalid parameter[%s], the value[%s] is not in the valid options%s' % (param_name, value, annotation.valid_values))

            if value is not None and isinstance(value, str) and annotation.max_length and len(value) > annotation.max_length:
                raise SdkError('invalid length[%s] of the parameter[%s], the max allowed length is %s' % (len(value), param_name, annotation.max_length))

            if value is not None and isinstance(value, str) and annotation.min_length and len(value) > annotation.min_length:
                raise SdkError('invalid length[%s] of the parameter[%s], the minimal allowed length is %s' % (len(value), param_name, annotation.min_length))

            if value is not None and isinstance(value, list) and annotation.non_empty is True and len(value) == 0:
                raise SdkError('invalid parameter[%s], it cannot be an empty list' % param_name)

            if value is not None and isinstance(value, list) and annotation.null_elements is True and None in value:
                raise SdkError('invalid parameter[%s], the list cannot contain a null element' % param_name)

            if value is not None and isinstance(value, str) and annotation.empty_string is False and len(value) == 0:
                raise SdkError('invalid parameter[%s], it cannot be an empty string' % param_name)

            if value is not None and (isinstance(value, int) or isinstance(value, long)) \
                    and annotation.number_range is not None and len(annotation.number_range) == 2:
                low = annotation.number_range[0]
                high = annotation.number_range[1]
                if value < low or value > high:
                    raise SdkError('invalid parameter[%s], its value is not in the valid range' % annotation.number_range)

            if value is not None and isinstance(value, str) and annotation.no_trim is False:
                value = str(value).strip()
                setattr(self, param_name, value)

    def _params(self):
        ret = {}
        for k, _ in self._param_descriptors.items():
            val = getattr(self, k, None)
            if val is not None:
                ret[k] = val

        return ret

    def _query_string(self, params):
        return '&'.join(['%s=%s' % (k, v) for k, v in params.items()])

    def _url(self):
        elements = ['http://', __config__[CONFIG_HOSTNAME], ':', str(__config__[CONFIG_PORT])]
        context_path = __config__.get(CONFIG_CONTEXT_PATH, None)
        if context_path is not None:
            elements.append(context_path)
        elements.append('/v1')

        path = self.PATH.replace('{', '${')
        unresolved = re.findall('${(.+?)}', path)
        params = self._params()
        if unresolved:
            for u in unresolved:
                if u in params:
                    raise SdkError('missing a mandatory parameter[%s]' % u)

        path = string.Template(path).substitute(params)
        elements.append(path)

        if self.HTTP_METHOD == 'GET' or self.HTTP_METHOD == 'DELETE':
            elements.append('?')
            elements.append(self._query_string(params))

        return ''.join(elements), unresolved

    def call(self, cb=None):

        def _return(result):
            if cb:
                cb(result)
            else:
                return result

        _error_if_not_configured()

        self._check_params()
        url, params_in_url = self._url()

        headers = {}
        if self.apiId is not None:
            headers[HEADER_JOB_UUID] = self.apiId
        else:
            headers[HEADER_JOB_UUID] = _uuid()

        if self.NEED_SESSION:
            headers[HEADER_AUTHORIZATION] = "%s %s" % (OAUTH, self.sessionId)

        web_hook = __config__.get(CONFIG_WEBHOOK, None)
        if web_hook is not None:
            headers[CONFIG_WEBHOOK] = web_hook

        params = self._params()
        body = None
        if self.HTTP_METHOD == 'POST' or self.HTTP_METHOD == 'PUT':
            m = {}
            for k, v in params.items():
                if v is None:
                    continue

                if k == 'sessionId':
                    continue

                if k in params_in_url:
                    continue

                m[k] = v

            body = {self.PARAM_NAME: m}

        if not self.timeout:
            self.timeout = __config__[CONFIG_READ_TIMEOUT]

        rsp = _json_http(uri=url, body=body, headers=headers, method=self.HTTP_METHOD, timeout=self.timeout)

        if rsp.status < 200 or rsp.status >= 300:
            return _return(Obj(_http_error(rsp.status, rsp.data)))
        elif rsp.status == 200 or rsp.status == 204:
            # the API completes
            return _return(Obj(self._write_result(rsp)))
        elif rsp.status == 202:
            # the API needs polling
            return self._poll_result(rsp, cb)
        else:
            raise SdkError('[Internal Error] the server returns an unknown status code[%s], body[%s]' % (rsp.status, rsp.data))

    def _write_result(self, rsp):
        data = rsp.data
        if not data:
            data = '{}'

        if rsp.status == 200:
            return {"value": json.loads(data)}
        elif rsp.status == 503:
            return json.loads(data)
        else:
            raise SdkError('unknown status code[%s]' % rsp.status)

    def _poll_result(self, rsp, cb):
        if not self.NEED_POLL:
            raise SdkError('[Internal Error] the api is not an async API but the server returns 202 status code')

        m = json.loads(rsp.data)
        location = m[LOCATION]
        if not location:
            raise SdkError("Internal Error] the api[%s] is an async API but the server doesn't return the polling location url")

        if cb:
            # async polling
            self._async_poll(location, cb)
        else:
            # sync polling
            return self._sync_polling(location)

    def _fill_timeout_parameters(self):
        if self.timeout is None:
            self.timeout = __config__.get(CONFIG_POLLING_TIMEOUT)

        if self.pollingInterval is None:
            self.pollingInterval = __config__.get(CONFIG_POLLING_INTERVAL)

    def _async_poll(self, location, cb):
        @_exception_safe
        def _polling():
            ret = self._sync_polling(location)
            cb(ret)

        threading.Thread(target=_polling).start()

    def _sync_polling(self, location):
        count = 0
        self._fill_timeout_parameters()

        while count < self.timeout:
            rsp = _json_http(
                uri=location,
                headers={HEADER_AUTHORIZATION: "%s %s" % (OAUTH, self.sessionId)},
                method='GET'
            )

            if rsp.status not in [200, 503, 202]:
                return Obj(_http_error(rsp.status, rsp.data))
            elif rsp.status in [200, 503]:
                return Obj(self._write_result(rsp))

            time.sleep(self.pollingInterval)
            count += self.pollingInterval

        return Obj(_error(POLLING_TIMEOUT_ERROR, 'polling an API result time out',
                          'failed to poll the result after %s seconds' % self.timeout))


class QueryAction(AbstractAction):
    PARAMS = {
        'conditions': ParamAnnotation(required=True),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(valid_values=['asc', 'desc']),
        'fields': ParamAnnotation(),
    }

    def __init__(self):
        super(QueryAction, self).__init__()
        self.conditions = []
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.sessionId = None

    def _query_string(self, params):
        m = []

        ps = {}
        for k, v in params.items():
            if k in self.PARAMS:
                ps[k] = v

        for k, v in ps.items():
            if v is None:
                continue

            if k == 'sortBy' and v is not None:
                if self.sortDirection is None:
                    m.append('sort=%s' % v)
                else:
                    op = '+' if self.sortDirection == 'asc' else '-'
                    m.append('sort=%s%s' % (op, v))
            elif k == 'sortDirection':
                continue
            elif k == 'fields':
                m.append('fields=%s' % ','.join(v))
            elif k == 'conditions':
                m.extend(['q=%s' % q for q in v])
            else:
                m.append('%s=%s' % (k, v))

        return '&'.join(m)


def _uuid():
    return str(uuid4()).replace('-', '')


def _json_http(
        uri,
        body=None,
        headers={},
        method='POST',
        timeout=120.0
):
    pool = urllib3.PoolManager(timeout=timeout, retries=urllib3.util.retry.Retry(15))
    headers.update({'Content-Type': 'application/json', 'Connection': 'close'})

    if body is not None and not isinstance(body, str):
        body = json.dumps(body).encode('utf-8')

    print '[Request]: %s url=%s, headers=%s, body=%s' % (method, uri, headers, body)
    if body:
        headers['Content-Length'] = len(body)
        rsp = pool.request(method, uri, body=body, headers=headers)
    else:
        rsp = pool.request(method, uri, headers=headers)

    print '[Response to %s %s]: status: %s, body: %s' % (method, uri, rsp.status, rsp.data)
    return rsp



class ChangeZoneStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/zones/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeZoneState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeZoneStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmQgaAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{uuid}/qga'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmQgaAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateWebhookAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/web-hooks/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateWebhook'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'opaque': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateWebhookAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.url = None
        self.type = None
        self.opaque = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVolumeQosAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/volumes/{uuid}/qos'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVolumeQosAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ReconnectHostAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hosts/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'reconnectHost'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ReconnectHostAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateClusterAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/clusters/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateCluster'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateClusterAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class PauseVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'pauseVmInstance'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(PauseVmInstanceAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class PrometheusQueryMetadataAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/prometheus/meta-data'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'matches': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(PrometheusQueryMetadataAction, self).__init__()
        self.matches = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachBackupStorageFromZoneAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/zones/{zoneUuid}/backup-storage/{backupStorageUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'backupStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachBackupStorageFromZoneAction, self).__init__()
        self.backupStorageUuid = None
        self.zoneUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVmNicAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/nics'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVmNicAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteRouteEntryRemoteAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/route-entry/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=True,valid_values=['vbr','vrouter'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteRouteEntryRemoteAction, self).__init__()
        self.uuid = None
        self.type = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteEipAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/eips/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteEipAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryL2VxlanNetworkPoolAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/l2-networks/vxlan-pool'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryL2VxlanNetworkPoolAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeletePolicyAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/accounts/policies/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeletePolicyAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class LogOutAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/accounts/sessions/{sessionUuid}'
    NEED_SESSION = False
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'sessionUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation()
    }

    def __init__(self):
        super(LogOutAction, self).__init__()
        self.sessionUuid = None
        self.systemTags = None
        self.userTags = None


class QueryLoadBalancerAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/load-balancers'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryLoadBalancerAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachL3NetworkToVmAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/vm-instances/{vmInstanceUuid}/l3-networks/{l3NetworkUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'staticIp': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachL3NetworkToVmAction, self).__init__()
        self.vmInstanceUuid = None
        self.l3NetworkUuid = None
        self.staticIp = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteEcsVSwitchRemoteAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/vswitch/remote/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteEcsVSwitchRemoteAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryEipAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/eips'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryEipAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryPolicyAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/accounts/policies'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryPolicyAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachIsoToVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/vm-instances/{vmInstanceUuid}/iso/{isoUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'isoUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachIsoToVmInstanceAction, self).__init__()
        self.vmInstanceUuid = None
        self.isoUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachDataVolumeFromVmAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/volumes/{uuid}/vm-instances'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vmUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachDataVolumeFromVmAction, self).__init__()
        self.uuid = None
        self.vmUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVolumeFormatAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/volumes/formats'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVolumeFormatAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteCephPrimaryStoragePoolAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/primary-storage/ceph/pools/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteCephPrimaryStoragePoolAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachSecurityGroupToL3NetworkAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/security-groups/{securityGroupUuid}/l3-networks/{l3NetworkUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'securityGroupUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachSecurityGroupToL3NetworkAction, self).__init__()
        self.securityGroupUuid = None
        self.l3NetworkUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateVirtualBorderRouterRemoteAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hybrid/aliyun/border-router/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateVirtualBorderRouterRemote'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'localGatewayIp': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'peerGatewayIp': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'peeringSubnetMask': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=64,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=128,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vlanId': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'circuitCode': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateVirtualBorderRouterRemoteAction, self).__init__()
        self.uuid = None
        self.localGatewayIp = None
        self.peerGatewayIp = None
        self.peeringSubnetMask = None
        self.name = None
        self.description = None
        self.vlanId = None
        self.circuitCode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteLoadBalancerAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/load-balancers/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteLoadBalancerAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateQuotaAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/accounts/quotas/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateQuota'

    PARAMS = {
        'identityUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'value': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateQuotaAction, self).__init__()
        self.identityUuid = None
        self.name = None
        self.value = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QuerySchedulerAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/schedulers'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QuerySchedulerAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ExpungeImageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/images/{imageUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'expungeImage'

    PARAMS = {
        'imageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'backupStorageUuids': ParamAnnotation(required=False,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ExpungeImageAction, self).__init__()
        self.imageUuid = None
        self.backupStorageUuids = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryCephPrimaryStorageAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/primary-storage/ceph'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryCephPrimaryStorageAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RebootEcsInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hybrid/aliyun/ecs/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'rebootEcsInstance'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RebootEcsInstanceAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteUserGroupAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/accounts/groups/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteUserGroupAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetResourceNamesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/resources/names'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'uuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetResourceNamesAction, self).__init__()
        self.uuids = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateWebhookAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/web-hooks'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=True,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'opaque': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateWebhookAction, self).__init__()
        self.name = None
        self.description = None
        self.url = None
        self.type = None
        self.opaque = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateL2VxlanNetworkPoolAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/l2-networks/vxlan-pool'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'physicalInterface': ParamAnnotation(required=False,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateL2VxlanNetworkPoolAction, self).__init__()
        self.name = None
        self.description = None
        self.zoneUuid = None
        self.physicalInterface = None
        self.type = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateSchedulerAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/schedulers/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateScheduler'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'schedulerName': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'schedulerDescription': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateSchedulerAction, self).__init__()
        self.uuid = None
        self.schedulerName = None
        self.schedulerDescription = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetCandidateIsoForAttachingVmAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{vmInstanceUuid}/iso-candidates'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetCandidateIsoForAttachingVmAction, self).__init__()
        self.vmInstanceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteEcsVSwitchInLocalAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/vswitch/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteEcsVSwitchInLocalAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateSftpBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/backup-storage/sftp/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateSftpBackupStorage'

    PARAMS = {
        'username': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'hostname': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateSftpBackupStorageAction, self).__init__()
        self.username = None
        self.password = None
        self.hostname = None
        self.sshPort = None
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachPortForwardingRuleAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/port-forwarding/{uuid}/vm-instances/nics'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachPortForwardingRuleAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncEcsSecurityGroupFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/security-group/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncEcsSecurityGroupFromRemote'

    PARAMS = {
        'ecsVpcUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ecsSecurityGroupId': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncEcsSecurityGroupFromRemoteAction, self).__init__()
        self.ecsVpcUuid = None
        self.ecsSecurityGroupId = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmConsolePasswordAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{uuid}/console-passwords'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmConsolePasswordAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ExpungeVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'expungeVmInstance'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ExpungeVmInstanceAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteIPsecConnectionAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/ipsec/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteIPsecConnectionAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteNicQosAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/vm-instances/{uuid}/nic-qos'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'direction': ParamAnnotation(required=True,valid_values=['in','out'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteNicQosAction, self).__init__()
        self.uuid = None
        self.direction = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class PrometheusQueryLabelValuesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/prometheus/labels'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'labels': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(PrometheusQueryLabelValuesAction, self).__init__()
        self.labels = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteNotificationsAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/notifications'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteNotificationsAction, self).__init__()
        self.uuids = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeVipStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vips/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeVipState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeVipStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteAliyunKeySecretAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/key/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteAliyunKeySecretAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetEcsInstanceVncUrlAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/ecs-vnc/{uuid}'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetEcsInstanceVncUrlAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class LogInByUserAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/accounts/users/login'
    NEED_SESSION = False
    NEED_POLL = False
    PARAM_NAME = 'logInByUser'

    PARAMS = {
        'accountUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'accountName': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'userName': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation()
    }

    def __init__(self):
        super(LogInByUserAction, self).__init__()
        self.accountUuid = None
        self.accountName = None
        self.userName = None
        self.password = None
        self.systemTags = None
        self.userTags = None


class CreateL2NoVlanNetworkAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/l2-networks/no-vlan'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'physicalInterface': ParamAnnotation(required=True,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateL2NoVlanNetworkAction, self).__init__()
        self.name = None
        self.description = None
        self.zoneUuid = None
        self.physicalInterface = None
        self.type = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteEcsVpcInLocalAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/vpc/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteEcsVpcInLocalAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryApplianceVmAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/appliances'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryApplianceVmAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryConnectionAccessPointFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/access-point'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryConnectionAccessPointFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateNotificationsStatusAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/notifications/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateNotificationsStatus'

    PARAMS = {
        'uuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'status': ParamAnnotation(required=True,valid_values=['Unread','Read'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateNotificationsStatusAction, self).__init__()
        self.uuids = None
        self.status = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteEcsSecurityGroupRuleRemoteAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/security-group-rule/remote/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteEcsSecurityGroupRuleRemoteAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncEcsVSwitchFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/vswitch/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncEcsVSwitchFromRemote'

    PARAMS = {
        'identityZoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vSwitchId': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncEcsVSwitchFromRemoteAction, self).__init__()
        self.identityZoneUuid = None
        self.vSwitchId = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryAccountAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/accounts'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryAccountAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVmInstanceAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVmInstanceAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetBackupStorageTypesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/backup-storage/types'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetBackupStorageTypesAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncEcsInstanceFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/ecs/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncEcsInstanceFromRemote'

    PARAMS = {
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncEcsInstanceFromRemoteAction, self).__init__()
        self.dataCenterUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SetVmStaticIpAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{vmInstanceUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'setVmStaticIp'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ip': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SetVmStaticIpAction, self).__init__()
        self.vmInstanceUuid = None
        self.l3NetworkUuid = None
        self.ip = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateImageStoreBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/backup-storage/image-store/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateImageStoreBackupStorage'

    PARAMS = {
        'username': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'hostname': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateImageStoreBackupStorageAction, self).__init__()
        self.username = None
        self.password = None
        self.hostname = None
        self.sshPort = None
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateIpRangeAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/l3-networks/ip-ranges/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateIpRange'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateIpRangeAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateRouteEntryForConnectionRemoteAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/route-entry'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vRouterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'destinationCidrBlock': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vRouterInterfaceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vRouterType': ParamAnnotation(required=True,valid_values=['vbr','vrouter'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateRouteEntryForConnectionRemoteAction, self).__init__()
        self.vRouterUuid = None
        self.destinationCidrBlock = None
        self.vRouterInterfaceUuid = None
        self.vRouterType = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVolumeAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/volumes'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVolumeAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateResourcePriceAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/billings/prices'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'resourceName': ParamAnnotation(required=True,valid_values=['cpu','memory','rootVolume','dataVolume'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUnit': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'timeUnit': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'price': ParamAnnotation(required=True,number_range=[0, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'accountUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dateInLong': ParamAnnotation(required=False,number_range=[0, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateResourcePriceAction, self).__init__()
        self.resourceName = None
        self.resourceUnit = None
        self.timeUnit = None
        self.price = None
        self.accountUuid = None
        self.dateInLong = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryLocalStorageResourceRefAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/primary-storage/local-storage/resource-refs'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryLocalStorageResourceRefAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeL3NetworkStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/l3-networks/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeL3NetworkState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeL3NetworkStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddIpRangeAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/l3-networks/{l3NetworkUuid}/ip-ranges'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'startIp': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'endIp': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'netmask': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'gateway': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddIpRangeAction, self).__init__()
        self.l3NetworkUuid = None
        self.name = None
        self.description = None
        self.startIp = None
        self.endIp = None
        self.netmask = None
        self.gateway = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteSecurityGroupRuleAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/security-groups/rules'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'ruleUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteSecurityGroupRuleAction, self).__init__()
        self.ruleUuids = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/backup-storage/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateBackupStorage'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateBackupStorageAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteEcsImageRemoteAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/image/remote/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteEcsImageRemoteAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateEcsImageFromLocalImageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/image'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'imageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'backupStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateEcsImageFromLocalImageAction, self).__init__()
        self.imageUuid = None
        self.dataCenterUuid = None
        self.backupStorageUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryCephBackupStorageAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/backup-storage/ceph'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryCephBackupStorageAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteLdapBindingAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/ldap/bindings/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteLdapBindingAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddCephPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/primary-storage/ceph'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'monUrls': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'rootVolumePoolName': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dataVolumePoolName': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'imageCachePoolName': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddCephPrimaryStorageAction, self).__init__()
        self.monUrls = None
        self.rootVolumePoolName = None
        self.dataVolumePoolName = None
        self.imageCachePoolName = None
        self.url = None
        self.name = None
        self.description = None
        self.type = None
        self.zoneUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddSharedMountPointPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/primary-storage/smp'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'url': ParamAnnotation(required=True,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddSharedMountPointPrimaryStorageAction, self).__init__()
        self.url = None
        self.name = None
        self.description = None
        self.type = None
        self.zoneUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetLicenseCapabilitiesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/licenses/capabilities'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetLicenseCapabilitiesAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RevertVolumeFromSnapshotAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/volume-snapshots/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'revertVolumeFromSnapshot'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RevertVolumeFromSnapshotAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateUserAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/accounts/users'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateUserAction, self).__init__()
        self.name = None
        self.password = None
        self.description = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class TerminateVirtualBorderRouterRemoteAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hybrid/aliyun/border-router/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'terminateVirtualBorderRouterRemote'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(TerminateVirtualBorderRouterRemoteAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeResourceOwnerAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/account/{accountUuid}/resources'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'accountUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeResourceOwnerAction, self).__init__()
        self.accountUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddAliyunKeySecretAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/key'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'key': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'secret': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'accountUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddAliyunKeySecretAction, self).__init__()
        self.name = None
        self.key = None
        self.secret = None
        self.accountUuid = None
        self.description = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RecoverImageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/images/{imageUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'recoverImage'

    PARAMS = {
        'imageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'backupStorageUuids': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RecoverImageAction, self).__init__()
        self.imageUuid = None
        self.backupStorageUuids = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteL3NetworkAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/l3-networks/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteL3NetworkAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateClusterAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/clusters'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'hypervisorType': ParamAnnotation(required=True,valid_values=['KVM','Simulator'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=False,valid_values=['zstack'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateClusterAction, self).__init__()
        self.zoneUuid = None
        self.name = None
        self.description = None
        self.hypervisorType = None
        self.type = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncEcsImageFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/image/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncEcsImageFromRemote'

    PARAMS = {
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncEcsImageFromRemoteAction, self).__init__()
        self.dataCenterUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RemoveMonFromFusionstorPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/primary-storage/fusionstor/{uuid}/mons'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monHostnames': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RemoveMonFromFusionstorPrimaryStorageAction, self).__init__()
        self.uuid = None
        self.monHostnames = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateDataVolumeAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/volumes/data'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'diskOfferingUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'primaryStorageUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateDataVolumeAction, self).__init__()
        self.name = None
        self.description = None
        self.diskOfferingUuid = None
        self.primaryStorageUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteInstanceOfferingAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/instance-offerings/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteInstanceOfferingAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateRebootVmInstanceSchedulerAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/vm-instances/{vmUuid}/schedulers/rebooting'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vmUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'schedulerName': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'schedulerDescription': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=True,valid_values=['simple','cron'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'interval': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'repeatCount': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'startTime': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'cron': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateRebootVmInstanceSchedulerAction, self).__init__()
        self.vmUuid = None
        self.schedulerName = None
        self.schedulerDescription = None
        self.type = None
        self.interval = None
        self.repeatCount = None
        self.startTime = None
        self.cron = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryUserTagAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/user-tags'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryUserTagAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVolumeCapabilitiesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/volumes/{uuid}/capabilities'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVolumeCapabilitiesAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryUserAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/accounts/users'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryUserAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryL2VxlanNetworkAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/l2-networks/vxlan'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryL2VxlanNetworkAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryEcsImageFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/image'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryEcsImageFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class PowerResetBaremetalHostAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/baremetal/chessis/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'powerResetBaremetalHost'

    PARAMS = {
        'chessisUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(PowerResetBaremetalHostAction, self).__init__()
        self.chessisUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateAccountAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/accounts/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateAccount'

    PARAMS = {
        'uuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateAccountAction, self).__init__()
        self.uuid = None
        self.password = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateStopVmInstanceSchedulerAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/vm-instances/{vmUuid}/schedulers/stopping'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vmUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'schedulerName': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'schedulerDescription': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=True,valid_values=['simple','cron'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'interval': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'repeatCount': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'startTime': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'cron': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateStopVmInstanceSchedulerAction, self).__init__()
        self.vmUuid = None
        self.schedulerName = None
        self.schedulerDescription = None
        self.type = None
        self.interval = None
        self.repeatCount = None
        self.startTime = None
        self.cron = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SetVolumeQosAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/volumes/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'setVolumeQos'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'volumeBandwidth': ParamAnnotation(required=True,number_range=[1024, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SetVolumeQosAction, self).__init__()
        self.uuid = None
        self.volumeBandwidth = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateLoadBalancerAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/load-balancers'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vipUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateLoadBalancerAction, self).__init__()
        self.name = None
        self.description = None
        self.vipUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateCephPrimaryStorageMonAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/primary-storage/ceph/mons/{monUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateCephPrimaryStorageMon'

    PARAMS = {
        'monUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'hostname': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshUsername': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPassword': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateCephPrimaryStorageMonAction, self).__init__()
        self.monUuid = None
        self.hostname = None
        self.sshUsername = None
        self.sshPassword = None
        self.sshPort = None
        self.monPort = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateDiskOfferingAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/disk-offerings'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'diskSize': ParamAnnotation(required=True,number_range=[1, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sortKey': ParamAnnotation(),
        'allocationStrategy': ParamAnnotation(),
        'type': ParamAnnotation(required=False,valid_values=['zstack'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateDiskOfferingAction, self).__init__()
        self.name = None
        self.description = None
        self.diskSize = None
        self.sortKey = None
        self.allocationStrategy = None
        self.type = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateSystemTagAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/system-tags'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'resourceType': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'tag': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateSystemTagAction, self).__init__()
        self.resourceType = None
        self.resourceUuid = None
        self.tag = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteClusterAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/clusters/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteClusterAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVCenterAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/vcenters'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVCenterAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmMigrationCandidateHostsAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{vmInstanceUuid}/migration-target-hosts'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmMigrationCandidateHostsAction, self).__init__()
        self.vmInstanceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachBackupStorageToZoneAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/zones/{zoneUuid}/backup-storage/{backupStorageUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'backupStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachBackupStorageToZoneAction, self).__init__()
        self.zoneUuid = None
        self.backupStorageUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class LocalStorageGetVolumeMigratableHostsAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/volumes/{volumeUuid}/migration-target-hosts'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'volumeUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(LocalStorageGetVolumeMigratableHostsAction, self).__init__()
        self.volumeUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddVmNicToLoadBalancerAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/load-balancers/listeners/{listenerUuid}/vm-instances/nics'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vmNicUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'listenerUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddVmNicToLoadBalancerAction, self).__init__()
        self.vmNicUuids = None
        self.listenerUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateEcsSecurityGroupRuleRemoteAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/security-group-rule'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateEcsSecurityGroupRuleRemoteAction, self).__init__()
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CloneVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{vmInstanceUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'cloneVmInstance'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'strategy': ParamAnnotation(required=False,valid_values=['InstantStart','JustCreate'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'names': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CloneVmInstanceAction, self).__init__()
        self.vmInstanceUuid = None
        self.strategy = None
        self.names = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachOssBucketToEcsDataCenterAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/{dataCenterUuid}/oss-bucket/{ossBucketUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'ossBucketUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachOssBucketToEcsDataCenterAction, self).__init__()
        self.ossBucketUuid = None
        self.dataCenterUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryIdentityZoneFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/identity-zone'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryIdentityZoneFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryRouteEntryFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/route-entry'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryRouteEntryFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateVipAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vips/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateVip'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateVipAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateLdapBindingAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/ldap/bindings'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'ldapUid': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'accountUuid': ParamAnnotation(required=True,max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateLdapBindingAction, self).__init__()
        self.ldapUid = None
        self.accountUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryGlobalConfigAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/global-configurations'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryGlobalConfigAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteEcsVpcRemoteAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/vpc/remote/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteEcsVpcRemoteAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVolumeSnapshotTreeAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/volume-snapshots/trees'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVolumeSnapshotTreeAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddIdentityZoneFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/identity-zone'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'zoneId': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=True,valid_values=['aliyun'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddIdentityZoneFromRemoteAction, self).__init__()
        self.dataCenterUuid = None
        self.zoneId = None
        self.type = None
        self.description = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddFusionstorBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/backup-storage/fusionstor'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'monUrls': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'poolName': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'importImages': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddFusionstorBackupStorageAction, self).__init__()
        self.monUrls = None
        self.poolName = None
        self.url = None
        self.name = None
        self.description = None
        self.type = None
        self.importImages = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddKVMHostAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hosts/kvm'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'username': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'managementIp': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'clusterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddKVMHostAction, self).__init__()
        self.username = None
        self.password = None
        self.sshPort = None
        self.name = None
        self.description = None
        self.managementIp = None
        self.clusterUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateHostAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hosts/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateHost'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'managementIp': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateHostAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.managementIp = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryCephPrimaryStoragePoolAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/primary-storage/ceph/pools'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryCephPrimaryStoragePoolAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ReclaimSpaceFromImageStoreAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/backup-storage/image-store/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'reclaimSpaceFromImageStore'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ReclaimSpaceFromImageStoreAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class PrometheusQueryVmMonitoringDataAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/prometheus/vm-instances'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'vmUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'instant': ParamAnnotation(),
        'startTime': ParamAnnotation(required=False,number_range=[0, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'endTime': ParamAnnotation(required=False,number_range=[0, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'step': ParamAnnotation(),
        'expression': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'relativeTime': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(PrometheusQueryVmMonitoringDataAction, self).__init__()
        self.vmUuids = None
        self.instant = None
        self.startTime = None
        self.endTime = None
        self.step = None
        self.expression = None
        self.relativeTime = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetHostAllocatorStrategiesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hosts/allocators/strategies'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetHostAllocatorStrategiesAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachPrimaryStorageToClusterAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/clusters/{clusterUuid}/primary-storage/{primaryStorageUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'clusterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'primaryStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachPrimaryStorageToClusterAction, self).__init__()
        self.clusterUuid = None
        self.primaryStorageUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachPolicyToUserAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/accounts/users/{userUuid}/policies'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'userUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'policyUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachPolicyToUserAction, self).__init__()
        self.userUuid = None
        self.policyUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RemoveVmNicFromLoadBalancerAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/load-balancers/listeners/{listenerUuid}/vm-instances/nics'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vmNicUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'listenerUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RemoveVmNicFromLoadBalancerAction, self).__init__()
        self.vmNicUuids = None
        self.listenerUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetAccountQuotaUsageAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/accounts/quota/{uuid}/usages'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetAccountQuotaUsageAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateCephBackupStorageMonAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/backup-storage/ceph/mons/{monUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateCephBackupStorageMon'

    PARAMS = {
        'monUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'hostname': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshUsername': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPassword': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateCephBackupStorageMonAction, self).__init__()
        self.monUuid = None
        self.hostname = None
        self.sshUsername = None
        self.sshPassword = None
        self.sshPort = None
        self.monPort = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteSchedulerAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/schedulers/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteSchedulerAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class MigrateVmAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{vmInstanceUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'migrateVm'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'hostUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(MigrateVmAction, self).__init__()
        self.vmInstanceUuid = None
        self.hostUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class StartEcsInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hybrid/aliyun/ecs/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'startEcsInstance'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(StartEcsInstanceAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachPoliciesFromUserAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/accounts/users/{userUuid}/policies'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'policyUuids': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'userUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachPoliciesFromUserAction, self).__init__()
        self.policyUuids = None
        self.userUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateVolumeSnapshotSchedulerAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/volumes/{volumeUuid}/schedulers/creating-volume-snapshots'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'volumeUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'snapShotName': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'volumeSnapshotDescription': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'schedulerName': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'schedulerDescription': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=True,valid_values=['simple','cron'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'interval': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'repeatCount': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'startTime': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'cron': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateVolumeSnapshotSchedulerAction, self).__init__()
        self.volumeUuid = None
        self.snapShotName = None
        self.volumeSnapshotDescription = None
        self.schedulerName = None
        self.schedulerDescription = None
        self.type = None
        self.interval = None
        self.repeatCount = None
        self.startTime = None
        self.cron = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryBaremetalChessisAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/baremetal/chessis'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryBaremetalChessisAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SetVmBootOrderAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'setVmBootOrder'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'bootOrder': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SetVmBootOrderAction, self).__init__()
        self.uuid = None
        self.bootOrder = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteTagAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/tags/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteTagAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddSecurityGroupRuleAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/security-groups/{securityGroupUuid}/rules'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'securityGroupUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'rules': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddSecurityGroupRuleAction, self).__init__()
        self.securityGroupUuid = None
        self.rules = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVirtualBorderRouterLocalAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/border-router/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVirtualBorderRouterLocalAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryPortForwardingRuleAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/port-forwarding'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryPortForwardingRuleAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryPrimaryStorageAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/primary-storage'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryPrimaryStorageAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetCandidateVmForAttachingIsoAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/images/iso/{isoUuid}/vm-candidates'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'isoUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetCandidateVmForAttachingIsoAction, self).__init__()
        self.isoUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetBackupStorageCapacityAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/backup-storage/capacities'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'zoneUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'backupStorageUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'all': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetBackupStorageCapacityAction, self).__init__()
        self.zoneUuids = None
        self.backupStorageUuids = None
        self.all = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVCenterAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/vcenters/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVCenterAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmSshKeyAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{uuid}/ssh-keys'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmSshKeyAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVmStaticIpAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/vm-instances/{vmInstanceUuid}/static-ips'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVmStaticIpAction, self).__init__()
        self.vmInstanceUuid = None
        self.l3NetworkUuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryUserGroupAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/accounts/groups'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryUserGroupAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachL2NetworkFromClusterAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/l2-networks/{l2NetworkUuid}/clusters/{clusterUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'l2NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'clusterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachL2NetworkFromClusterAction, self).__init__()
        self.l2NetworkUuid = None
        self.clusterUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteDataVolumeAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/volumes/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteDataVolumeAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetPrimaryStorageTypesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/primary-storage/types'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetPrimaryStorageTypesAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetCandidateZonesClustersHostsForCreatingVmAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/candidate-destinations'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'instanceOfferingUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'imageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'rootDiskOfferingUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dataDiskOfferingUuids': ParamAnnotation(required=False,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'zoneUuid': ParamAnnotation(),
        'clusterUuid': ParamAnnotation(),
        'defaultL3NetworkUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetCandidateZonesClustersHostsForCreatingVmAction, self).__init__()
        self.instanceOfferingUuid = None
        self.imageUuid = None
        self.l3NetworkUuids = None
        self.rootDiskOfferingUuid = None
        self.dataDiskOfferingUuids = None
        self.zoneUuid = None
        self.clusterUuid = None
        self.defaultL3NetworkUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVipAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/vips/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVipAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateKVMHostAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hosts/kvm/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateKVMHost'

    PARAMS = {
        'username': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'managementIp': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateKVMHostAction, self).__init__()
        self.username = None
        self.password = None
        self.sshPort = None
        self.uuid = None
        self.name = None
        self.description = None
        self.managementIp = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVCenterDatacenterAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/vcenters/datacenters'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVCenterDatacenterAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteExportedImageFromBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/backup-storage/{backupStorageUuid}/exported-images/{imageUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'backupStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'imageUuid': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteExportedImageFromBackupStorageAction, self).__init__()
        self.backupStorageUuid = None
        self.imageUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryHybridEipFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/eip'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryHybridEipFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVniRangeAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/l2-networks/vxlan-pool/vni-range'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVniRangeAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateLdapServerAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/ldap/servers/{ldapServerUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateLdapServer'

    PARAMS = {
        'ldapServerUuid': ParamAnnotation(required=True,max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=False,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'base': ParamAnnotation(required=False,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'username': ParamAnnotation(required=False,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=False,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'encryption': ParamAnnotation(required=False,valid_values=['None','TLS'],max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateLdapServerAction, self).__init__()
        self.ldapServerUuid = None
        self.name = None
        self.description = None
        self.url = None
        self.base = None
        self.username = None
        self.password = None
        self.encryption = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class TriggerGCJobAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/gc-jobs/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'triggerGCJob'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(TriggerGCJobAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryEcsInstanceFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/ecs'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryEcsInstanceFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateDiskOfferingAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/disk-offerings/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateDiskOffering'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateDiskOfferingAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryL2VlanNetworkAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/l2-networks/vlan'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryL2VlanNetworkAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVmNicInSecurityGroupAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/security-groups/vm-instances/nics'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVmNicInSecurityGroupAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class LogInByAccountAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/accounts/login'
    NEED_SESSION = False
    NEED_POLL = False
    PARAM_NAME = 'logInByAccount'

    PARAMS = {
        'accountName': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation()
    }

    def __init__(self):
        super(LogInByAccountAction, self).__init__()
        self.accountName = None
        self.password = None
        self.systemTags = None
        self.userTags = None


class ChangeVmPasswordAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeVmPassword'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,valid_regex_values=r'[\da-zA-Z-`=\\\[\];',./~!@#$%^&*()_+|{}:"<>?]{1,}',max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=True),
        'account': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=True),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeVmPasswordAction, self).__init__()
        self.uuid = None
        self.password = None
        self.account = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVirtualRouterOfferingAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/instance-offerings/virtual-routers'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVirtualRouterOfferingAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetCpuMemoryCapacityAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hosts/capacities/cpu-memory'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'zoneUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'clusterUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'hostUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'all': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetCpuMemoryCapacityAction, self).__init__()
        self.zoneUuids = None
        self.clusterUuids = None
        self.hostUuids = None
        self.all = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeSchedulerStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/schedulers/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeSchedulerState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeSchedulerStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmBootOrderAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{uuid}/boot-orders'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmBootOrderAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateDataVolumeFromVolumeTemplateAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/volumes/data/from/data-volume-templates/{imageUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'imageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'primaryStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'hostUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateDataVolumeFromVolumeTemplateAction, self).__init__()
        self.imageUuid = None
        self.name = None
        self.description = None
        self.primaryStorageUuid = None
        self.hostUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncVolumeSizeAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/volumes/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncVolumeSize'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncVolumeSizeAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class StopVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'stopVmInstance'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=False,valid_values=['grace','cold'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(StopVmInstanceAction, self).__init__()
        self.uuid = None
        self.type = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class StartVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'startVmInstance'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'clusterUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'hostUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(StartVmInstanceAction, self).__init__()
        self.uuid = None
        self.clusterUuid = None
        self.hostUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeVolumeStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/volumes/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeVolumeState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeVolumeStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateRouteInterfaceRemoteAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hybrid/aliyun/router-interface/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateRouteInterfaceRemote'

    PARAMS = {
        'riUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'op': ParamAnnotation(required=True,valid_values=['active','inactive'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vRouterType': ParamAnnotation(required=True,valid_values=['vbr','vrouter'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateRouteInterfaceRemoteAction, self).__init__()
        self.riUuid = None
        self.op = None
        self.vRouterType = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddLocalPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/primary-storage/local-storage'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'url': ParamAnnotation(required=True,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddLocalPrimaryStorageAction, self).__init__()
        self.url = None
        self.name = None
        self.description = None
        self.type = None
        self.zoneUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryImageAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/images'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryImageAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SetNicQosAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'setNicQos'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'outboundBandwidth': ParamAnnotation(required=False,number_range=[8192, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'inboundBandwidth': ParamAnnotation(required=False,number_range=[8192, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SetNicQosAction, self).__init__()
        self.uuid = None
        self.outboundBandwidth = None
        self.inboundBandwidth = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteRouterInterfaceRemoteAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/router-interface/remote/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vRouterType': ParamAnnotation(required=True,valid_values=['vrouter','vbr'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteRouterInterfaceRemoteAction, self).__init__()
        self.uuid = None
        self.vRouterType = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RemoveMonFromCephBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/backup-storage/ceph/{uuid}/mons'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monHostnames': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RemoveMonFromCephBackupStorageAction, self).__init__()
        self.uuid = None
        self.monHostnames = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddFusionstorPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/primary-storage/fusionstor'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'monUrls': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'rootVolumePoolName': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dataVolumePoolName': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'imageCachePoolName': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddFusionstorPrimaryStorageAction, self).__init__()
        self.monUrls = None
        self.rootVolumePoolName = None
        self.dataVolumePoolName = None
        self.imageCachePoolName = None
        self.url = None
        self.name = None
        self.description = None
        self.type = None
        self.zoneUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetCurrentTimeAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/management-nodes/actions'
    NEED_SESSION = False
    NEED_POLL = False
    PARAM_NAME = 'getCurrentTime'

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation()
    }

    def __init__(self):
        super(GetCurrentTimeAction, self).__init__()
        self.systemTags = None
        self.userTags = None


class DetachPolicyFromUserGroupAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/accounts/groups/{groupUuid}/policies/{policyUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'policyUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'groupUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachPolicyFromUserGroupAction, self).__init__()
        self.policyUuid = None
        self.groupUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateVirtualRouterOfferingAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/instance-offerings/virtual-routers/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateVirtualRouterOffering'

    PARAMS = {
        'isDefault': ParamAnnotation(),
        'imageUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateVirtualRouterOfferingAction, self).__init__()
        self.isDefault = None
        self.imageUuid = None
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddImageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/images'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=True,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'mediaType': ParamAnnotation(required=False,valid_values=['RootVolumeTemplate','ISO','DataVolumeTemplate'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'guestOsType': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'system': ParamAnnotation(),
        'format': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'platform': ParamAnnotation(required=False,valid_values=['Linux','Windows','Other','Paravirtualization','WindowsVirtio'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'backupStorageUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddImageAction, self).__init__()
        self.name = None
        self.description = None
        self.url = None
        self.mediaType = None
        self.guestOsType = None
        self.system = None
        self.format = None
        self.platform = None
        self.backupStorageUuids = None
        self.type = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateL2VxlanNetworkAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/l2-networks/vxlan'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vni': ParamAnnotation(required=False,number_range=[1, 16777214],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'poolUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'zoneUuid': ParamAnnotation(required=False,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'physicalInterface': ParamAnnotation(required=False,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateL2VxlanNetworkAction, self).__init__()
        self.vni = None
        self.poolUuid = None
        self.name = None
        self.description = None
        self.zoneUuid = None
        self.physicalInterface = None
        self.type = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncVirtualRouterFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/vrouter/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncVirtualRouterFromRemote'

    PARAMS = {
        'vpcUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vRouterUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncVirtualRouterFromRemoteAction, self).__init__()
        self.vpcUuid = None
        self.vRouterUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmAttachableL3NetworkAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{vmInstanceUuid}/l3-networks-candidates'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmAttachableL3NetworkAction, self).__init__()
        self.vmInstanceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddSftpBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/backup-storage/sftp'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'hostname': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'username': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=True,max_length=2048,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'importImages': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddSftpBackupStorageAction, self).__init__()
        self.hostname = None
        self.username = None
        self.password = None
        self.sshPort = None
        self.url = None
        self.name = None
        self.description = None
        self.type = None
        self.importImages = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVersionAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/management-nodes/actions'
    NEED_SESSION = False
    NEED_POLL = False
    PARAM_NAME = 'getVersion'

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation()
    }

    def __init__(self):
        super(GetVersionAction, self).__init__()
        self.systemTags = None
        self.userTags = None


class DetachPolicyFromUserAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/accounts/users/{userUuid}/policies/{policyUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'policyUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'userUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachPolicyFromUserAction, self).__init__()
        self.policyUuid = None
        self.userUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RefreshLoadBalancerAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/load-balancers/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'refreshLoadBalancer'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RefreshLoadBalancerAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddLdapServerAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/ldap/servers'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=True,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=True,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'base': ParamAnnotation(required=True,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'username': ParamAnnotation(required=True,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'encryption': ParamAnnotation(required=True,valid_values=['None','TLS'],max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddLdapServerAction, self).__init__()
        self.name = None
        self.description = None
        self.url = None
        self.base = None
        self.username = None
        self.password = None
        self.encryption = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetDataVolumeAttachableVmAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/volumes/{volumeUuid}/candidate-vm-instances'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'volumeUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetDataVolumeAttachableVmAction, self).__init__()
        self.volumeUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateEcsInstanceFromLocalImageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/ecs'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'ecsRootVolumeType': ParamAnnotation(required=False,valid_values=['cloud','cloud_efficiency','cloud_ssd','ephemeral_ssd'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=256,min_length=2,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ecsRootVolumeGBSize': ParamAnnotation(required=False,number_range=[40, 500],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'createMode': ParamAnnotation(required=False,valid_values=['atomic','permissive'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'privateIpAddress': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ecsInstanceName': ParamAnnotation(required=False,max_length=128,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'allocatePublicIp': ParamAnnotation(required=False,valid_values=['true','false'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'identityZoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'backupStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'imageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'instanceOfferingUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ecsVSwitchUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ecsSecurityGroupUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ecsRootPassword': ParamAnnotation(required=True,valid_regex_values=r'^[a-zA-Z][\w\W]{7,17}$',max_length=30,min_length=8,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ecsBandWidth': ParamAnnotation(required=True,number_range=[0, 200],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateEcsInstanceFromLocalImageAction, self).__init__()
        self.ecsRootVolumeType = None
        self.description = None
        self.ecsRootVolumeGBSize = None
        self.createMode = None
        self.privateIpAddress = None
        self.ecsInstanceName = None
        self.allocatePublicIp = None
        self.identityZoneUuid = None
        self.backupStorageUuid = None
        self.imageUuid = None
        self.instanceOfferingUuid = None
        self.ecsVSwitchUuid = None
        self.ecsSecurityGroupUuid = None
        self.ecsRootPassword = None
        self.ecsBandWidth = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryFusionstorBackupStorageAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/backup-storage/fusionstor'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryFusionstorBackupStorageAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateBaremetalPxeServerAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/baremetal/pxeserver'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'dhcpInterface': ParamAnnotation(required=True,max_length=128,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'dhcpRangeBegin': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dhcpRangeEnd': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dhcpRangeNetmask': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateBaremetalPxeServerAction, self).__init__()
        self.dhcpInterface = None
        self.dhcpRangeBegin = None
        self.dhcpRangeEnd = None
        self.dhcpRangeNetmask = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachEipAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/eips/{eipUuid}/vm-instances/nics/{vmNicUuid'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'eipUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vmNicUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachEipAction, self).__init__()
        self.eipUuid = None
        self.vmNicUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateBaremetalHostCfgAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/baremetal/hostcfg/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateBaremetalHostCfg'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'vnc': ParamAnnotation(required=False,valid_values=['true','false'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'unattended': ParamAnnotation(required=False,valid_values=['true','false'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'nicCfgs': ParamAnnotation(required=False,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'chessisUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateBaremetalHostCfgAction, self).__init__()
        self.uuid = None
        self.password = None
        self.vnc = None
        self.unattended = None
        self.nicCfgs = None
        self.chessisUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteHostAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hosts/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteHostAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ReloadLicenseAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/licenses/actions'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'reloadLicense'

    PARAMS = {
        'managementNodeUuids': ParamAnnotation(required=False,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ReloadLicenseAction, self).__init__()
        self.managementNodeUuids = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncEcsVpcFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/vpc/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncEcsVpcFromRemote'

    PARAMS = {
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ecsVpcId': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncEcsVpcFromRemoteAction, self).__init__()
        self.dataCenterUuid = None
        self.ecsVpcId = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateEcsSecurityGroupRemoteAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/security-group/remote'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateEcsSecurityGroupRemoteAction, self).__init__()
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVirtualBorderRouterFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/border-router'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVirtualBorderRouterFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/vm-instances'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'instanceOfferingUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'imageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=False,valid_values=['UserVm','ApplianceVm'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'rootDiskOfferingUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dataDiskOfferingUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'zoneUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'clusterUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'hostUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'primaryStorageUuidForRootVolume': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'defaultL3NetworkUuid': ParamAnnotation(),
        'strategy': ParamAnnotation(required=False,valid_values=['InstantStart','JustCreate'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateVmInstanceAction, self).__init__()
        self.name = None
        self.instanceOfferingUuid = None
        self.imageUuid = None
        self.l3NetworkUuids = None
        self.type = None
        self.rootDiskOfferingUuid = None
        self.dataDiskOfferingUuids = None
        self.zoneUuid = None
        self.clusterUuid = None
        self.hostUuid = None
        self.primaryStorageUuidForRootVolume = None
        self.description = None
        self.defaultL3NetworkUuid = None
        self.strategy = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetHypervisorTypesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hosts/hypervisor-types'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetHypervisorTypesAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVolumeSnapshotAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/volume-snapshots/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVolumeSnapshotAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryAccountResourceRefAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/accounts/resources/refs'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryAccountResourceRefAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddMonToFusionstorPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/primary-storage/fusionstor/{uuid}/mons'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monUrls': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddMonToFusionstorPrimaryStorageAction, self).__init__()
        self.uuid = None
        self.monUrls = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateUserGroupAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/accounts/groups/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateUserGroup'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateUserGroupAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdatePortForwardingRuleAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/port-forwarding/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updatePortForwardingRule'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdatePortForwardingRuleAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ReconnectConsoleProxyAgentAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/consoles/agents'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'reconnectConsoleProxyAgent'

    PARAMS = {
        'agentUuids': ParamAnnotation(required=False,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ReconnectConsoleProxyAgentAction, self).__init__()
        self.agentUuids = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateVirtualRouterOfferingAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/instance-offerings/virtual-routers'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'managementNetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'imageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'publicNetworkUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'isDefault': ParamAnnotation(),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'cpuNum': ParamAnnotation(required=True,number_range=[1, 1024],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'memorySize': ParamAnnotation(required=True,number_range=[1, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'allocatorStrategy': ParamAnnotation(),
        'sortKey': ParamAnnotation(),
        'type': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateVirtualRouterOfferingAction, self).__init__()
        self.zoneUuid = None
        self.managementNetworkUuid = None
        self.imageUuid = None
        self.publicNetworkUuid = None
        self.isDefault = None
        self.name = None
        self.description = None
        self.cpuNum = None
        self.memorySize = None
        self.allocatorStrategy = None
        self.sortKey = None
        self.type = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateBaremetalChessisAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/baremetal/chessis/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateBaremetalChessis'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ipmiAddress': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ipmiUsername': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ipmiPassword': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'provisioned': ParamAnnotation(required=False,valid_values=['true','false'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateBaremetalChessisAction, self).__init__()
        self.uuid = None
        self.ipmiAddress = None
        self.ipmiUsername = None
        self.ipmiPassword = None
        self.provisioned = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteIpRangeAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/l3-networks/ip-ranges/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteIpRangeAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncRouterInterfaceFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/router-interface/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncRouterInterfaceFromRemote'

    PARAMS = {
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncRouterInterfaceFromRemoteAction, self).__init__()
        self.dataCenterUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetPortForwardingAttachableVmNicsAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/port-forwarding/{ruleUuid}/vm-instances/candidate-nics'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'ruleUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetPortForwardingAttachableVmNicsAction, self).__init__()
        self.ruleUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetL2NetworkTypesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/l2-networks/types'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetL2NetworkTypesAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CheckApiPermissionAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/accounts/permissions/actions'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'checkApiPermission'

    PARAMS = {
        'userUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'apiNames': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CheckApiPermissionAction, self).__init__()
        self.userUuid = None
        self.apiNames = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetTaskProgressAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/task-progresses/{apiId}'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'apiId': ParamAnnotation(),
        'all': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetTaskProgressAction, self).__init__()
        self.apiId = None
        self.all = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ExpungeDataVolumeAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/volumes/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'expungeDataVolume'

    PARAMS = {
        'uuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ExpungeDataVolumeAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachNetworkServiceToL3NetworkAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/l3-networks/{l3NetworkUuid}/network-services'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'networkServices': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachNetworkServiceToL3NetworkAction, self).__init__()
        self.l3NetworkUuid = None
        self.networkServices = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RemoveMonFromCephPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/primary-storage/ceph/{uuid}/mons'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monHostnames': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RemoveMonFromCephPrimaryStorageAction, self).__init__()
        self.uuid = None
        self.monHostnames = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ReconnectImageStoreBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/backup-storage/image-store/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'reconnectImageStoreBackupStorage'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ReconnectImageStoreBackupStorageAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteSecurityGroupAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/security-groups/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteSecurityGroupAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QuerySecurityGroupRuleAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/security-groups/rules'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QuerySecurityGroupRuleAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QuerySecurityGroupAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/security-groups'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QuerySecurityGroupAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetDataCenterFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/data-center/remote'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'type': ParamAnnotation(required=True,valid_values=['aliyun'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetDataCenterFromRemoteAction, self).__init__()
        self.type = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddVmNicToSecurityGroupAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/security-groups/{securityGroupUuid}/vm-instances/nics'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'securityGroupUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vmNicUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddVmNicToSecurityGroupAction, self).__init__()
        self.securityGroupUuid = None
        self.vmNicUuids = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeletePortForwardingRuleAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/port-forwarding/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeletePortForwardingRuleAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddMonToFusionstorBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/backup-storage/fusionstor/{uuid}/mons'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monUrls': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddMonToFusionstorBackupStorageAction, self).__init__()
        self.uuid = None
        self.monUrls = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ExportImageFromBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/backup-storage/{backupStorageUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'exportImageFromBackupStorage'

    PARAMS = {
        'backupStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'imageUuid': ParamAnnotation(required=True,max_length=2048,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ExportImageFromBackupStorageAction, self).__init__()
        self.backupStorageUuid = None
        self.imageUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SetVmHostnameAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'setVmHostname'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'hostname': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SetVmHostnameAction, self).__init__()
        self.uuid = None
        self.hostname = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetPrimaryStorageAllocatorStrategiesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/primary-storage/allocators/strategies'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetPrimaryStorageAllocatorStrategiesAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetLocalStorageHostDiskCapacityAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/primary-storage/local-storage/{primaryStorageUuid}/capacities'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'hostUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'primaryStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetLocalStorageHostDiskCapacityAction, self).__init__()
        self.hostUuid = None
        self.primaryStorageUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryNetworkServiceL3NetworkRefAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/l3-networks/network-services/refs'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryNetworkServiceL3NetworkRefAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class LocalStorageMigrateVolumeAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/primary-storage/local-storage/volumes/{volumeUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'localStorageMigrateVolume'

    PARAMS = {
        'volumeUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'destHostUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(LocalStorageMigrateVolumeAction, self).__init__()
        self.volumeUuid = None
        self.destHostUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetCandidateBackupStorageForCreatingImageAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = 'null'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'volumeUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'volumeSnapshotUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetCandidateBackupStorageForCreatingImageAction, self).__init__()
        self.volumeUuid = None
        self.volumeSnapshotUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryL2NetworkAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/l2-networks'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryL2NetworkAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ResumeVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'resumeVmInstance'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ResumeVmInstanceAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachPolicyToUserGroupAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/accounts/groups/{groupUuid}/policies'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'policyUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'groupUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachPolicyToUserGroupAction, self).__init__()
        self.policyUuid = None
        self.groupUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVmInstanceHaLevelAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/vm-instances/{uuid}/ha-levels'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVmInstanceHaLevelAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryEcsVSwitchFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/vswitch'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryEcsVSwitchFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class IsReadyToGoAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/management-nodes/ready'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'managementNodeId': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(IsReadyToGoAction, self).__init__()
        self.managementNodeId = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVmHostnameAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/vm-instances/{uuid}/hostnames'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVmHostnameAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachPrimaryStorageFromClusterAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/clusters/{clusterUuid}/primary-storage/{primaryStorageUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'primaryStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'clusterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachPrimaryStorageFromClusterAction, self).__init__()
        self.primaryStorageUuid = None
        self.clusterUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddSimulatorPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/primary-storage/simulators'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'totalCapacity': ParamAnnotation(),
        'availableCapacity': ParamAnnotation(),
        'url': ParamAnnotation(required=True,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddSimulatorPrimaryStorageAction, self).__init__()
        self.totalCapacity = None
        self.availableCapacity = None
        self.url = None
        self.name = None
        self.description = None
        self.type = None
        self.zoneUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateEcsVpcRemoteAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/vpc'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateEcsVpcRemoteAction, self).__init__()
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddCephPrimaryStoragePoolAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/primary-storage/ceph/{primaryStorageUuid}/pools'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'primaryStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'poolName': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'errorIfNotExist': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddCephPrimaryStoragePoolAction, self).__init__()
        self.primaryStorageUuid = None
        self.poolName = None
        self.description = None
        self.errorIfNotExist = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddVCenterAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/vcenters'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'username': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'https': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'port': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'domainName': ParamAnnotation(required=True,max_length=256,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddVCenterAction, self).__init__()
        self.username = None
        self.password = None
        self.zoneUuid = None
        self.name = None
        self.https = None
        self.port = None
        self.domainName = None
        self.description = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateL3NetworkAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/l3-networks'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'l2NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'system': ParamAnnotation(),
        'dnsDomain': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateL3NetworkAction, self).__init__()
        self.name = None
        self.description = None
        self.type = None
        self.l2NetworkUuid = None
        self.system = None
        self.dnsDomain = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateVmInstance'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'state': ParamAnnotation(required=False,valid_values=['Stopped','Running'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'defaultL3NetworkUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'platform': ParamAnnotation(required=False,valid_values=['Linux','Windows','Other','Paravirtualization','WindowsVirtio'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'cpuNum': ParamAnnotation(required=False,number_range=[1, 1024],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'memorySize': ParamAnnotation(required=False,number_range=[1, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateVmInstanceAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.state = None
        self.defaultL3NetworkUuid = None
        self.platform = None
        self.cpuNum = None
        self.memorySize = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ReconnectVirtualRouterAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/appliances/virtual-routers/{vmInstanceUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'reconnectVirtualRouter'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ReconnectVirtualRouterAction, self).__init__()
        self.vmInstanceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryLoadBalancerListenerAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/load-balancers/listeners'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryLoadBalancerListenerAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryResourcePriceAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/billings/prices'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryResourcePriceAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryWebhookAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/web-hooks'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryWebhookAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteConnectionAccessPointLocalAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/access-point/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteConnectionAccessPointLocalAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ReconnectPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/primary-storage/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'reconnectPrimaryStorage'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ReconnectPrimaryStorageAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CleanUpImageCacheOnPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/primary-storage/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'cleanUpImageCacheOnPrimaryStorage'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CleanUpImageCacheOnPrimaryStorageAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SetImageQgaAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/images/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'setImageQga'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'enable': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SetImageQgaAction, self).__init__()
        self.uuid = None
        self.enable = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetEipAttachableVmNicsAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/eips/{eipUuid}/vm-instances/candidate-nics'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'eipUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vipUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetEipAttachableVmNicsAction, self).__init__()
        self.eipUuid = None
        self.vipUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryZoneAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/zones'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryZoneAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachSecurityGroupFromL3NetworkAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/security-groups/{securityGroupUuid}/l3-networks/{l3NetworkUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'securityGroupUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachSecurityGroupFromL3NetworkAction, self).__init__()
        self.securityGroupUuid = None
        self.l3NetworkUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryEcsVpcFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/vpc'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryEcsVpcFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateDataVolumeFromVolumeSnapshotAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/volumes/data/from/volume-snapshots/{volumeSnapshotUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'volumeSnapshotUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'primaryStorageUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateDataVolumeFromVolumeSnapshotAction, self).__init__()
        self.name = None
        self.description = None
        self.volumeSnapshotUuid = None
        self.primaryStorageUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeEipStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/eips/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeEipState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeEipStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateL2NetworkAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/l2-networks/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateL2Network'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateL2NetworkAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachNetworkServiceFromL3NetworkAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/l3-networks/{l3NetworkUuid}/network-services'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'networkServices': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachNetworkServiceFromL3NetworkAction, self).__init__()
        self.l3NetworkUuid = None
        self.networkServices = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddNfsPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/primary-storage/nfs'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'url': ParamAnnotation(required=True,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddNfsPrimaryStorageAction, self).__init__()
        self.url = None
        self.name = None
        self.description = None
        self.type = None
        self.zoneUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateEcsVSwitchRemoteAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/vswitch'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateEcsVSwitchRemoteAction, self).__init__()
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateAccountAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/accounts'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=False,valid_values=['SystemAdmin','Normal'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateAccountAction, self).__init__()
        self.name = None
        self.password = None
        self.type = None
        self.description = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmStartingCandidateClustersHostsAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{uuid}/starting-target-hosts'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmStartingCandidateClustersHostsAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RebootVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'rebootVmInstance'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RebootVmInstanceAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmAttachableDataVolumeAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{vmInstanceUuid}/data-volume-candidates'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmAttachableDataVolumeAction, self).__init__()
        self.vmInstanceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CleanInvalidLdapBindingAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/ldap/bindings/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'cleanInvalidLdapBinding'

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CleanInvalidLdapBindingAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryAliyunKeySecretAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/key'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryAliyunKeySecretAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeClusterStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/clusters/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeClusterState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeClusterStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachDataVolumeToVmAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/volumes/{volumeUuid}/vm-instances/{vmInstanceUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'volumeUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachDataVolumeToVmAction, self).__init__()
        self.vmInstanceUuid = None
        self.volumeUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RemoveUserFromGroupAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/accounts/groups/{groupUuid}/users/{userUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'userUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'groupUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RemoveUserFromGroupAction, self).__init__()
        self.userUuid = None
        self.groupUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddSimulatorHostAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hosts/simulators'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'memoryCapacity': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'cpuCapacity': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'managementIp': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'clusterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddSimulatorHostAction, self).__init__()
        self.memoryCapacity = None
        self.cpuCapacity = None
        self.name = None
        self.description = None
        self.managementIp = None
        self.clusterUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVipAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/vips'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVipAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RecoverDataVolumeAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/volumes/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'recoverDataVolume'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RecoverDataVolumeAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryEcsSecurityGroupFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/security-group'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryEcsSecurityGroupFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVirtualRouterVmAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/appliances/virtual-routers'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVirtualRouterVmAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateUserGroupAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/accounts/groups'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateUserGroupAction, self).__init__()
        self.name = None
        self.description = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateImageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/images/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateImage'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'guestOsType': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'mediaType': ParamAnnotation(required=False,valid_values=['RootVolumeTemplate','DataVolumeTemplate','ISO'],max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'format': ParamAnnotation(required=False,valid_values=['raw','qcow2','iso'],max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'system': ParamAnnotation(),
        'platform': ParamAnnotation(required=False,valid_values=['Linux','Windows','Other','Paravirtualization','WindowsVirtio'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateImageAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.guestOsType = None
        self.mediaType = None
        self.format = None
        self.system = None
        self.platform = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class KvmRunShellAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hosts/kvm/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'kvmRunShell'

    PARAMS = {
        'hostUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'script': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(KvmRunShellAction, self).__init__()
        self.hostUuids = None
        self.script = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateVolumeAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/volumes/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateVolume'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateVolumeAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachIsoFromVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/vm-instances/{vmInstanceUuid}/iso'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachIsoFromVmInstanceAction, self).__init__()
        self.vmInstanceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVCenterPrimaryStorageAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/vcenters/primary-storage'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVCenterPrimaryStorageAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateVolumeSnapshotAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/volumes/{volumeUuid}/volume-snapshots'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'volumeUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateVolumeSnapshotAction, self).__init__()
        self.volumeUuid = None
        self.name = None
        self.description = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RemoveDnsFromL3NetworkAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/l3-networks/{l3NetworkUuid}/dns/{dns}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dns': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RemoveDnsFromL3NetworkAction, self).__init__()
        self.l3NetworkUuid = None
        self.dns = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ValidateSessionAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/accounts/sessions/{sessionUuid}/valid'
    NEED_SESSION = False
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'sessionUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation()
    }

    def __init__(self):
        super(ValidateSessionAction, self).__init__()
        self.sessionUuid = None
        self.systemTags = None
        self.userTags = None


class QueryDiskOfferingAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/disk-offerings'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryDiskOfferingAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteResourcePriceAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/billings/prices/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteResourcePriceAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVCenterClusterAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/vcenters/clusters'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVCenterClusterAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachAliyunKeyAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hybrid/aliyun/key/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'detachAliyunKey'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachAliyunKeyAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddImageStoreBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/backup-storage/image-store'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'hostname': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'username': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=True,max_length=2048,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'importImages': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddImageStoreBackupStorageAction, self).__init__()
        self.hostname = None
        self.username = None
        self.password = None
        self.sshPort = None
        self.url = None
        self.name = None
        self.description = None
        self.type = None
        self.importImages = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateSecurityGroupAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/security-groups/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateSecurityGroup'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateSecurityGroupAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RecoveryVirtualBorderRouterRemoteAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hybrid/aliyun/border-router/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'recoveryVirtualBorderRouterRemote'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RecoveryVirtualBorderRouterRemoteAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryInstanceOfferingAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/instance-offerings'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryInstanceOfferingAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVmNicFromSecurityGroupAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/security-groups/{securityGroupUuid}/vm-instances/nics'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'securityGroupUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vmNicUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVmNicFromSecurityGroupAction, self).__init__()
        self.securityGroupUuid = None
        self.vmNicUuids = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SetVmQgaAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'setVmQga'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'enable': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SetVmQgaAction, self).__init__()
        self.uuid = None
        self.enable = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryBaremetalPxeServerAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/baremetal/pxeserver'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryBaremetalPxeServerAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetL3NetworkDhcpIpAddressAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/l3-networks/{l3NetworkUuid/dhcp-ip'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'l3NetworkUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetL3NetworkDhcpIpAddressAction, self).__init__()
        self.l3NetworkUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddDataCenterFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/data-center'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'regionId': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=True,valid_values=['aliyun'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddDataCenterFromRemoteAction, self).__init__()
        self.regionId = None
        self.type = None
        self.description = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateInstanceOfferingAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/instance-offerings/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateInstanceOffering'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateInstanceOfferingAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateSecurityGroupAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/security-groups'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateSecurityGroupAction, self).__init__()
        self.name = None
        self.description = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryLdapBindingAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/ldap/bindings'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryLdapBindingAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachAliyunKeyAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hybrid/aliyun/key/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'attachAliyunKey'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachAliyunKeyAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateRouterInterfacePairRemoteAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/router-interface'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'accessPointUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'Spec': ParamAnnotation(required=True,valid_values=['Small.1','Small.2','Small.5','Middle.1','Middle.2','Middle.5','Large.1','Large.2'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vRouterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vBorderRouterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'aDescription': ParamAnnotation(required=False,max_length=128,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'aName': ParamAnnotation(required=False,max_length=64,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'bDescription': ParamAnnotation(required=False,max_length=128,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'bName': ParamAnnotation(required=False,max_length=64,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ownerName': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateRouterInterfacePairRemoteAction, self).__init__()
        self.dataCenterUuid = None
        self.accessPointUuid = None
        self.Spec = None
        self.vRouterUuid = None
        self.vBorderRouterUuid = None
        self.aDescription = None
        self.aName = None
        self.bDescription = None
        self.bName = None
        self.ownerName = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateSystemTagAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/system-tags/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateSystemTag'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'tag': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateSystemTagAction, self).__init__()
        self.uuid = None
        self.tag = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVCenterBackupStorageAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/vcenters/backup-storage'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVCenterBackupStorageAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVirtualRouterFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/vrouter'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVirtualRouterFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateL3NetworkAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/l3-networks/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateL3Network'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'system': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateL3NetworkAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.system = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateVolumeSnapshotAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/volume-snapshots/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateVolumeSnapshot'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateVolumeSnapshotAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RevokeResourceSharingAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/accounts/resources/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'revokeResourceSharing'

    PARAMS = {
        'resourceUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'toPublic': ParamAnnotation(),
        'accountUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'all': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RevokeResourceSharingAction, self).__init__()
        self.resourceUuids = None
        self.toPublic = None
        self.accountUuids = None
        self.all = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryClusterAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/clusters'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryClusterAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteWebhookAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/web-hooks/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteWebhookAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryL3NetworkAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/l3-networks'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryL3NetworkAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetIpAddressCapacityAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/ip-capacity'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'zoneUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ipRangeUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'all': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetIpAddressCapacityAction, self).__init__()
        self.zoneUuids = None
        self.l3NetworkUuids = None
        self.ipRangeUuids = None
        self.all = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryNotificationSubscriptionAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/notifications/subscriptions'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryNotificationSubscriptionAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetOssBucketNameFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/oss/remote'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetOssBucketNameFromRemoteAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateVipAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/vips'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'allocatorStrategy': ParamAnnotation(),
        'requiredIp': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateVipAction, self).__init__()
        self.name = None
        self.description = None
        self.l3NetworkUuid = None
        self.allocatorStrategy = None
        self.requiredIp = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryDataCenterFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/data-center'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryDataCenterFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetIdentityZoneFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/identity-zone/remote'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'type': ParamAnnotation(required=True,valid_values=['aliyun'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dataCenterUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'regionId': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetIdentityZoneFromRemoteAction, self).__init__()
        self.type = None
        self.dataCenterUuid = None
        self.regionId = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncPrimaryStorageCapacityAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/primary-storage/{primaryStorageUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncPrimaryStorageCapacity'

    PARAMS = {
        'primaryStorageUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncPrimaryStorageCapacityAction, self).__init__()
        self.primaryStorageUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachEipAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/eips/{uuid}/vm-instances/nics'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachEipAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmCapabilitiesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{uuid}/capabilities'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmCapabilitiesAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreatePolicyAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/accounts/policies'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'statements': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreatePolicyAction, self).__init__()
        self.name = None
        self.description = None
        self.statements = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateEipAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/eips'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vipUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vmNicUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateEipAction, self).__init__()
        self.name = None
        self.description = None
        self.vipUuid = None
        self.vmNicUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateDataVolumeTemplateFromVolumeAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/images/data-volume-templates/from/volumes/{volumeUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'volumeUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'backupStorageUuids': ParamAnnotation(required=False,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateDataVolumeTemplateFromVolumeAction, self).__init__()
        self.name = None
        self.description = None
        self.volumeUuid = None
        self.backupStorageUuids = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CalculateAccountSpendingAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/billings/accounts/{accountUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'calculateAccountSpending'

    PARAMS = {
        'accountUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dateStart': ParamAnnotation(required=False,number_range=[0, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dateEnd': ParamAnnotation(required=False,number_range=[0, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CalculateAccountSpendingAction, self).__init__()
        self.accountUuid = None
        self.dateStart = None
        self.dateEnd = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreatePortForwardingRuleAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/port-forwarding'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vipUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vipPortStart': ParamAnnotation(required=True,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vipPortEnd': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'privatePortStart': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'privatePortEnd': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'protocolType': ParamAnnotation(required=True,valid_values=['TCP','UDP'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vmNicUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'allowedCidr': ParamAnnotation(),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreatePortForwardingRuleAction, self).__init__()
        self.vipUuid = None
        self.vipPortStart = None
        self.vipPortEnd = None
        self.privatePortStart = None
        self.privatePortEnd = None
        self.protocolType = None
        self.vmNicUuid = None
        self.allowedCidr = None
        self.name = None
        self.description = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddUserToGroupAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/accounts/groups/{groupUuid}/users'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'addUserToGroup'

    PARAMS = {
        'userUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'groupUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddUserToGroupAction, self).__init__()
        self.userUuid = None
        self.groupUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddDnsToL3NetworkAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/l3-networks/{l3NetworkUuid}/dns'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dns': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddDnsToL3NetworkAction, self).__init__()
        self.l3NetworkUuid = None
        self.dns = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteAccountAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/accounts/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteAccountAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateEcsInstanceVncPasswordAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hybrid/aliyun/ecs-vnc/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateEcsInstanceVncPassword'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,valid_regex_values=r'[A-Za-z0-9]{6}',max_length=6,min_length=6,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateEcsInstanceVncPasswordAction, self).__init__()
        self.uuid = None
        self.password = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateRootVolumeTemplateFromVolumeSnapshotAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/images/root-volume-templates/from/volume-snapshots/{snapshotUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'snapshotUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'guestOsType': ParamAnnotation(),
        'backupStorageUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'platform': ParamAnnotation(required=False,valid_values=['Linux','Windows','Other','Paravirtualization','WindowsVirtio'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'system': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateRootVolumeTemplateFromVolumeSnapshotAction, self).__init__()
        self.snapshotUuid = None
        self.name = None
        self.description = None
        self.guestOsType = None
        self.backupStorageUuids = None
        self.platform = None
        self.system = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ReconnectBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/backup-storage/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'reconnectBackupStorage'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ReconnectBackupStorageAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetL3NetworkTypesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/l3-networks/types'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetL3NetworkTypesAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RemoveMonFromFusionstorBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/backup-storage/fusionstor/{uuid}/mons'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monHostnames': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RemoveMonFromFusionstorBackupStorageAction, self).__init__()
        self.uuid = None
        self.monHostnames = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateUserTagAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/user-tags'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'resourceType': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'tag': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateUserTagAction, self).__init__()
        self.resourceType = None
        self.resourceUuid = None
        self.tag = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddMonToCephPrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/primary-storage/ceph/{uuid}/mons'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monUrls': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddMonToCephPrimaryStorageAction, self).__init__()
        self.uuid = None
        self.monUrls = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVirtualRouterLocalAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/vrouter/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVirtualRouterLocalAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteBaremetalPxeServerAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/baremetal/pxeserver/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteBaremetalPxeServerAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateIPsecConnectionAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/ipsec'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'peerAddress': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'authMode': ParamAnnotation(required=False,valid_values=['psk','certs'],max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'authKey': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vipUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'peerCidrs': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'ikeAuthAlgorithm': ParamAnnotation(required=False,valid_values=['md5','sha1','sha256','sha384','sha512'],max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ikeEncryptionAlgorithm': ParamAnnotation(required=False,valid_values=['3des','aes-128','aes-192','aes-256'],max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ikeDhGroup': ParamAnnotation(),
        'policyAuthAlgorithm': ParamAnnotation(required=False,valid_values=['md5','sha1','sha256','sha384','sha512'],max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'policyEncryptionAlgorithm': ParamAnnotation(required=False,valid_values=['3des','aes-128','aes-192','aes-256'],max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'pfs': ParamAnnotation(required=False,valid_values=['dh-group2','dh-group5','dh-group14','dh-group15','dh-group16','dh-group17','dh-group18','dh-group19','dh-group20','dh-group21','dh-group22','dh-group23','dh-group24','dh-group25','dh-group26'],max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'policyMode': ParamAnnotation(required=False,valid_values=['tunnel','transport'],max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'transformProtocol': ParamAnnotation(required=False,valid_values=['esp','ah','ah-esp'],max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateIPsecConnectionAction, self).__init__()
        self.name = None
        self.description = None
        self.l3NetworkUuid = None
        self.peerAddress = None
        self.authMode = None
        self.authKey = None
        self.vipUuid = None
        self.peerCidrs = None
        self.ikeAuthAlgorithm = None
        self.ikeEncryptionAlgorithm = None
        self.ikeDhGroup = None
        self.policyAuthAlgorithm = None
        self.policyEncryptionAlgorithm = None
        self.pfs = None
        self.policyMode = None
        self.transformProtocol = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateStartVmInstanceSchedulerAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/vm-instances/{vmUuid}/schedulers/starting'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vmUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'clusterUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'hostUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'schedulerName': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'schedulerDescription': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(required=True,valid_values=['simple','cron'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'interval': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'repeatCount': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'startTime': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'cron': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateStartVmInstanceSchedulerAction, self).__init__()
        self.vmUuid = None
        self.clusterUuid = None
        self.hostUuid = None
        self.schedulerName = None
        self.schedulerDescription = None
        self.type = None
        self.interval = None
        self.repeatCount = None
        self.startTime = None
        self.cron = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeDiskOfferingStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/disk-offerings/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeDiskOfferingState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeDiskOfferingStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QuerySftpBackupStorageAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/backup-storage/sftp'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QuerySftpBackupStorageAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryQuotaAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/accounts/quotas'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryQuotaAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteDataCenterInLocalAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/data-center/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteDataCenterInLocalAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteOssFileBucketNameInLocalAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/oss-bucket/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteOssFileBucketNameInLocalAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmHostnameAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{uuid}/hostnames'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmHostnameAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachL3NetworkFromVmAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/vm-instances/nics/{vmNicUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'vmNicUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachL3NetworkFromVmAction, self).__init__()
        self.vmNicUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class StartBaremetalPxeServerAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/baremetal/pxeserver/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'startBaremetalPxeServer'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(StartBaremetalPxeServerAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryOssBucketFileNameAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/oss-bucket'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryOssBucketFileNameAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdatePrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/primary-storage/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updatePrimaryStorage'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdatePrimaryStorageAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.url = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncVirtualBorderRouterFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/border-router/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncVirtualBorderRouterFromRemote'

    PARAMS = {
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncVirtualBorderRouterFromRemoteAction, self).__init__()
        self.dataCenterUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SetVmSshKeyAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'setVmSshKey'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'SshKey': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SetVmSshKeyAction, self).__init__()
        self.uuid = None
        self.SshKey = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmInstanceHaLevelAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{uuid}/ha-levels'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmInstanceHaLevelAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeHostStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hosts/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeHostState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable','maintain'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeHostStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateBaremetalHostCfgAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/baremetal/hostcfg'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'chessisUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'vnc': ParamAnnotation(required=False,valid_values=['true','false'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'unattended': ParamAnnotation(required=False,valid_values=['true','false'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'nicCfgs': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateBaremetalHostCfgAction, self).__init__()
        self.chessisUuid = None
        self.password = None
        self.vnc = None
        self.unattended = None
        self.nicCfgs = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryImageStoreBackupStorageAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/backup-storage/image-store'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryImageStoreBackupStorageAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteRouterInterfaceLocalAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/router-interface/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteRouterInterfaceLocalAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryEcsSecurityGroupRuleFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/security-group-rule'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryEcsSecurityGroupRuleFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteLdapServerAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/ldap/servers/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteLdapServerAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateUserAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/accounts/users/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateUser'

    PARAMS = {
        'uuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateUserAction, self).__init__()
        self.uuid = None
        self.password = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteZoneAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/zones/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'zone'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteZoneAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeBackupStorageStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/backup-storage/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeBackupStorageState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeBackupStorageStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteAllEcsInstancesFromDataCenterAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/dc-ecs/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteAllEcsInstancesFromDataCenterAction, self).__init__()
        self.dataCenterUuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryConsoleProxyAgentAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/consoles/agents'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryConsoleProxyAgentAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteEcsInstanceAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/ecs/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteEcsInstanceAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ProvisionBaremetalHostAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/baremetal/chessis/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'provisionBaremetalHost'

    PARAMS = {
        'chessisUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ProvisionBaremetalHostAction, self).__init__()
        self.chessisUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class LogInByLdapAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/ldap/login'
    NEED_SESSION = False
    NEED_POLL = False
    PARAM_NAME = 'logInByLdap'

    PARAMS = {
        'uid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'password': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation()
    }

    def __init__(self):
        super(LogInByLdapAction, self).__init__()
        self.uid = None
        self.password = None
        self.systemTags = None
        self.userTags = None


class GetInterdependentL3NetworksImagesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/images-l3networks/dependencies'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuids': ParamAnnotation(required=False,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'imageUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetInterdependentL3NetworksImagesAction, self).__init__()
        self.zoneUuid = None
        self.l3NetworkUuids = None
        self.imageUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachPoliciesToUserAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/accounts/users/{userUuid}/policy-collection'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'userUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'policyUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachPoliciesToUserAction, self).__init__()
        self.userUuid = None
        self.policyUuids = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteBaremetalChessisAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/baremetal/chessis/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteBaremetalChessisAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryBackupStorageAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/backup-storage'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryBackupStorageAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryHostAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hosts'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryHostAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class PowerOffBaremetalHostAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/baremetal/chessis/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'powerOffBaremetalHost'

    PARAMS = {
        'chessisUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(PowerOffBaremetalHostAction, self).__init__()
        self.chessisUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVmSshKeyAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/vm-instances/{uuid}/ssh-keys'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'uuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVmSshKeyAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateFusionstorBackupStorageMonAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/backup-storage/fusionstor/mons/{monUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateFusionstorBackupStorageMon'

    PARAMS = {
        'monUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'hostname': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshUsername': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPassword': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateFusionstorBackupStorageMonAction, self).__init__()
        self.monUuid = None
        self.hostname = None
        self.sshUsername = None
        self.sshPassword = None
        self.sshPort = None
        self.monPort = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateBaremetalPxeServerAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/baremetal/pxeserver/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateBaremetalPxeServer'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dhcpInterface': ParamAnnotation(required=False,max_length=128,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dhcpRangeBegin': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dhcpRangeEnd': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dhcpRangeNetmask': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateBaremetalPxeServerAction, self).__init__()
        self.uuid = None
        self.dhcpInterface = None
        self.dhcpRangeBegin = None
        self.dhcpRangeEnd = None
        self.dhcpRangeNetmask = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachL2NetworkToClusterAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/l2-networks/{l2NetworkUuid}/clusters/{clusterUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'l2NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'clusterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachL2NetworkToClusterAction, self).__init__()
        self.l2NetworkUuid = None
        self.clusterUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddMonToCephBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/backup-storage/ceph/{uuid}/mons'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monUrls': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddMonToCephBackupStorageAction, self).__init__()
        self.uuid = None
        self.monUrls = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryBaremetalHostCfgAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/baremetal/hostcfg'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryBaremetalHostCfgAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class PrometheusQueryPassThroughAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/prometheus/all'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'instant': ParamAnnotation(),
        'startTime': ParamAnnotation(required=False,number_range=[0, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'endTime': ParamAnnotation(required=False,number_range=[0, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'step': ParamAnnotation(),
        'expression': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'relativeTime': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(PrometheusQueryPassThroughAction, self).__init__()
        self.instant = None
        self.startTime = None
        self.endTime = None
        self.step = None
        self.expression = None
        self.relativeTime = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/backup-storage/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteBackupStorageAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetCandidateVmNicForSecurityGroupAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/security-groups/{securityGroupUuid}/vm-instances/candidate-nics'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'securityGroupUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetCandidateVmNicForSecurityGroupAction, self).__init__()
        self.securityGroupUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetPrimaryStorageCapacityAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/primary-storage/capacities'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'zoneUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'clusterUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'primaryStorageUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'all': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetPrimaryStorageCapacityAction, self).__init__()
        self.zoneUuids = None
        self.clusterUuids = None
        self.primaryStorageUuids = None
        self.all = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class StopEcsInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/hybrid/aliyun/ecs/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'stopEcsInstance'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(StopEcsInstanceAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVmConsolePasswordAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/vm-instances/{uuid}/console-password'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVmConsolePasswordAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SetVmInstanceHaLevelAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/vm-instances/{uuid}/ha-levels'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'level': ParamAnnotation(required=True,valid_values=['NeverStop','OnHostFailure'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SetVmInstanceHaLevelAction, self).__init__()
        self.uuid = None
        self.level = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetLicenseInfoAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/licenses'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetLicenseInfoAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeImageStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/images/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeImageState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeImageStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateInstanceOfferingAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/instance-offerings'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'cpuNum': ParamAnnotation(required=True,number_range=[1, 1024],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'memorySize': ParamAnnotation(required=True,number_range=[1, 9223372036854775807],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'allocatorStrategy': ParamAnnotation(),
        'sortKey': ParamAnnotation(),
        'type': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateInstanceOfferingAction, self).__init__()
        self.name = None
        self.description = None
        self.cpuNum = None
        self.memorySize = None
        self.allocatorStrategy = None
        self.sortKey = None
        self.type = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryManagementNodeAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/management-nodes'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryManagementNodeAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateEipAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/eips/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateEip'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateEipAction, self).__init__()
        self.uuid = None
        self.name = None
        self.description = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryFusionstorPrimaryStorageAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/primary-storage/fusionstor'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryFusionstorPrimaryStorageAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeInstanceOfferingStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/instance-offerings/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeInstanceOfferingState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeInstanceOfferingStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetImageQgaAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/images/{uuid}/qga'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetImageQgaAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeSecurityGroupStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/security-groups/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeSecurityGroupState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeSecurityGroupStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangeInstanceOfferingAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{vmInstanceUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changeInstanceOffering'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'instanceOfferingUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangeInstanceOfferingAction, self).__init__()
        self.vmInstanceUuid = None
        self.instanceOfferingUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteDiskOfferingAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/disk-offerings/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteDiskOfferingAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateVniRangeAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/l2-networks/vxlan-pool/{l2NetworkUuid}/vni-ranges'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'startVni': ParamAnnotation(required=True,number_range=[0, 16777214],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'endVni': ParamAnnotation(required=True,number_range=[0, 16777214],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l2NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateVniRangeAction, self).__init__()
        self.name = None
        self.description = None
        self.startVni = None
        self.endVni = None
        self.l2NetworkUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryVolumeSnapshotAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/volume-snapshots'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryVolumeSnapshotAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class StopBaremetalPxeServerAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/baremetal/pxeserver/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'stopBaremetalPxeServer'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(StopBaremetalPxeServerAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryNotificationAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/notifications'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryNotificationAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CheckIpAvailabilityAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/l3-networks/{l3NetworkUuid}/ip/{ip}/availability'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ip': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CheckIpAvailabilityAction, self).__init__()
        self.l3NetworkUuid = None
        self.ip = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteLoadBalancerListenerAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/load-balancers/listeners/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteLoadBalancerListenerAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteIdentityZoneInLocalAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/identity-zone/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteIdentityZoneInLocalAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryIpRangeAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/l3-networks/ip-ranges'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryIpRangeAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetCandidateVmNicsForLoadBalancerAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/load-balancers/listeners/{listenerUuid}/vm-instances/candidate-nics'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'listenerUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetCandidateVmNicsForLoadBalancerAction, self).__init__()
        self.listenerUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryRouterInterfaceFromLocalAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/router-interface'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryRouterInterfaceFromLocalAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteBaremetalHostCfgAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/baremetal/hostcfg/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteBaremetalHostCfgAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class PowerOnBaremetalHostAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/baremetal/chessis/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'powerOnBaremetalHost'

    PARAMS = {
        'chessisUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(PowerOnBaremetalHostAction, self).__init__()
        self.chessisUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateRootVolumeTemplateFromRootVolumeAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/images/root-volume-templates/from/volumes/{rootVolumeUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'guestOsType': ParamAnnotation(),
        'backupStorageUuids': ParamAnnotation(required=False,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'rootVolumeUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'platform': ParamAnnotation(required=False,valid_values=['Linux','Windows','Other','Paravirtualization','WindowsVirtio'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'system': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateRootVolumeTemplateFromRootVolumeAction, self).__init__()
        self.name = None
        self.description = None
        self.guestOsType = None
        self.backupStorageUuids = None
        self.rootVolumeUuid = None
        self.platform = None
        self.system = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteImageAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/images/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'backupStorageUuids': ParamAnnotation(required=False,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteImageAction, self).__init__()
        self.uuid = None
        self.backupStorageUuids = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QuerySystemTagAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/system-tags'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QuerySystemTagAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteEcsSecurityGroupInLocalAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/security-group/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteEcsSecurityGroupInLocalAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateZoneAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/zones/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateZone'

    PARAMS = {
        'name': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateZoneAction, self).__init__()
        self.name = None
        self.description = None
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DestroyVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/vm-instances/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DestroyVmInstanceAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RecoverVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'recoverVmInstance'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RecoverVmInstanceAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncImageSizeAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/images/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncImageSize'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncImageSizeAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangePortForwardingRuleStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/port-forwarding/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changePortForwardingRuleState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangePortForwardingRuleStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateLoadBalancerListenerAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/load-balancers/{loadBalancerUuid}/listeners'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'loadBalancerUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'instancePort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'loadBalancerPort': ParamAnnotation(required=True,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'protocol': ParamAnnotation(required=False,valid_values=['tcp','http'],max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateLoadBalancerListenerAction, self).__init__()
        self.loadBalancerUuid = None
        self.name = None
        self.description = None
        self.instancePort = None
        self.loadBalancerPort = None
        self.protocol = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateL2VlanNetworkAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/l2-networks/vlan'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vlan': ParamAnnotation(required=True,number_range=[1, 4094],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'zoneUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'physicalInterface': ParamAnnotation(required=True,max_length=1024,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateL2VlanNetworkAction, self).__init__()
        self.vlan = None
        self.name = None
        self.description = None
        self.zoneUuid = None
        self.physicalInterface = None
        self.type = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetNetworkServiceTypesAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/network-services/types'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetNetworkServiceTypesAction, self).__init__()
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QuerySharedResourceAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/accounts/resources'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QuerySharedResourceAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteUserAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/accounts/users/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteUserAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddOssFileBucketNameAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/oss-bucket'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'ossBucketName': ParamAnnotation(required=True,max_length=32,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'regionId': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddOssFileBucketNameAction, self).__init__()
        self.ossBucketName = None
        self.regionId = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetFreeIpAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = 'null'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'params'

    PARAMS = {
        'l3NetworkUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ipRangeUuid': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'start': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetFreeIpAction, self).__init__()
        self.l3NetworkUuid = None
        self.ipRangeUuid = None
        self.start = None
        self.limit = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateFusionstorPrimaryStorageMonAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/primary-storage/fusionstor/mons/{monUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateFusionstorPrimaryStorageMon'

    PARAMS = {
        'monUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'hostname': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshUsername': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPassword': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'sshPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'monPort': ParamAnnotation(required=False,number_range=[1, 65535],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateFusionstorPrimaryStorageMonAction, self).__init__()
        self.monUuid = None
        self.hostname = None
        self.sshUsername = None
        self.sshPassword = None
        self.sshPort = None
        self.monPort = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetNicQosAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{uuid}/nic-qos'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetNicQosAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class GetVmConsoleAddressAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/vm-instances/{uuid}/console-addresses'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = 'null'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(GetVmConsoleAddressAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ReimageVmInstanceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{vmInstanceUuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'reimageVmInstance'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ReimageVmInstanceAction, self).__init__()
        self.vmInstanceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SyncRouteEntryFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'GET'
    PATH = '/hybrid/aliyun/route-entry/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'syncRouteEntryFromRemote'

    PARAMS = {
        'vRouterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vRouterType': ParamAnnotation(required=True,valid_values=['vbr','vrouter'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SyncRouteEntryFromRemoteAction, self).__init__()
        self.vRouterUuid = None
        self.vRouterType = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteEcsSecurityGroupRemoteAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/security-group/remote/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteEcsSecurityGroupRemoteAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryLdapServerAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/ldap/servers'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryLdapServerAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DetachOssBucketToEcsDataCenterAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/{dataCenterUuid}/oss-bucket/{ossBucketUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'ossBucketUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DetachOssBucketToEcsDataCenterAction, self).__init__()
        self.ossBucketUuid = None
        self.dataCenterUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class UpdateGlobalConfigAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/global-configurations/{category}/{name}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'updateGlobalConfig'

    PARAMS = {
        'category': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'value': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(UpdateGlobalConfigAction, self).__init__()
        self.category = None
        self.name = None
        self.value = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteGCJobAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/gc-jobs/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteGCJobAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddConnectionAccessPointFromRemoteAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/hybrid/aliyun/access-point'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'dataCenterUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddConnectionAccessPointFromRemoteAction, self).__init__()
        self.dataCenterUuid = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class SetVmConsolePasswordAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/vm-instances/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'setVmConsolePassword'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'consolePassword': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(SetVmConsolePasswordAction, self).__init__()
        self.uuid = None
        self.consolePassword = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVolumeQosAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/volumes/{uuid}/qos'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVolumeQosAction, self).__init__()
        self.uuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class RequestConsoleAccessAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/consoles'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'vmInstanceUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(RequestConsoleAccessAction, self).__init__()
        self.vmInstanceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ShareResourceAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/accounts/resources/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'shareResource'

    PARAMS = {
        'resourceUuids': ParamAnnotation(required=True,non_empty=True,null_elements=False,empty_string=True,no_trim=False),
        'accountUuids': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'toPublic': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ShareResourceAction, self).__init__()
        self.resourceUuids = None
        self.accountUuids = None
        self.toPublic = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryIPSecConnectionAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/ipsec'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryIPSecConnectionAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeletePrimaryStorageAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/primary-storage/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'null'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeletePrimaryStorageAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AttachPortForwardingRuleAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/port-forwarding/{ruleUuid}/vm-instances/nics/{vmNicUuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'ruleUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'vmNicUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AttachPortForwardingRuleAction, self).__init__()
        self.ruleUuid = None
        self.vmNicUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryGCJobAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/gc-jobs'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryGCJobAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteEcsImageLocalAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/hybrid/aliyun/image/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteEcsImageLocalAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddSimulatorBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/backup-storage/simulators'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'totalCapacity': ParamAnnotation(),
        'availableCapacity': ParamAnnotation(),
        'url': ParamAnnotation(required=True,max_length=2048,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'importImages': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddSimulatorBackupStorageAction, self).__init__()
        self.totalCapacity = None
        self.availableCapacity = None
        self.url = None
        self.name = None
        self.description = None
        self.type = None
        self.importImages = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class QueryNetworkServiceProviderAction(QueryAction):
    HTTP_METHOD = 'GET'
    PATH = '/network-services/providers'
    NEED_SESSION = True
    NEED_POLL = False
    PARAM_NAME = ''

    PARAMS = {
        'conditions': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'limit': ParamAnnotation(),
        'start': ParamAnnotation(),
        'count': ParamAnnotation(),
        'groupBy': ParamAnnotation(),
        'replyWithCount': ParamAnnotation(),
        'sortBy': ParamAnnotation(),
        'sortDirection': ParamAnnotation(required=False,valid_values=['asc','desc'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'fields': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(QueryNetworkServiceProviderAction, self).__init__()
        self.conditions = None
        self.limit = None
        self.start = None
        self.count = None
        self.groupBy = None
        self.replyWithCount = None
        self.sortBy = None
        self.sortDirection = None
        self.fields = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddIpRangeByNetworkCidrAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/l3-networks/{l3NetworkUuid}/ip-ranges/by-cidr'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'l3NetworkUuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'networkCidr': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddIpRangeByNetworkCidrAction, self).__init__()
        self.name = None
        self.description = None
        self.l3NetworkUuid = None
        self.networkCidr = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class AddCephBackupStorageAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/backup-storage/ceph'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'monUrls': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=False,no_trim=False),
        'poolName': ParamAnnotation(required=False,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'url': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'type': ParamAnnotation(),
        'importImages': ParamAnnotation(required=False,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(AddCephBackupStorageAction, self).__init__()
        self.monUrls = None
        self.poolName = None
        self.url = None
        self.name = None
        self.description = None
        self.type = None
        self.importImages = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateBaremetalChessisAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/baremetal/chessis'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'ipmiAddress': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ipmiUsername': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'ipmiPassword': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateBaremetalChessisAction, self).__init__()
        self.ipmiAddress = None
        self.ipmiUsername = None
        self.ipmiPassword = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteL2NetworkAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/l2-networks/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = ''

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteL2NetworkAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class CreateZoneAction(AbstractAction):
    HTTP_METHOD = 'POST'
    PATH = '/zones'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'name': ParamAnnotation(required=True,max_length=255,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'description': ParamAnnotation(required=False,max_length=2048,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'resourceUuid': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(CreateZoneAction, self).__init__()
        self.name = None
        self.description = None
        self.resourceUuid = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class DeleteVniRangeAction(AbstractAction):
    HTTP_METHOD = 'DELETE'
    PATH = '/l2-networks/vxlan-pool/vni-ranges/{uuid}'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'params'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'deleteMode': ParamAnnotation(),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(DeleteVniRangeAction, self).__init__()
        self.uuid = None
        self.deleteMode = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None


class ChangePrimaryStorageStateAction(AbstractAction):
    HTTP_METHOD = 'PUT'
    PATH = '/primary-storage/{uuid}/actions'
    NEED_SESSION = True
    NEED_POLL = True
    PARAM_NAME = 'changePrimaryStorageState'

    PARAMS = {
        'uuid': ParamAnnotation(required=True,non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'stateEvent': ParamAnnotation(required=True,valid_values=['enable','disable','maintain','deleting'],non_empty=False,null_elements=False,empty_string=True,no_trim=False),
        'systemTags': ParamAnnotation(),
        'userTags': ParamAnnotation(),
        'sessionId': ParamAnnotation(required=True)
    }

    def __init__(self):
        super(ChangePrimaryStorageStateAction, self).__init__()
        self.uuid = None
        self.stateEvent = None
        self.systemTags = None
        self.userTags = None
        self.sessionId = None
