import hashlib
import io
import os
import shutil
import struct
import subprocess
import tarfile
import tempfile
import unittest
import zipfile

import yaml

import zpkg_tool

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
ALLOWLIST = os.path.join(HERE, 'allowlist-v1.txt')


def make_class(name, strings=(), supers=()):
    """A class file with just enough of a constant pool for zpkg_tool to read; supers: the super class, then the
    interfaces."""
    pool = []

    def utf8(text):
        data = text.replace('.', '/').encode('utf-8')
        pool.append(b'\x01' + struct.pack('>H', len(data)) + data)
        return len(pool)

    def cls(text):
        index = utf8(text)
        pool.append(b'\x07' + struct.pack('>H', index))
        return len(pool)
    this = cls(name)
    refs = [cls(n) for n in supers]
    for s in strings:
        data = s.encode('utf-8')
        pool.append(b'\x01' + struct.pack('>H', len(data)) + data)
    head = b'\xca\xfe\xba\xbe' + struct.pack('>HHH', 0, 52, len(pool) + 1)
    interfaces = refs[1:]
    return head + b''.join(pool) + struct.pack('>HHH', 0x21, this, refs[0] if refs else 0) + \
        struct.pack('>H', len(interfaces)) + b''.join(struct.pack('>H', i) for i in interfaces) + b'\x00' * 6


class CheckCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.platform = os.path.join(self.tmp, 'platform-lib')
        os.makedirs(self.platform)
        for jar in ('header-5.5.0.jar', 'compute-5.5.0.jar', 'gson-2.8.5.jar'):
            open(os.path.join(self.platform, jar), 'w').close()

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def run_check(self, refs=(), classes=None, desc=None, plugin_jars=(), global_configs=None, supers=None,
                  class_files=None):
        stage = os.path.join(self.tmp, 'stage')
        os.makedirs(os.path.join(stage, 'lib'))
        d = {'id': 'tpm', 'version': '1.0.0', 'requires': {'sdk': '>=1.0 <2'}, 'mn': {'entities': []}}
        d.update(desc or {})
        with open(os.path.join(stage, 'plugin.yaml'), 'w') as f:
            yaml.safe_dump(d, f)
        with zipfile.ZipFile(os.path.join(stage, 'lib', 'tpm-1.0.0.jar'), 'w') as jar:
            for name, strings in (classes or {'org.zstack.tpm.TpmManager': ()}).items():
                jar.writestr(name.replace('.', '/') + '.class', make_class(name, strings, (supers or {}).get(name, ())))
            for name, data in (class_files or {}).items():
                jar.writestr(name.replace('.', '/') + '.class', data)
        for j in plugin_jars:
            open(os.path.join(stage, 'lib', j), 'w').close()
        for name, content in (global_configs or {}).items():
            os.makedirs(os.path.join(stage, 'classes', 'globalConfig'), exist_ok=True)
            with open(os.path.join(stage, 'classes', 'globalConfig', name), 'w') as f:
                f.write(content)
        jdeps = os.path.join(self.tmp, 'jdeps.txt')
        with open(jdeps, 'w') as f:
            f.write('tpm-1.0.0.jar -> %s/header-5.5.0.jar\n   org.zstack.tpm.TpmManager (tpm-1.0.0.jar)\n' % self.platform)
            f.write('      -> java.lang.Object\n')
            for r in refs:
                jar = 'compute-5.5.0.jar' if r.startswith('org.zstack.compute') else 'header-5.5.0.jar'
                f.write('      -> %s    %s\n' % (r, jar))
        return zpkg_tool.check(stage, self.platform, ALLOWLIST, jdeps)


