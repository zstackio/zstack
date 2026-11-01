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


def make_class(name, strings=()):
    """A class file with just enough of a constant pool for zpkg_tool to read."""
    pool = [b'\x01' + struct.pack('>H', len(name.replace('.', '/'))) + name.replace('.', '/').encode('utf-8'),
            b'\x07' + struct.pack('>H', 1)]
    for s in strings:
        data = s.encode('utf-8')
        pool.append(b'\x01' + struct.pack('>H', len(data)) + data)
    head = b'\xca\xfe\xba\xbe' + struct.pack('>HHH', 0, 52, len(pool) + 1)
    return head + b''.join(pool) + struct.pack('>HHH', 0x21, 2, 0) + b'\x00' * 8


class CheckCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.platform = os.path.join(self.tmp, 'platform-lib')
        os.makedirs(self.platform)
        for jar in ('header-5.5.0.jar', 'compute-5.5.0.jar', 'gson-2.8.5.jar'):
            open(os.path.join(self.platform, jar), 'w').close()

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def run_check(self, refs=(), classes=None, desc=None, plugin_jars=(), global_configs=None):
        stage = os.path.join(self.tmp, 'stage')
        os.makedirs(os.path.join(stage, 'lib'))
        d = {'id': 'tpm', 'version': '1.0.0', 'requires': {'sdk': '>=1.0 <2'}, 'mn': {'entities': []}}
        d.update(desc or {})
        with open(os.path.join(stage, 'plugin.yaml'), 'w') as f:
            yaml.safe_dump(d, f)
        with zipfile.ZipFile(os.path.join(stage, 'lib', 'tpm-1.0.0.jar'), 'w') as jar:
            for name, strings in (classes or {'org.zstack.tpm.TpmManager': ()}).items():
                jar.writestr(name.replace('.', '/') + '.class', make_class(name, strings))
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


class TestStaging(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.project = os.path.join(self.tmp, 'hsm')
        os.makedirs(os.path.join(self.project, 'target'))
        open(os.path.join(self.project, 'pom.xml'), 'w').close()
        open(os.path.join(self.project, 'target', 'hsm-test-1.0.0.jar'), 'w').close()

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def stage(self, desc):
        with open(os.path.join(self.project, 'plugin.yaml'), 'w') as f:
            yaml.safe_dump(desc, f)
        stage = zpkg_tool.staging(self.project, self.tmp)
        return sorted(os.path.relpath(os.path.join(d, n), stage) for d, _, ns in os.walk(stage) for n in ns)

    def test_project_jar_goes_to_lib(self):
        self.assertEqual(['lib/hsm-test-1.0.0.jar', 'plugin.yaml'], self.stage({'id': 'hsm-test', 'version': '1.0.0'}))

    def test_project_jar_declared_as_driver_goes_to_drivers(self):
        desc = {'id': 'hsm-test', 'version': '1.0.0', 'drivers': ['drivers/hsm-test-1.0.0.jar']}
        self.assertEqual(['drivers/hsm-test-1.0.0.jar', 'plugin.yaml'], self.stage(desc))


if __name__ == '__main__':
    unittest.main()
