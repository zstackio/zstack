package org.zstack.test.integration.core.notification

import org.springframework.http.HttpEntity
import org.zstack.core.notification.NotificationConstant
import org.zstack.core.notification.NotificationGlobalConfig
import org.zstack.core.notification.NotificationStatus
import org.zstack.core.notification.NotificationVO
import org.zstack.header.zone.ZoneVO
import org.zstack.sdk.NotificationInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/3/18.
 */
class NotificationCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
            }
        }
    }

    void testQueryNotifications() {
        ZoneInventory zone = env.inventoryByName("zone")

        NotificationInventory inv = queryNotification {
            conditions=["resourceType=${ZoneVO.class.simpleName}", "resourceUuid=${zone.uuid}"]
        }[0]

        assert inv != null
        assert inv.status == NotificationStatus.Unread.toString()
    }

    void testUpdateNotificationsStatus() {
        ZoneInventory zone = env.inventoryByName("zone")

        NotificationInventory inv = queryNotification {
            conditions=["resourceType=${ZoneVO.class.simpleName}", "resourceUuid=${zone.uuid}"]
        }[0]

        updateNotificationsStatus {
            uuids=[inv.uuid]
            status=NotificationStatus.Read.toString()
        }

         inv = queryNotification {
            conditions=["resourceType=${ZoneVO.class.simpleName}", "resourceUuid=${zone.uuid}"]
        }[0]

        assert inv.status == NotificationStatus.Read.toString()
    }

    void testDeleteNotifications() {
        ZoneInventory zone = env.inventoryByName("zone")

        NotificationInventory inv = queryNotification {
            conditions=["resourceType=${ZoneVO.class.simpleName}", "resourceUuid=${zone.uuid}"]
        }[0]

        deleteNotifications {
            uuids = [inv.uuid]
        }

        assert !dbIsExists(inv.uuid, NotificationVO.class)
    }

    void testNotificationWebhook() {
        String webhook = "/notification-web-hook"

        List<NotificationInventory> invs = null
        env.simulator(webhook) { HttpEntity<String> e ->
            invs = JSONObjectUtil.toCollection(e.getBody(), ArrayList.class, NotificationInventory.class)
        }

        NotificationGlobalConfig.WEBHOOK_URL.updateValue("http://127.0.0.1:8989$webhook")

        createZone {
            name = "test zone"
        }

        retryInSecs {
            assert invs != null
            assert invs.size() == 1
            NotificationInventory inv = invs[0]
            assert inv.sender == NotificationConstant.API_SENDER
        }
    }

    @Override
    void test() {
        env.create {
            testQueryNotifications()
            testUpdateNotificationsStatus()
            testDeleteNotifications()
            testNotificationWebhook()
        }
    }
}