class TestCheck(CheckCase):
    def test_disallowed_reference(self):
        self.assertIn('disallowed-reference: org.zstack.compute.vm.VmInstanceBase',
                      self.run_check(refs=['org.zstack.compute.vm.VmInstanceBase']))

    def test_allowed_inventory_reference(self):
        self.assertEqual([], self.run_check(refs=['org.zstack.header.vm.VmInstanceInventory']))

    def test_deny_overrides_allow(self):
        self.assertIn('disallowed-reference: org.zstack.header.vm.VmInstanceSpec',
                      self.run_check(refs=['org.zstack.header.vm.VmInstanceSpec']))
        allow, deny = ['org.zstack.header.vm.*'], ['org.zstack.header.vm.VmInstanceSpec']
        self.assertFalse(zpkg_tool.allowed('org.zstack.header.vm.VmInstanceSpec', allow, deny))
        self.assertTrue(zpkg_tool.allowed('org.zstack.header.vm.VmNicInventory', allow, deny))

    def test_calling_kvmagent_is_allowed(self):
        # what javac emits for sending KVMHostAsyncHttpCallMsg to a host with a callback and an AgentResponse subclass
        refs = ['org.zstack.core.cloudbus.CloudBusCallBack', 'org.zstack.header.core.AsyncBackup',
                'org.zstack.header.host.HostConstant', 'org.zstack.kvm.KVMHostAsyncHttpCallMsg',
                'org.zstack.kvm.KVMAgentCommands', 'org.zstack.kvm.KVMAgentCommands$AgentCommand',
                'org.zstack.kvm.KVMAgentCommands$AgentResponse']
        self.assertEqual([], self.run_check(refs=refs))

    def test_other_agent_commands_stay_disallowed(self):
        self.assertIn('disallowed-reference: org.zstack.kvm.KVMAgentCommands$AttachNicCommand',
                      self.run_check(refs=['org.zstack.kvm.KVMAgentCommands$AttachNicCommand']))

    def test_reading_hosts_is_allowed(self):
        # what javac emits for checking a host with Q/HostVO_ and a foreign key to HostEO
        refs = ['org.zstack.header.host.HostVO', 'org.zstack.header.host.HostVO_',
                'org.zstack.header.host.HostEO', 'org.zstack.header.host.HostStatus']
        self.assertEqual([], self.run_check(refs=refs))

    def test_reading_hosts_woven_is_allowed(self):
        # the AspectJ compiler of the template names the declaring metamodel of HostVO_.uuid, HostAO_
        self.assertEqual([], self.run_check(refs=['org.zstack.header.host.HostAO_']))

    def test_host_base_class_stays_disallowed(self):
        self.assertEqual(['disallowed-reference: org.zstack.header.host.HostAO'],
                         self.run_check(refs=['org.zstack.header.host.HostAO']))

    def test_hooks_the_platform_never_calls_on_a_plugin_are_refused(self):
        # spec N1, C8 and S1: their consumers never reach a plugin's implementation
        hooks = ['org.zstack.header.network.l2.L2NetworkRealizationExtensionPoint',
                 'org.zstack.header.vm.AfterReleaseVmNicExtensionPoint',
                 'org.zstack.header.storage.addon.primary.PrimaryStorageControllerSvc',
                 'org.zstack.header.storage.addon.primary.PrimaryStorageNodeSvc']
        self.assertEqual(sorted('disallowed-reference: ' + h for h in hooks), sorted(self.run_check(refs=hooks)))

    def test_jdk_reference_is_not_checked(self):
        self.assertEqual([], self.run_check())

    def test_error_code_prefix(self):
        classes = {'org.zstack.tpm.TpmErrors': ('ORG_ZSTACK_PLUGIN_TPM_10000', 'ORG_ZSTACK_CORE_PLUGIN_10002')}
        self.assertEqual(['error-code-prefix: ORG_ZSTACK_CORE_PLUGIN_10002'], self.run_check(classes=classes))

    def test_error_code_prefix_of_hyphenated_id(self):
        classes = {'org.zstack.fm.Errors': ('ORG_ZSTACK_PLUGIN_FLOW_METER_10000',)}
        self.assertEqual([], self.run_check(classes=classes, desc={'id': 'flow-meter'}))

    def test_config_category(self):
        xml = ('<globalConfig xmlns="http://zstack.org/schema/zstack"><config><category>tpm</category></config>'
               '<config><category>vm</category></config></globalConfig>')
        self.assertEqual(['config-category: tpm.xml: vm'], self.run_check(global_configs={'tpm.xml': xml}))

    def test_undeclared_entity(self):
        classes = {'org.zstack.tpm.VmTpmVO': ('Ljavax/persistence/Entity;',),
                   'org.zstack.tpm.TpmKeyVO': ('Ljavax/persistence/Entity;',)}
        problems = self.run_check(classes=classes, desc={'mn': {'entities': ['org.zstack.tpm.VmTpmVO']}})
        self.assertEqual(['undeclared-entity: org.zstack.tpm.TpmKeyVO'], problems)

    def test_entity_named_like_a_platform_entity_is_refused(self):
        # JPQL names entities by their simple names, a feature plugin's must not take the platform's
        with zipfile.ZipFile(os.path.join(self.platform, 'header-5.5.0.jar'), 'w') as jar:
            jar.writestr('org/zstack/header/vm/VmInstanceVO.class',
                         make_class('org.zstack.header.vm.VmInstanceVO', (zpkg_tool.ENTITY,)))
        classes = {'org.zstack.tpm.VmInstanceVO': (zpkg_tool.ENTITY,), 'org.zstack.tpm.TpmKeyVO': (zpkg_tool.ENTITY,)}
        problems = self.run_check(classes=classes, desc={'mn': {'entities': sorted(classes)}})
        self.assertEqual(['entity-name-taken: org.zstack.tpm.VmInstanceVO'], problems)

    def test_query_api_is_refused_in_v1(self):
        # the platform registers query APIs at start only until P2b (spec 2.5)
        classes = {'org.zstack.tpm.APIQueryTpmMsg': (), 'org.zstack.tpm.APIQueryTpmKeyMsg': ()}
        supers = {'org.zstack.tpm.APIQueryTpmMsg': ['org.zstack.header.query.APIQueryMessage'],
                  'org.zstack.tpm.APIQueryTpmKeyMsg': ['org.zstack.tpm.APIQueryTpmMsg']}
        self.assertEqual(['v1-unsupported: org.zstack.tpm.APIQueryTpmKeyMsg (query API)',
                          'v1-unsupported: org.zstack.tpm.APIQueryTpmMsg (query API)'],
                         self.run_check(classes=classes, supers=supers))

    def test_other_runtime_registrations_are_refused_in_v1(self):
        classes = {'org.zstack.tpm.TpmInventory': ('Lorg/zstack/header/search/Inventory;',),
                   'org.zstack.tpm.TpmVO': (zpkg_tool.ENTITY,),
                   'org.zstack.tpm.TpmJob': ('Lorg/zstack/header/longjob/LongJobFor;',),
                   'org.zstack.tpm.TpmNamespace': (),
                   'org.zstack.tpm.TpmMetrics': ()}
        supers = {'org.zstack.tpm.TpmVO': ['org.zstack.header.vo.ResourceVO'],
                  'org.zstack.tpm.TpmNamespace': ['org.zstack.zwatch.namespace.AbstractNamespace'],
                  'org.zstack.tpm.TpmMetrics': ['java.lang.Object', 'org.zstack.zwatch.datatype.Namespace']}
        problems = self.run_check(classes=classes, supers=supers, desc={'mn': {'entities': ['org.zstack.tpm.TpmVO']}})
        self.assertEqual(['v1-unsupported: org.zstack.tpm.TpmInventory (inventory)',
                          'v1-unsupported: org.zstack.tpm.TpmJob (long job)',
                          'v1-unsupported: org.zstack.tpm.TpmMetrics (zwatch namespace)',
                          'v1-unsupported: org.zstack.tpm.TpmNamespace (zwatch namespace)',
                          'v1-unsupported: org.zstack.tpm.TpmVO (resource)'], problems)

    def test_system_tag_definitions_are_refused_in_v1(self):
        # the platform reads @TagDefinition classes at start only until P2b
        classes = {'org.zstack.tpm.TpmSystemTags': ('Lorg/zstack/header/tag/TagDefinition;',)}
        self.assertEqual(['v1-unsupported: org.zstack.tpm.TpmSystemTags (system tag)'], self.run_check(classes=classes))

    def test_write_api_paths_must_start_with_the_plugin_id(self):
        # the upgrade gate holds and the UI channel lets through only writes under /<plugin id>/
        javac = shutil.which('javac')
        if not javac:
            self.skipTest('javac is not on PATH')
        src = os.path.join(self.tmp, 'src')
        sources = {
            'org/zstack/header/rest/RestRequest.java':
                'package org.zstack.header.rest;\n'
                '@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)\n'
                'public @interface RestRequest { String path(); String[] optionalPaths() default {};\n'
                '    org.springframework.http.HttpMethod method(); Class responseClass(); }\n',
            'org/springframework/http/HttpMethod.java':
                'package org.springframework.http;\n'
                'public enum HttpMethod { GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE }\n',
        }
        apis = {'APICreateTpmKeyMsg': 'path = "/tpm/keys", method = HttpMethod.POST',
                'APIAttachTpmMsg': 'path = "/hosts/{uuid}/tpm", method = HttpMethod.PUT',
                'APIGetHostTpmMsg': 'path = "/hosts/{uuid}/tpm", method = HttpMethod.GET',
                'APIDeleteTpmKeyMsg': 'path = "/tpm/keys/{uuid}", optionalPaths = {"/keys/{uuid}"}, method = HttpMethod.DELETE'}
        for name, args in apis.items():
            sources['org/zstack/tpm/%s.java' % name] = (
                'package org.zstack.tpm;\nimport org.springframework.http.HttpMethod;\n'
                'import org.zstack.header.rest.RestRequest;\n'
                '@RestRequest(%s, responseClass = Object.class)\n'
                'public class %s { @Deprecated private String uuid; public String getUuid() { return uuid; } }\n' % (args, name))
        for path, text in sources.items():
            os.makedirs(os.path.dirname(os.path.join(src, path)), exist_ok=True)
            with open(os.path.join(src, path), 'w') as f:
                f.write(text)
        out = os.path.join(self.tmp, 'classes')
        os.makedirs(out)
        subprocess.check_call([javac, '-d', out] + [os.path.join(src, p) for p in sources])
        class_files = {}
        for name in apis:
            with open(os.path.join(out, 'org', 'zstack', 'tpm', name + '.class'), 'rb') as f:
                class_files['org.zstack.tpm.' + name] = f.read()
        self.assertEqual(['write-path-prefix: org.zstack.tpm.APIAttachTpmMsg PUT /hosts/{uuid}/tpm',
                          'write-path-prefix: org.zstack.tpm.APIDeleteTpmKeyMsg DELETE /keys/{uuid}'],
                         self.run_check(class_files=class_files))

    def test_check_package_without_lib(self):
        stage = os.path.join(self.tmp, 'drivers-only')
        os.makedirs(os.path.join(stage, 'drivers'))
        with open(os.path.join(stage, 'plugin.yaml'), 'w') as f:
            yaml.safe_dump({'id': 'hsm-test', 'version': '1.0.0', 'drivers': ['drivers/hsm.jar']}, f)
        with zipfile.ZipFile(os.path.join(stage, 'drivers', 'hsm.jar'), 'w') as jar:
            jar.writestr('org/zstack/hsm/Driver.class', make_class('org.zstack.hsm.Driver'))
        jdeps = os.path.join(self.tmp, 'jdeps.txt')
        with open(jdeps, 'w') as f:
            f.write('      -> org.zstack.abstraction.crypto.CryptoClientDriver    header-5.5.0.jar\n')
        self.assertEqual([], zpkg_tool.check(stage, self.platform, ALLOWLIST, jdeps))

    def test_jar_conflict(self):
        self.assertIn('jar-conflict: gson-2.8.9.jar', self.run_check(plugin_jars=['gson-2.8.9.jar']))


