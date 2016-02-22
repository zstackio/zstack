package org.zstack.header.identity;

import org.zstack.header.message.APIReply;

import java.util.List;

/**
 * Created by frank on 2/22/2016.
 */
public class APIGetAccountQuotaUsageReply extends APIReply {
    private List<Quota.QuotaUsage> usages;

    public List<Quota.QuotaUsage> getUsages() {
        return usages;
    }

    public void setUsages(List<Quota.QuotaUsage> usages) {
        this.usages = usages;
    }
}
