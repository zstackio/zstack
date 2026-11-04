#!/usr/bin/env python3
"""Check, pack and sign ZStack feature plugin packages (.zpkg).

  check --plugin-dir DIR --platform-lib WEB-INF/lib [--jdeps-output FILE] [--allowlist FILE]
  pack  --plugin-dir DIR --out DIR
  sign  --package PKG --key PRIVATE_KEY_PEM

DIR is either a plugin project made from template/ (plugin.yaml, pom.xml, target/*.jar, src/main/zpkg/)
or an unpacked package (plugin.yaml, lib/, classes/, db/, drivers/, agent/, ui/).
"""
import argparse
import ast
import fnmatch
import glob
import hashlib
import io
import json
import os
import re
import shutil
import struct
import subprocess
import sys
import tarfile
import tempfile
import xml.etree.ElementTree as ET
import zipfile

import yaml

HERE = os.path.dirname(os.path.abspath(__file__))
MANIFEST = 'META-INF/MANIFEST.sha256'
SIGNATURE = 'META-INF/zpkg.sig'
DESCRIPTOR = 'plugin.yaml'
PACKAGE_ROOTS = ('lib/', 'classes/', 'db/', 'drivers/', 'agent/', 'ui/')
# the ui part has fixed paths so that zstack-ctl and the BFF read it without a path table (SDK spec 7.1, P4 design 5.1);
# plugin code does not run in the BFF, which all users share and which cannot unload it
UI_PATHS = {'remote': 'ui/remote/', 'routes': 'ui/routes.json', 'menus': 'ui/menus.json', 'slots': 'ui/slots.json',
            'i18n': 'ui/locale/'}
UI_LOCALES = ('zh-CN', 'en-US')
UI_SLOTS = (('detail.tab', 'Host'),)
# what the kvmagent part of a plugin may import besides the standard library; its own modules are imported
# relatively (SDK spec 5.3)
AGENT_ALLOWED_MODULES = ('zstacklib.utils.jsonobject', 'zstacklib.utils.http', 'zstacklib.utils.log', 'zstacklib.utils.shell',
                         'zstacklib.utils.bash', 'zstacklib.utils.linux', 'zstacklib.utils.lock', 'zstacklib.utils.thread',
                         'kvmagent.kvmagent', 'kvmagent.sdk')
ERROR_CODE = re.compile(r'^ORG_ZSTACK_[A-Z0-9_]+_\d{5}$')
JAR_VERSION = re.compile(r'^(.*?)-\d[\w.]*\.jar$')
ENTITY = 'Ljavax/persistence/Entity;'
# what the platform registers only when it starts until P2b (SDK spec 2.5): by super class or interface ...
V1_UNSUPPORTED_SUPERS = {'org.zstack.header.query.APIQueryMessage': 'query API',
                         'org.zstack.header.vo.ResourceVO': 'resource',
                         'org.zstack.zwatch.namespace.AbstractNamespace': 'zwatch namespace',
                         'org.zstack.zwatch.datatype.Namespace': 'zwatch namespace'}
# ... and by annotation
V1_UNSUPPORTED_ANNOTATIONS = {'Lorg/zstack/header/search/Inventory;': 'inventory',
                              'Lorg/zstack/header/longjob/LongJobFor;': 'long job'}


def load_allowlist(path):
    allow, deny = [], []
    with open(path) as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#'):
                (deny if line.startswith('!') else allow).append(line.lstrip('!'))
    return allow, deny


def allowed(cls, allow, deny):
    if any(fnmatch.fnmatchcase(cls, p) for p in deny):
        return False
    return any(fnmatch.fnmatchcase(cls, p) for p in allow)


def jdeps_references(output, platform_jars):
    """Platform classes referenced in `jdeps -verbose:class` output; JDK classes carry no archive name."""
    refs = set()
    for line in output.splitlines():
        tokens = line.split()
        if len(tokens) >= 3 and tokens[0] == '->' and tokens[2] in platform_jars:
            refs.add(tokens[1])
    return refs