class TestAgentCheck(CheckCase):
    def agent_stage(self, files):
        stage = os.path.join(self.tmp, 'agent-stage')
        os.makedirs(os.path.join(stage, 'agent', 'kvmagent'))
        with open(os.path.join(stage, 'plugin.yaml'), 'w') as f:
            yaml.safe_dump({'id': 'tpm', 'version': '1.0.0', 'requires': {'sdk': '>=1.0 <2'},
                            'kvmagent': {'plugins': 'agent/kvmagent/'}}, f)
        for name, content in files.items():
            with open(os.path.join(stage, 'agent', 'kvmagent', name), 'w') as f:
                f.write(content)
        jdeps = os.path.join(self.tmp, 'jdeps.txt')
        open(jdeps, 'w').close()
        return zpkg_tool.check(stage, self.platform, ALLOWLIST, jdeps)

    def test_disallowed_agent_import(self):
        self.assertEqual(['disallowed-agent-import: kvmagent.plugins.vm_plugin in agent/kvmagent/tpm.py'],
                         self.agent_stage({'tpm.py': 'from kvmagent.plugins import vm_plugin\n'}))

    def test_agent_part_over_10_mb_is_refused(self):
        # every host gets the agent part in one command
        problems = self.agent_stage({'blob.py': '# ' + os.urandom(12 * 1024 * 1024).hex() + '\n'})
        self.assertEqual(1, len(problems))
        self.assertTrue(problems[0].startswith('agent-too-large: '), problems)

    def test_allowed_agent_imports(self):
        self.assertEqual([], self.agent_stage({
            'tpm.py': 'import os\nfrom zstacklib.utils import http, jsonobject\nfrom kvmagent import kvmagent, sdk\n'
                      'from . import helpers as h\n',
            'helpers.py': 'import json\n'}))

    def test_own_module_must_be_imported_relatively(self):
        # the loader gives each plugin a package of its own, absolute imports of its modules do not resolve
        self.assertEqual(['disallowed-agent-import: helpers in agent/kvmagent/tpm.py'],
                         self.agent_stage({'tpm.py': 'import helpers\n', 'helpers.py': ''}))


