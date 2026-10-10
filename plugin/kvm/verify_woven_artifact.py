#!/usr/bin/env python3
"""Read-only release gate for KVM's compile-time deferred cleanup advice.

Run after the final Maven lifecycle, on the JAR actually included in the WAR.
This supplements (not replaces) functional tests and source compilation.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import zipfile

CRITICAL = 'org/zstack/kvm/hypervisor/KvmHypervisorInfoManagerImpl.class'
ANNOTATION = b'Lorg/zstack/core/defer/Deferred;'
CLEANUP = b'ajc$after$org_zstack_core_defer_DeferAspect$4$'


def verify(data):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        if CRITICAL not in archive.namelist():
            raise ValueError('KVM production lock class missing')
        checked = []
        for name in archive.namelist():
            if not name.endswith('.class'):
                continue
            bytecode = archive.read(name)
            if ANNOTATION in bytecode or name == CRITICAL:
                if CLEANUP not in bytecode:
                    raise ValueError('Missing compiled deferred cleanup advice: ' + name)
                checked.append(name)
        return {'sha256': hashlib.sha256(data).hexdigest(),
                'deferred_classes_checked': len(checked), 'classes': checked}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('jar', type=Path)
    parser.add_argument('--war', type=Path)
    args = parser.parse_args()
    data = args.jar.read_bytes()
    result = verify(data)
    if args.war:
        with zipfile.ZipFile(args.war) as archive:
            embedded = archive.read('WEB-INF/lib/' + args.jar.name)
            if embedded != data:
                raise ValueError('WAR KVM bytes differ from verified standalone JAR')
            verify(embedded)
        result['war_matches'] = True
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
