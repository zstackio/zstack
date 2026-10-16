package org.zstack.test.integration.configuration.systemTag

import org.zstack.core.db.SQLBatch
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.network.service.NetworkServiceSystemTag
import org.zstack.tag.PatternedSystemTag
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.L3NetworkSpec

class SystemTagTransactionCase extends SubCase {
    EnvSpec env
    PatternedSystemTag addressTag

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = 'tag-transaction-zone'
                l2NoVlanNetwork {
                    name = 'l2'
                    physicalInterface = 'eth0'
                    l3Network { name = 'l3' }
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            addressTag = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP
            String uuid = (env.specByName('l3') as L3NetworkSpec).inventory.uuid
            createAddress(uuid, '4', '192.0.2.2')
            createAddress(uuid, '6', '2001:db8::2')

            expect(IllegalStateException) {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        replaceAddress(uuid)
                        throw new IllegalStateException('fail after address replacement')
                    }
                }.execute()
            }
            assert addresses(uuid) == ['4': '192.0.2.2', '6': '2001:db8::2']

            new SQLBatch() {
                @Override
                protected void scripts() {
                    replaceAddress(uuid)
                }
            }.execute()
            assert addresses(uuid) == ['4': '192.0.2.3', '6': '2001:db8::2']
            verifyMtuRollback(uuid)
        }
    }

    void replaceAddress(String uuid) {
        String oldTag = addressTag.instantiateTag([ipUuid: '4', ip: '192.0.2.2'])
        addressTag.deleteInCurrentTransaction(uuid, oldTag, true)
        createAddress(uuid, '4', '192.0.2.3')
    }

    void createAddress(String uuid, String family, String address) {
        def creator = addressTag.newSystemTagCreator(uuid)
        creator.inherent = true
        creator.setTagByTokens([ipUuid: family, ip: address])
        creator.create()
    }

    Map addresses(String uuid) {
        addressTag.getTokensOfTagsByResourceUuid(uuid).collectEntries { [it.ipUuid, it.ip] }
    }

    void verifyMtuRollback(String uuid) {
        def mtuTag = NetworkServiceSystemTag.L3_MTU
        def original = mtuTag.getTokensByResourceUuid(uuid)
        expect(IllegalStateException) {
            new SQLBatch() {
                @Override
                protected void scripts() {
                    def creator = mtuTag.newSystemTagCreator(uuid)
                    creator.recreate = true
                    creator.inherent = false
                    creator.setTagByTokens([
                            (NetworkServiceSystemTag.L3_UUID_TOKEN): uuid,
                            (NetworkServiceSystemTag.MTU_TOKEN): '1510'])
                    creator.create()
                    throw new IllegalStateException('fail after MTU replacement')
                }
            }.execute()
        }
        assert mtuTag.getTokensByResourceUuid(uuid) == original
    }

    @Override
    void clean() {
        env.delete()
    }
}