def class_info(data):
    """The name of a class file, the strings of its constant pool, and its super class and interfaces."""
    count = struct.unpack('>H', data[8:10])[0]
    pos, index = 10, 1
    utf8, classes = {}, {}
    sizes = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
    while index < count:
        tag = data[pos]
        if tag == 1:
            length = struct.unpack('>H', data[pos + 1:pos + 3])[0]
            utf8[index] = data[pos + 3:pos + 3 + length].decode('utf-8', 'replace')
            pos += 3 + length
        else:
            if tag == 7:
                classes[index] = struct.unpack('>H', data[pos + 1:pos + 3])[0]
            pos += 1 + sizes[tag]
        # long and double take two slots
        index += 2 if tag in (5, 6) else 1
    this_class, super_class, count = struct.unpack('>HHH', data[pos + 2:pos + 8])
    interfaces = struct.unpack('>%dH' % count, data[pos + 8:pos + 8 + 2 * count])

    def name(index):
        return utf8[classes[index]].replace('/', '.')
    return name(this_class), list(utf8.values()), [name(i) for i in (super_class,) + interfaces if i]


def plugin_jars(stage):
    return sorted(glob.glob(os.path.join(stage, 'lib', '*.jar')) + glob.glob(os.path.join(stage, 'drivers', '*.jar')))


def plugin_classes(stage):
    for jar in plugin_jars(stage):
        try:
            with zipfile.ZipFile(jar) as z:
                for name in z.namelist():
                    if name.endswith('.class'):
                        yield class_info(z.read(name))
        except zipfile.BadZipfile:
            continue


def platform_entity_names(platform_lib):
    """Simple names of the platform's entity classes; a byte search spares parsing every platform class."""
    names = set()
    key = ENTITY.encode('utf-8')
    for jar in sorted(glob.glob(os.path.join(platform_lib, '*.jar'))):
        try:
            with zipfile.ZipFile(jar) as z:
                for n in z.namelist():
                    if n.startswith('org/zstack/') and n.endswith('.class'):
                        data = z.read(n)
                        if key in data and ENTITY in class_info(data)[1]:
                            names.add(n[:-len('.class')].rsplit('/', 1)[-1])
        except zipfile.BadZipfile:
            continue
    return names


def _unsupported_kind(name, hierarchy, seen=()):
    # through the plugin's own classes; the platform classes a plugin may extend are on the allowlist
    for s in hierarchy.get(name, ()):
        if s in V1_UNSUPPORTED_SUPERS:
            return V1_UNSUPPORTED_SUPERS[s]
        if s in hierarchy and s not in seen:
            kind = _unsupported_kind(s, hierarchy, seen + (name,))
            if kind:
                return kind
    return None


def run_jdeps(stage, platform_lib):
    jdeps = os.path.join(os.environ['JAVA_HOME'], 'bin', 'jdeps') if os.environ.get('JAVA_HOME') else 'jdeps'
    out = []
    for jar in plugin_jars(stage):
        out.append(subprocess.check_output([jdeps, '-verbose:class', '-cp', os.path.join(platform_lib, '*'), jar],
                                           stderr=subprocess.DEVNULL).decode('utf-8', 'replace'))
    return '\n'.join(out)


def agent_imports(stage):
    """(file, module) of each absolute import in the kvmagent part; 'from a import b' gives a.b, relative imports
    of the plugin's own modules are left out."""
    found = []
    for dirpath, _, names in sorted(os.walk(os.path.join(stage, 'agent', 'kvmagent'))):
        for n in sorted(names):
            if not n.endswith('.py'):
                continue
            path = os.path.join(dirpath, n)
            with open(path) as f:
                tree = ast.parse(f.read(), path)
            rel = os.path.relpath(path, stage).replace(os.sep, '/')
            for node in ast.walk(tree):
                if isinstance(node, ast.Import):
                    found += [(rel, a.name) for a in node.names]
                elif isinstance(node, ast.ImportFrom) and not node.level:
                    found += [(rel, '%s.%s' % (node.module, a.name)) for a in node.names]
    return found


def _agent_import_allowed(module):
    # sys.stdlib_module_names needs python 3.10
    if module.split('.')[0] in sys.stdlib_module_names:
        return True
    return any(module == m or module.startswith(m + '.') for m in AGENT_ALLOWED_MODULES)


