package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedQuotaCheckMessage;

import java.util.*;

/**
 * Created by frank on 7/13/2015.
 */
public class Quota {
    public interface QuotaOperator {
        void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs);

        void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs);

        List<QuotaUsage> getQuotaUsageByAccount(String accountUuid);
    }

    public interface QuotaValidator {
        void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs);

        Set<String> reportQuotaName();

        List<Class<? extends Message>> getMessagesNeedValidation();
    }

    public static class QuotaUsage {
        private String name;
        private Long total;
        private Long used;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public Long getUsed() {
            return used;
        }

        public void setUsed(Long used) {
            this.used = used;
        }
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

    private Set<String> quotaSet;
    private List<QuotaPair> quotaPairs;
    private List<Class<? extends Message>> messagesNeedValidation = new ArrayList<>();
    private QuotaOperator operator;
    private Set<QuotaValidator> quotaValidators;

    public void addPair(QuotaPair p) {
        if (quotaPairs == null) {
            quotaPairs = new ArrayList<>();
        }
        quotaPairs.add(p);
    }

    public List<QuotaPair> getQuotaPairs() {
        return quotaPairs;
    }

    public void setQuotaPairs(List<QuotaPair> quotaPairs) {
        this.quotaPairs = quotaPairs;
    }

    public void addMessageNeedValidation(Class<? extends Message> msgClass) {
        messagesNeedValidation.add(msgClass);
    }

    public List<Class<? extends Message>> getMessagesNeedValidation() {
        return messagesNeedValidation;
    }

    public QuotaOperator getOperator() {
        return operator;
    }

    public void setOperator(QuotaOperator operator) {
        this.operator = operator;
    }

    public Set<QuotaValidator> getQuotaValidators() {
        return this.quotaValidators;
    }

    public void addQuotaValidators(Set<QuotaValidator> quotaValidators) {
        if (this.quotaValidators == null) {
            this.quotaValidators = new HashSet<>();
        }
        for (QuotaValidator q : quotaValidators) {
            this.quotaValidators.add(q);
        }
    }

    public void addToQuotaSet(String quotaName) {
        if (this.quotaSet == null) {
            this.quotaSet = new HashSet<>();
        }
        this.quotaSet.add(quotaName);
    }

    public Set<String> getQuotaSet() {
        if (this.quotaSet == null) {
            this.quotaSet = new HashSet<>();
        }
        return this.quotaSet;
    }
}