UI_PART = {'remote': 'ui/remote/', 'routes': 'ui/routes.json', 'menus': 'ui/menus.json', 'slots': 'ui/slots.json',
           'i18n': 'ui/locale/'}


class TestUiCheck(CheckCase):
    def ui_stage(self, ui=None, files=None):
        stage = os.path.join(self.tmp, 'ui-stage')
        with open(os.path.join(self.tmp, 'plugin.yaml'), 'w') as f:
            yaml.safe_dump({'id': 'tpm', 'version': '1.0.0', 'requires': {'sdk': '>=1.0 <2'},
                            'ui': ui if ui is not None else dict(UI_PART)}, f)
        content = {
            'ui/remote/mf-manifest.json': '{}',
            'ui/routes.json': '[{"path": "/plugin/tpm/x", "module": "./X"}]',
            'ui/menus.json': '[{"key": "tpm.x", "parent": "network.service", "title": "tpm.menu", "route": "/plugin/tpm/x", "order": 1}]',
            'ui/slots.json': '[{"slot": "detail.tab", "resource": "Host", "module": "./Tab", "title": "tpm.tab"}]',
            'ui/locale/zh-CN.json': '{"tpm.menu": "x", "tpm.tab": "y"}',
            'ui/locale/en-US.json': '{"tpm.menu": "x", "tpm.tab": "y"}',
        }
        content.update(files or {})
        for name, text in content.items():
            if text is None:
                continue
            path = os.path.join(stage, *name.split('/'))
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, 'w') as f:
                f.write(text)
        shutil.copy(os.path.join(self.tmp, 'plugin.yaml'), stage)
        jdeps = os.path.join(self.tmp, 'jdeps.txt')
        open(jdeps, 'w').close()
        return zpkg_tool.check(stage, self.platform, ALLOWLIST, jdeps)

    def test_valid_ui_part(self):
        self.assertEqual([], self.ui_stage())

    def test_privileges_rejected(self):
        self.assertIn('ui-field: privileges', self.ui_stage(ui=dict(UI_PART, privileges='ui/privileges.json'),
                                                          files={'ui/privileges.json': '[]'}))

    def test_nonstandard_path_rejected(self):
        self.assertIn('ui-field: routes', self.ui_stage(ui=dict(UI_PART, routes='ui/r.json'), files={'ui/r.json': '[]'}))

    def test_route_prefix(self):
        self.assertIn('ui-route: /tpm/x', self.ui_stage(files={
            'ui/routes.json': '[{"path": "/tpm/x", "module": "./X"}]',
            'ui/menus.json': '[]'}))

    def test_menu_key_prefix(self):
        self.assertIn('ui-menu: x.tasks', self.ui_stage(files={
            'ui/menus.json': '[{"key": "x.tasks", "parent": "network.service", "title": "tpm.menu", "route": "/plugin/tpm/x", "order": 1}]'}))

    def test_menu_route_must_exist(self):
        self.assertIn('ui-menu: tpm.x', self.ui_stage(files={
            'ui/menus.json': '[{"key": "tpm.x", "parent": "network.service", "title": "tpm.menu", "route": "/plugin/tpm/y", "order": 1}]'}))

    def test_unsupported_slot(self):
        self.assertIn('ui-slot: detail.tab VmInstance', self.ui_stage(files={
            'ui/slots.json': '[{"slot": "detail.tab", "resource": "VmInstance", "module": "./Tab", "title": "tpm.tab"}]'}))

    def test_locales_must_match(self):
        self.assertIn('ui-i18n: tpm.a', self.ui_stage(files={
            'ui/locale/zh-CN.json': '{"tpm.menu": "x", "tpm.tab": "y", "tpm.a": "z"}'}))

    def test_locale_keys_must_be_prefixed(self):
        self.assertIn('ui-i18n: menu', self.ui_stage(files={
            'ui/locale/zh-CN.json': '{"tpm.menu": "x", "tpm.tab": "y", "menu": "z"}',
            'ui/locale/en-US.json': '{"tpm.menu": "x", "tpm.tab": "y", "menu": "z"}'}))

    def test_missing_manifest(self):
        self.assertIn('ui-missing: ui/remote/mf-manifest.json', self.ui_stage(files={'ui/remote/mf-manifest.json': None}))

    def test_ui_files_need_a_declared_ui_part(self):
        stage = os.path.join(self.tmp, 'undeclared-ui')
        os.makedirs(os.path.join(stage, 'ui', 'remote'))
        open(os.path.join(stage, 'ui', 'remote', 'mf-manifest.json'), 'w').close()
        self.assertEqual(['ui-field: ui'], zpkg_tool.ui_problems(stage, {'id': 'tpm'}))

    def test_ui_bff_is_refused(self):
        # plugin code does not run in the BFF, which all users share and which cannot unload it
        self.assertEqual(['ui-bff: ui/bff/'], self.ui_stage(files={'ui/bff/index.js': ''}))
        self.assertIn('ui-field: bff', self.ui_stage(ui=dict(UI_PART, bff='ui/bff/')))


