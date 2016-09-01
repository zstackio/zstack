package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by frank on 7/13/2015.
 */
public class Quota {
    public Set<QuotaValidator> getQuotaValidators() {
        return quotaValidators;
    }

    public void addQuotaValidators(Set<QuotaValidator> quotaValidators) {
        if (quotaValidators == null) {
            quotaValidators = new HashSet<>();
        }
        for (QuotaValidator q : quotaValidators) {
            quotaValidators.add(q);
        }
    }

    public interface QuotaOperator {
        void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs);

        List<QuotaUsage> getQuotaUsageByAccount(String accountUuid);
    }

    public interface QuotaValidator {
        void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs);

        List<Class<? extends APIMessage>> getMessagesNeedValidation();
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

    private List<QuotaPair> quotaPairs;
    private List<Class<? extends APIMessage>> messagesNeedValidation = new ArrayList<Class<? extends APIMessage>>();
    private QuotaOperator operator;
    private Set<QuotaValidator> quotaValidators;

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

    public void addMessageNeedValidation(Class<? extends APIMessage> msgClass) {
        messagesNeedValidation.add(msgClass);
    }

    public List<Class<? extends APIMessage>> getMessagesNeedValidation() {
        return messagesNeedValidation;
    }

    public QuotaOperator getOperator() {
        return operator;
    }

    public void setOperator(QuotaOperator operator) {
        this.operator = operator;
    }
}
