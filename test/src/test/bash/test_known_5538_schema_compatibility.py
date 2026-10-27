#!/usr/bin/env python3
"""Exercise the released 835350201 history with MariaDB 10.3 and Flyway 3.2.1.

Inputs are a disposable container and a schema-only fixture dump containing the
real migration history. Never pass a customer data dump. The test adds synthetic
rows, including 43 space mappings and 12 publications. The caller owns
container lifecycle. All logs and generated SQL stay outside the source tree.
"""
import argparse
import hashlib
import json
import pathlib
import re
import shutil
import subprocess


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--container', required=True)
parser.add_argument('--port', required=True, type=int)
parser.add_argument('--fixture', required=True, type=pathlib.Path)
parser.add_argument('--output', required=True, type=pathlib.Path)
args = parser.parse_args()
assert args.container.startswith('dataset-5548-compat-'), 'disposable test container required'
root = pathlib.Path(__file__).resolve().parents[4]
args.output.mkdir(parents=True, exist_ok=True)
password = 'local-compat-test'
mysql_cmd = ['docker', 'exec', '-i', args.container, 'mysql', '-uroot', '-p' + password,
             '--batch', '--raw', '--skip-column-names']
flyway = root / 'conf/tools/flyway-3.2.1/flyway'
current = root / 'conf/db/upgrade'
legacy_checksum = '835350201'
published_checksum = '-2089553698'
tables = ['SdnControllerVO', 'ZnsControllerVO', 'ZnsControllerStateVO',
          'ZnsTransportZoneVO', 'ZnsTenantRouterVO', 'ZnsNetworkNotificationVO',
          'ZsDatasetSpaceRefVO', 'ZsDatasetServiceKeyVO', 'ZsDatasetPublicationVO',
          'ModelVO', 'DatasetVO']
# Recreate only tables touched by the V38 delta or the four pending migrations.
# Replaying hundreds of unrelated historical tables adds no coverage per case.
fixture_tables = set(tables + [
    'schema_version', 'AIBusinessGatewayVO', 'AICapacityReservationVO', 'AccountVO',
    'AsyncRestVO', 'AuditsVO', 'LongJobVO', 'ModelCenterBusinessNetworkProfileVO',
    'ModelServiceInstanceGroupVO', 'ModelServiceTemplateVO', 'ResourceVO', 'SnmpEngineVO', 'SystemTagVO',
    'ZnsControllerNodeEndpointVO', 'ZnsControllerCapabilityVO', 'ZnsControllerTransitionVO',
    'ZnsCloudProtocolIdentityVO', 'ZnsCloudOfferSnapshotVO', 'ZnsPeerOfferVO',
    'ZnsSegmentProjectionDeleteVO', 'ZnsSegmentInventoryVO', 'ZnsSegmentRefVO'])
dump = args.fixture.read_text()
definitions = {}
for match in re.finditer(r'CREATE TABLE `([^`]+)` \(.*?\n\) ENGINE=.*?;', dump, re.S):
    if match[1] in fixture_tables:
        definitions[match[1]] = match[0]
assert fixture_tables <= definitions.keys(), 'complete schema fixture required'
fixture_inserts = [line for line in dump.splitlines() if line.startswith('INSERT INTO `')
                  and line.split('`')[1] in fixture_tables]
results = []


def sql(statement, database=True):
    command = mysql_cmd + (['zstack'] if database else [])
    process = subprocess.run(command, input=statement, text=True, capture_output=True)
    if process.returncode:
        raise AssertionError(process.stderr)
    return process.stdout.strip()


def reset():
    statements = ['SET FOREIGN_KEY_CHECKS=0;']
    for table in sorted(fixture_tables | {'ZnsSiteVO', 'ZnsResourceEventReceiptVO', 'ZnsControllerApiCompatibilityVO'}):
        statements.append('DROP TABLE IF EXISTS `' + table + '`;')
    statements.extend(definitions.values())
    statements.extend(fixture_inserts)
    statements.append('SET FOREIGN_KEY_CHECKS=1;')
    sql('\n'.join(statements))
    assert checksum() == legacy_checksum, 'fixture must keep the real released checksum'
    sql("INSERT INTO SdnControllerVO(uuid,vendorType,name,ip,username,password) VALUES "
        "('controller-proof','ZNS','synthetic-proof','192.0.2.1','fixture','not-a-real-secret'),"
        "('controller-unproven','ZNS','synthetic-unproven','192.0.2.2','fixture','not-a-real-secret');"
        "INSERT INTO ZnsControllerVO(uuid,compatibilityProofDigest) VALUES "
        "('controller-proof','synthetic-proof-digest'),('controller-unproven','');"
        "INSERT INTO ZnsControllerStateVO(sdnControllerUuid) VALUES('controller-proof');"
        "INSERT INTO ZnsTransportZoneVO(uuid,znsResourceUuid,znsSdnControllerUuid) "
        "VALUES('zone-fixture','zone-resource','controller-proof');"
        "INSERT INTO ZnsTenantRouterVO(uuid,sdnControllerUuid,znsResourceUuid,name) "
        "VALUES('router-fixture','controller-proof','router-resource','synthetic-router');"
        "INSERT INTO ZnsNetworkNotificationVO(uuid,controllerUuid,resourceType,resourceUuid,payload) "
        "VALUES('notification-fixture','controller-proof','synthetic','synthetic-resource','{}');"
        "INSERT INTO ZnsCloudProtocolIdentityVO(identityKey,clusterUuid) VALUES('legacy-identity','synthetic-cluster')")
    spaces = ["('%032x','synthetic-space-%s','00000000-0000-0000-0000-000000000001')" % (i, i)
              for i in range(1, 44)]
    publications = ["('%032x','00000000-0000-0000-0000-000000000001','synthetic-space-1','synthetic-request-%s','COMPLETED')" % (i + 100, i)
                    for i in range(1, 13)]
    sql('INSERT INTO ZsDatasetSpaceRefVO(uuid,spaceId,appInstanceUuid) VALUES ' + ','.join(spaces) + ';'
        'INSERT INTO ZsDatasetPublicationVO(uuid,appInstanceUuid,spaceId,publishRequestId,state) VALUES ' + ','.join(publications))
    assert sql('SELECT COUNT(*) FROM ZsDatasetSpaceRefVO') == '43'
    assert sql('SELECT COUNT(*) FROM ZsDatasetPublicationVO') == '12'