class TestAllowlist(unittest.TestCase):
    def test_allowlist_entries_exist(self):
        allow, deny = zpkg_tool.load_allowlist(ALLOWLIST)
        sources = set()
        for root, _, files in os.walk(REPO):
            if '/src/main/java/' in root + '/':
                for f in files:
                    if f.endswith('.java') or f.endswith('.groovy'):
                        rel = os.path.join(root, f).split('/src/main/java/', 1)[1]
                        sources.add(rel.rsplit('.', 1)[0].replace('/', '.'))
        missing = [p for p in allow + deny if '*' not in p and p.split('$')[0] not in sources]
        self.assertEqual([], missing)


class TestPack(CheckCase):
    def make_stage(self):
        stage = os.path.join(self.tmp, 'stage')
        os.makedirs(os.path.join(stage, 'lib'))
        os.makedirs(os.path.join(stage, 'classes', 'serviceConfig'))
        with open(os.path.join(stage, 'plugin.yaml'), 'w') as f:
            yaml.safe_dump({'id': 'tpm', 'version': '1.0.0', 'requires': {'sdk': '>=1.0 <2'}}, f)
        with open(os.path.join(stage, 'lib', 'tpm-1.0.0.jar'), 'w') as f:
            f.write('jar')
        with open(os.path.join(stage, 'classes', 'serviceConfig', 'tpm.xml'), 'w') as f:
            f.write('<service/>')
        with open(os.path.join(stage, 'README'), 'w') as f:
            f.write('not packed')
        return stage

    def test_pack_manifest_matches_ctl_rule(self):
        pkg = zpkg_tool.pack(self.make_stage(), self.tmp)
        self.assertEqual('tpm-1.0.0.zpkg', os.path.basename(pkg))
        with tarfile.open(pkg, 'r:gz') as tar:
            names = sorted(m.name for m in tar.getmembers() if m.isfile())
            manifest = tar.extractfile(zpkg_tool.MANIFEST).read().decode('utf-8').splitlines()
            expected = ['%s  %s' % (hashlib.sha256(tar.extractfile(n).read()).hexdigest(), n)
                        for n in names if n != zpkg_tool.MANIFEST]
        self.assertEqual(['META-INF/MANIFEST.sha256', 'classes/serviceConfig/tpm.xml', 'lib/tpm-1.0.0.jar', 'plugin.yaml'], names)
        self.assertEqual(expected, manifest)

    def test_pack_includes_agent_files(self):
        stage = self.make_stage()
        os.makedirs(os.path.join(stage, 'agent', 'kvmagent'))
        with open(os.path.join(stage, 'agent', 'kvmagent', 'a.py'), 'w') as f:
            f.write('import os\n')
        pkg = zpkg_tool.pack(stage, self.tmp)
        with tarfile.open(pkg, 'r:gz') as tar:
            manifest = tar.extractfile(zpkg_tool.MANIFEST).read().decode('utf-8')
        self.assertIn('  agent/kvmagent/a.py\n', manifest)

    def test_pack_includes_ui_files(self):
        stage = self.make_stage()
        os.makedirs(os.path.join(stage, 'ui', 'remote'))
        for name in ('ui/remote/mf-manifest.json', 'ui/routes.json'):
            with open(os.path.join(stage, *name.split('/')), 'w') as f:
                f.write('{}')
        pkg = zpkg_tool.pack(stage, self.tmp)
        with tarfile.open(pkg, 'r:gz') as tar:
            names = [m.name for m in tar.getmembers() if m.isfile()]
            manifest = tar.extractfile(zpkg_tool.MANIFEST).read().decode('utf-8')
        for name in ('ui/remote/mf-manifest.json', 'ui/routes.json'):
            self.assertIn(name, names)
            self.assertIn('  %s\n' % name, manifest)

    def test_sign_then_openssl_verifies(self):
        key, pub = os.path.join(self.tmp, 'key.pem'), os.path.join(self.tmp, 'pub.pem')
        with open(os.devnull, 'w') as null:
            subprocess.check_call(['openssl', 'genrsa', '-out', key, '2048'], stdout=null, stderr=null)
            subprocess.check_call(['openssl', 'rsa', '-in', key, '-pubout', '-out', pub], stdout=null, stderr=null)
        pkg = zpkg_tool.pack(self.make_stage(), self.tmp)
        zpkg_tool.sign(pkg, key)
        with tarfile.open(pkg, 'r:gz') as tar:
            for name in (zpkg_tool.MANIFEST, zpkg_tool.SIGNATURE):
                with open(os.path.join(self.tmp, os.path.basename(name)), 'wb') as f:
                    f.write(tar.extractfile(name).read())
        self.assertEqual(0, subprocess.call(['openssl', 'dgst', '-sha256', '-verify', pub, '-signature',
                                             os.path.join(self.tmp, 'zpkg.sig'), os.path.join(self.tmp, 'MANIFEST.sha256')],
                                            stdout=subprocess.DEVNULL))


