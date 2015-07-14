package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/13/2015.
 */
public class Quota {
    public interface CheckQuotaForApiMessage {
        void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs);
    }

    public static class QuotaPair {
        private String name;
        private long value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }

    private List<QuotaPair> quotaPairs;
    private Class<APIMessage> messageNeedValidation;
    private CheckQuotaForApiMessage checker;

    public void addPair(QuotaPair p) {
        if (quotaPairs == null) {
            quotaPairs = new ArrayList<QuotaPair>();
        }
        quotaPairs.add(p);
    }

    public List<QuotaPair> getQuotaPairs() {
        return quotaPairs;
    }

    public void setQuotaPairs(List<QuotaPair> quotaPairs) {
        this.quotaPairs = quotaPairs;
    }

    public Class<APIMessage> getMessageNeedValidation() {
        return messageNeedValidation;
    }

    public void setMessageNeedValidation(Class messageNeedValidation) {
        this.messageNeedValidation = messageNeedValidation;
    }

    public CheckQuotaForApiMessage getChecker() {
        return checker;
    }

    public void setChecker(CheckQuotaForApiMessage checker) {
        this.checker = checker;
    }
}