def checksum():
    return sql("SELECT checksum FROM schema_version WHERE script='V5.5.38__schema.sql'")


def capture(selected=tables):
    data = {}
    for table in selected:
        columns = sql("SELECT column_name FROM information_schema.columns WHERE "
                      "table_schema='zstack' AND table_name='%s' ORDER BY ordinal_position" % table).splitlines()
        if not columns:
            continue
        fields = ','.join('`' + column + '`' for column in columns)
        data[table] = (fields, sql('SELECT ' + fields + ' FROM `' + table + '` ORDER BY 1'))
    return data


def preserved(before):
    for table, (fields, rows) in before.items():
        assert sql('SELECT ' + fields + ' FROM `' + table + '` ORDER BY 1') == rows, table


def migrate(label, directory=current, success=True):
    command = ['bash', str(flyway), '-url=jdbc:mysql://127.0.0.1:%s/zstack' % args.port,
               '-user=root', '-password=' + password, '-locations=filesystem:' + str(directory),
               '-outOfOrder=true', 'migrate']
    process = subprocess.run(command, text=True, capture_output=True)
    log = args.output / (label + '.log')
    log.write_text(process.stdout + process.stderr)
    assert (process.returncode == 0) == success, label + ': see ' + str(log)
    return process.stdout + process.stderr


def assert_contract():
    assert sql('SELECT COUNT(*) FROM ZsDatasetSpaceRefVO') == '43'
    assert sql('SELECT COUNT(*) FROM ZsDatasetPublicationVO') == '12'
    assert sql("SELECT GROUP_CONCAT(peerManifestSupported ORDER BY uuid) FROM ZnsControllerVO") == '0,1'
    assert sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='zstack' "
               "AND table_name IN ('ZnsTransportZoneVO','ZnsTenantRouterVO') "
               "AND column_name='siteUuid' AND character_maximum_length=36") == '2'
    assert sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='zstack' "
               "AND table_name='ZnsResourceEventReceiptVO' AND column_name='eventCursor' "
               "AND is_nullable='YES'") == '1'
    assert sql("SELECT COUNT(*) FROM information_schema.referential_constraints WHERE "
               "constraint_schema='zstack' AND table_name IN ('ZnsSiteVO','ZnsResourceEventReceiptVO') "
               "AND delete_rule='CASCADE'") == '3'
    # An invalid reference and a duplicate key must be rejected with FK checks on.
    for statement in [
        "INSERT INTO ZnsSiteVO(uuid,sdnControllerUuid,znsResourceUuid,name) "
        "VALUES('invalid-site','no-such-controller','bad-site','synthetic')",
        "INSERT INTO ZnsResourceEventReceiptVO(uuid,controllerUuid,eventId,payloadDigest,taskUuid,receivedAt) "
        "VALUES('invalid-receipt','no-such-controller','event','synthetic','no-such-task','2000-01-01')",
    ]:
        process = subprocess.run(mysql_cmd + ['zstack'], input=statement, text=True, capture_output=True)
        assert process.returncode != 0 and 'foreign key constraint fails' in process.stderr.lower()
    assert checksum() == published_checksum
    # This is the unchanged official V48 behavior, not a compatibility callback promise.
    assert sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='zstack' "
               "AND table_name='ZnsCloudProtocolIdentityVO'") == '0'


def published_table(name):
    source = (current / 'V5.5.38__schema.sql').read_text()
    start = source.index('CREATE TABLE IF NOT EXISTS `zstack`.`' + name + '`')
    return source[start:source.index(';', start) + 1]