class TestTemplate(unittest.TestCase):
    def test_template_builds_with_weaving(self):
        # a plugin's own @Transactional takes effect only woven at build time, as the platform's classes are
        if not shutil.which('mvn'):
            self.skipTest('mvn is not on PATH')
        tmp = tempfile.mkdtemp()
        try:
            project = os.path.join(tmp, 'example')
            shutil.copytree(os.path.join(HERE, 'template'), project, ignore=shutil.ignore_patterns('target'))
            repo = os.environ.get('ZPKG_TEST_M2_REPO')
            subprocess.check_call(['mvn', '-B', '-q', '-o', 'package'] + (['-Dmaven.repo.local=%s' % repo] if repo else []),
                                  cwd=project)
            with zipfile.ZipFile(os.path.join(project, 'target', 'example-1.0.0.jar')) as jar:
                _, strings, _ = zpkg_tool.class_info(jar.read('org/zstack/plugin/example/ExampleTransactional.class'))
            self.assertIn('org/springframework/transaction/aspectj/AnnotationTransactionAspect', strings)
        finally:
            shutil.rmtree(tmp)


class TestStaging(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.project = os.path.join(self.tmp, 'hsm')
        os.makedirs(os.path.join(self.project, 'target'))
        open(os.path.join(self.project, 'pom.xml'), 'w').close()
        open(os.path.join(self.project, 'target', 'hsm-test-1.0.0.jar'), 'w').close()

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def stage(self, desc, ui_dir=None):
        with open(os.path.join(self.project, 'plugin.yaml'), 'w') as f:
            yaml.safe_dump(desc, f)
        stage = zpkg_tool.staging(self.project, self.tmp, ui_dir)
        return sorted(os.path.relpath(os.path.join(d, n), stage) for d, _, ns in os.walk(stage) for n in ns)

    def test_ui_project_goes_to_ui(self):
        # the UI part is a package in zstack-ui-next: its build output is the remote module
        ui = os.path.join(self.tmp, 'ui')
        for name in ('dist/mf-manifest.json', 'dist/static/js/index.js', 'routes.json', 'menus.json', 'slots.json',
                     'locale/zh-CN.json', 'locale/en-US.json', 'src/index.ts', 'package.json'):
            os.makedirs(os.path.dirname(os.path.join(ui, name)), exist_ok=True)
            open(os.path.join(ui, name), 'w').close()
        self.assertEqual(['lib/hsm-test-1.0.0.jar', 'plugin.yaml', 'ui/locale/en-US.json', 'ui/locale/zh-CN.json',
                          'ui/menus.json', 'ui/remote/mf-manifest.json', 'ui/remote/static/js/index.js', 'ui/routes.json',
                          'ui/slots.json'],
                         self.stage({'id': 'hsm-test', 'version': '1.0.0'}, ui))

    def test_project_jar_goes_to_lib(self):
        self.assertEqual(['lib/hsm-test-1.0.0.jar', 'plugin.yaml'], self.stage({'id': 'hsm-test', 'version': '1.0.0'}))

    def test_project_jar_declared_as_driver_goes_to_drivers(self):
        desc = {'id': 'hsm-test', 'version': '1.0.0', 'drivers': ['drivers/hsm-test-1.0.0.jar']}
        self.assertEqual(['drivers/hsm-test-1.0.0.jar', 'plugin.yaml'], self.stage(desc))


if __name__ == '__main__':
    unittest.main()