def check(stage, platform_lib, allowlist, jdeps_output=None):
    with open(os.path.join(stage, DESCRIPTOR)) as f:
        desc = yaml.safe_load(f)
    problems = []

    platform_jars = set(os.listdir(platform_lib))
    if jdeps_output:
        with open(jdeps_output) as f:
            output = f.read()
    else:
        output = run_jdeps(stage, platform_lib)
    allow, deny = load_allowlist(allowlist)
    for cls in sorted(jdeps_references(output, platform_jars)):
        if not allowed(cls, allow, deny):
            problems.append('disallowed-reference: %s' % cls)

    prefix = 'ORG_ZSTACK_PLUGIN_%s_' % desc['id'].upper().replace('-', '_')
    entities = set((desc.get('mn') or {}).get('entities') or [])
    classes = sorted(plugin_classes(stage))
    hierarchy = dict((name, supers) for name, _, supers in classes)
    for name, strings, _ in classes:
        for s in strings:
            if ERROR_CODE.match(s) and not s.startswith(prefix):
                problems.append('error-code-prefix: %s' % s)
        if ENTITY in strings and name not in entities:
            problems.append('undeclared-entity: %s' % name)
        kind = next((k for a, k in sorted(V1_UNSUPPORTED_ANNOTATIONS.items()) if a in strings), None) or \
            _unsupported_kind(name, hierarchy)
        if kind:
            problems.append('v1-unsupported: %s (%s)' % (name, kind))
    # JPQL names entities by their simple names
    plugin_entities = [name for name, strings, _ in classes if ENTITY in strings]
    if plugin_entities:
        taken = platform_entity_names(platform_lib)
        problems += ['entity-name-taken: %s' % n for n in plugin_entities if n.rsplit('.', 1)[-1] in taken]

    for path in sorted(glob.glob(os.path.join(stage, 'classes', 'globalConfig', '*.xml'))):
        for e in ET.parse(path).iter():
            if e.tag.split('}')[-1] == 'category' and (e.text or '').strip() != desc['id']:
                problems.append('config-category: %s: %s' % (os.path.basename(path), (e.text or '').strip()))

    def base(jar):
        m = JAR_VERSION.match(jar)
        return m.group(1) if m else jar[:-len('.jar')]
    platform_bases = set(base(j) for j in platform_jars if j.endswith('.jar'))
    for jar in sorted(os.path.basename(j) for j in plugin_jars(stage)):
        if base(jar) in platform_bases:
            problems.append('jar-conflict: %s' % jar)

    for f, module in agent_imports(stage):
        if not _agent_import_allowed(module):
            problems.append('disallowed-agent-import: %s in %s' % (module, f))
    return problems + ui_problems(stage, desc)


def ui_problems(stage, desc):
    """Format problems of the ui part, the same rules zstack-ctl applies before installing."""
    ui = desc.get('ui') or {}
    if not ui:
        # zstack-ctl deploys ui/ files by their path, undeclared they would skip every check below
        return ['ui-field: ui'] if os.path.isdir(os.path.join(stage, 'ui')) else []
    pid = desc['id']
    problems = ['ui-field: %s' % k for k in sorted(ui) if UI_PATHS.get(k) != ui[k]]
    if os.path.isdir(os.path.join(stage, 'ui', 'bff')):
        problems.append('ui-bff: ui/bff/')

    def load(name, default):
        path = os.path.join(stage, *name.split('/'))
        if not os.path.exists(path):
            return default
        with open(path) as f:
            return json.load(f)

    required = ['ui/remote/mf-manifest.json'] + ['ui/locale/%s.json' % l for l in UI_LOCALES]
    problems += ['ui-missing: %s' % n for n in required if not os.path.exists(os.path.join(stage, *n.split('/')))]

    routes = load('ui/routes.json', [])
    problems += ['ui-route: %s' % r['path'] for r in routes if not r['path'].startswith('/plugin/%s/' % pid)]
    paths = set(r['path'] for r in routes)
    problems += ['ui-menu: %s' % m['key'] for m in load('ui/menus.json', [])
                 if not m['key'].startswith(pid + '.') or m['route'] not in paths]
    problems += ['ui-slot: %s %s' % (s['slot'], s['resource']) for s in load('ui/slots.json', [])
                 if (s['slot'], s['resource']) not in UI_SLOTS]

    messages = [load('ui/locale/%s.json' % l, {}) for l in UI_LOCALES]
    keys = set().union(*messages)
    problems += ['ui-i18n: %s' % k for k in sorted(keys)
                 if not k.startswith(pid + '.') or any(k not in m for m in messages)]
    return problems