def preexisting_rows():
    sql(published_table('ZnsSiteVO') + '\n' + published_table('ZnsResourceEventReceiptVO'))
    sql("INSERT INTO ZnsSiteVO(uuid,sdnControllerUuid,znsResourceUuid,name) "
        "VALUES('existing-site','controller-proof','site-resource','synthetic');"
        "INSERT INTO ZnsResourceEventReceiptVO(uuid,controllerUuid,eventId,eventCursor,payloadDigest,taskUuid,receivedAt) "
        "VALUES('existing-receipt','controller-proof','existing-event',7,'synthetic','notification-fixture','2000-01-01')")


source_hash = hashlib.sha256((current / 'beforeValidate.sql').read_bytes()).hexdigest()
historical_hashes = {name: hashlib.sha256((current / name).read_bytes()).hexdigest()
                     for name in ['V5.5.38__schema.sql', 'V5.5.48__schema.sql']}
# Verify the failure against the unchanged callback, not by disabling a guard in the fix.
red = args.output / 'unpatched'
shutil.copytree(current, red, dirs_exist_ok=True)
original = subprocess.check_output(['git', '-C', str(root), 'show',
                                    '7f811038919e700819153bdf17b80d307449d892:conf/db/upgrade/beforeValidate.sql'])
(red / 'beforeValidate.sql').write_bytes(original)
reset()
before = capture()
red_log = migrate('red-real-history', red, success=False)
assert 'migration checksum mismatch for migration 5.5.38' in red_log.lower()
assert legacy_checksum in red_log and published_checksum in red_log
assert checksum() == legacy_checksum
preserved(before)
results.append('RED: unchanged callback rejects the real 835350201 history')

reset()
before = capture()
migrate('green-missing-tables')
preserved(before)
assert_contract()
results.append('GREEN: missing tables, controller backfill, 43 spaces/12 publications preserved')
before = capture(tables + ['ZnsSiteVO', 'ZnsResourceEventReceiptVO'])
migrate('green-repeat')
preserved(before)
assert_contract()
results.append('GREEN: compatible history and repeated migrate are unchanged')

reset()
preexisting_rows()
before = capture(tables + ['ZnsSiteVO', 'ZnsResourceEventReceiptVO'])
migrate('green-existing-business-rows')
preserved(before)
assert_contract()
sql("INSERT INTO ZnsResourceEventReceiptVO(uuid,controllerUuid,eventId,payloadDigest,taskUuid,receivedAt) "
    "VALUES('nullable-cursor','controller-proof','new-event','synthetic','notification-fixture','2000-01-01')")
duplicate = subprocess.run(mysql_cmd + ['zstack'], input=
    "INSERT INTO ZnsSiteVO(uuid,sdnControllerUuid,znsResourceUuid,name) "
    "VALUES('duplicate-site','controller-proof','site-resource','synthetic')", text=True, capture_output=True)
assert duplicate.returncode != 0 and 'Duplicate entry' in duplicate.stderr
sql("DELETE FROM SdnControllerVO WHERE uuid='controller-proof'")
assert sql('SELECT COUNT(*) FROM ZnsSiteVO') == '0'
assert sql('SELECT COUNT(*) FROM ZnsResourceEventReceiptVO') == '0'
results.append('GREEN: existing rows, unique keys, nullable cursor and cascade FKs')

reset()
sql("UPDATE schema_version SET checksum=835350202 WHERE script='V5.5.38__schema.sql'")
before = capture()
unknown_log = migrate('unknown-checksum-rejected', success=False)
assert 'migration checksum mismatch for migration 5.5.38' in unknown_log.lower()
assert checksum() == '835350202'
preserved(before)
assert sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='zstack' "
           "AND table_name IN ('ZnsSiteVO','ZnsResourceEventReceiptVO')") == '0'
results.append('GREEN: unknown checksum rejected without schema acknowledgement')

reset()
preexisting_rows()
sql('ALTER TABLE ZnsSiteVO DROP INDEX uk_zns_site_controller_uuid')
before = capture(tables + ['ZnsSiteVO', 'ZnsResourceEventReceiptVO'])
bad_log = migrate('inconsistent-unique-rejected', success=False)
assert 'unique key is inconsistent' in bad_log
assert checksum() == legacy_checksum
preserved(before)
results.append('GREEN: inconsistent existing table is rejected before history acknowledgement')

sql('SET FOREIGN_KEY_CHECKS=0; DROP DATABASE zstack; CREATE DATABASE zstack;', False)
fresh = args.output / 'fresh'
shutil.copytree(current, fresh, dirs_exist_ok=True)
shutil.copyfile(root / 'conf/db/V0.6__schema.sql', fresh / 'V0.6__schema.sql')
migrate('green-fresh-install', fresh)
assert checksum() == published_checksum
assert sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='zstack' "
           "AND table_name IN ('ZnsSiteVO','ZnsResourceEventReceiptVO')") == '2'
results.append('GREEN: unmodified fresh installation migration chain')

assert source_hash == hashlib.sha256((current / 'beforeValidate.sql').read_bytes()).hexdigest()
for name, expected in historical_hashes.items():
    assert hashlib.sha256((current / name).read_bytes()).hexdigest() == expected
print(json.dumps({'status': 'passed', 'cases': results, 'callbackSha256': source_hash,
                  'historicalHashes': historical_hashes}, ensure_ascii=False, indent=2))