def staging(plugin_dir, tmp):
    """Lays a plugin project out as a package; an unpacked package is used as it is."""
    if not os.path.exists(os.path.join(plugin_dir, 'pom.xml')):
        return plugin_dir
    stage = os.path.join(tmp, 'stage')
    zpkg = os.path.join(plugin_dir, 'src', 'main', 'zpkg')
    if os.path.isdir(zpkg):
        shutil.copytree(zpkg, stage)
    else:
        os.makedirs(stage)
    shutil.copy(os.path.join(plugin_dir, DESCRIPTOR), stage)
    jars = [j for j in glob.glob(os.path.join(plugin_dir, 'target', '*.jar'))
            if not j.endswith('-sources.jar') and not j.endswith('-tests.jar')]
    if not jars:
        raise SystemExit('no jar in %s/target, build the project first' % plugin_dir)
    with open(os.path.join(plugin_dir, DESCRIPTOR)) as f:
        drivers = (yaml.safe_load(f) or {}).get('drivers') or []
    for jar in jars:
        # a jar declared in drivers is hot loaded as a plugin driver, the others go to WEB-INF/lib
        folder = 'drivers' if 'drivers/%s' % os.path.basename(jar) in drivers else 'lib'
        os.makedirs(os.path.join(stage, folder), exist_ok=True)
        shutil.copy(jar, os.path.join(stage, folder))
    return stage


def package_files(stage):
    files = [DESCRIPTOR]
    for root in PACKAGE_ROOTS:
        for dirpath, _, names in os.walk(os.path.join(stage, root)):
            for n in names:
                files.append(os.path.relpath(os.path.join(dirpath, n), stage).replace(os.sep, '/'))
    return sorted(files)


def sha256_of(path):
    digest = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def pack(stage, out):
    with open(os.path.join(stage, DESCRIPTOR)) as f:
        desc = yaml.safe_load(f)
    files = package_files(stage)
    # the same rule as zstack-ctl uses to verify: every file except the manifest and its signature, sorted
    manifest = ''.join('%s  %s\n' % (sha256_of(os.path.join(stage, n)), n) for n in files).encode('utf-8')
    path = os.path.join(out, '%s-%s.zpkg' % (desc['id'], desc['version']))
    with tarfile.open(path, 'w:gz') as tar:
        for n in files:
            tar.add(os.path.join(stage, n), arcname=n)
        _add_bytes(tar, MANIFEST, manifest)
    return path


def _add_bytes(tar, name, data):
    info = tarfile.TarInfo(name)
    info.size = len(data)
    info.mode = 0o644
    tar.addfile(info, io.BytesIO(data))


def sign(package, key):
    tmp = tempfile.mkdtemp()
    try:
        with tarfile.open(package, 'r:gz') as tar:
            entries = [(m, tar.extractfile(m).read()) for m in tar.getmembers() if m.isfile() and m.name != SIGNATURE]
        manifest = os.path.join(tmp, 'manifest')
        with open(manifest, 'wb') as f:
            f.write(dict((m.name, d) for m, d in entries)[MANIFEST])
        signature = os.path.join(tmp, 'signature')
        subprocess.check_call(['openssl', 'dgst', '-sha256', '-sign', key, '-out', signature, manifest])
        with open(signature, 'rb') as f:
            sig = f.read()
        with tarfile.open(package, 'w:gz') as tar:
            for m, d in entries:
                _add_bytes(tar, m.name, d)
            _add_bytes(tar, SIGNATURE, sig)
    finally:
        shutil.rmtree(tmp)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest='command')
    c = sub.add_parser('check')
    c.add_argument('--plugin-dir', required=True)
    c.add_argument('--platform-lib', required=True, help='WEB-INF/lib of the platform the plugin is built for')
    c.add_argument('--jdeps-output', help='use this jdeps -verbose:class output instead of running jdeps')
    c.add_argument('--allowlist', default=os.path.join(HERE, 'allowlist-v1.txt'))
    p = sub.add_parser('pack')
    p.add_argument('--plugin-dir', required=True)
    p.add_argument('--out', required=True)
    s = sub.add_parser('sign')
    s.add_argument('--package', required=True)
    s.add_argument('--key', required=True)
    args = parser.parse_args()

    if args.command == 'sign':
        sign(args.package, args.key)
        return
    tmp = tempfile.mkdtemp()
    try:
        stage = staging(args.plugin_dir, tmp)
        if args.command == 'check':
            problems = check(stage, args.platform_lib, args.allowlist, args.jdeps_output)
            for problem in problems:
                print(problem)
            sys.exit(1 if problems else 0)
        elif args.command == 'pack':
            print(pack(stage, args.out))
        else:
            parser.print_help()
    finally:
        shutil.rmtree(tmp)


if __name__ == '__main__':
    main()
