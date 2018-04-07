package org.zstack.identity.rbac;

import org.zstack.header.identity.PolicyInventory;
import org.zstack.header.identity.PolicyStatement;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIReply;
import org.zstack.header.message.Message;
import org.zstack.identity.APIResponseFilter;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RBACAPIResponseFilter implements APIResponseFilter {
    private static final CLogger logger = Utils.getLogger(RBACAPIResponseFilter.class);

    private APIMessage request;
    private Message response;

    @Override
    public Message filter(APIMessage req, Message rsp) {
        request = req;
        response = rsp;

        boolean doFilter = false;
        if (rsp instanceof APIReply) {
            doFilter = ((APIReply)rsp).isSuccess();
        } else if (rsp instanceof APIEvent) {
            doFilter = ((APIEvent)rsp).isSuccess();
        }

        if (doFilter) {
            filter();
        }

        return response;
    }

    private void filter() {
        Map<String, String> schema = (Map<String, String>) response.getHeaders().get("schema");
        if (schema == null || schema.isEmpty()) {
            return;
        }

        Map<PolicyInventory, List<PolicyStatement>> denyPolices = RBACManager.collectDenyStatements(RBACManager.getPoliciesByAPI(request));
        denyPolices.forEach((p, sts)-> sts.forEach(s -> s.getResources().forEach(statement -> {
            schema.forEach((path, type)-> {
                String[] ss = statement.split(":", 2);
                String resourceName = ss[0];
                String fieldList = null;
                if (ss.length > 1) {
                    fieldList = ss[1];
                }

                Pattern pattern = Pattern.compile(resourceName);
                if (!pattern.matcher(type).matches()) {
                    // the statement not matching this inventory
                    return;
                }

                if (fieldList == null) {
                    // the whole inventory is denied
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("[RBAC] denied inventory[%s] at %s", type, path));
                    }
                    denyTheInventory(path);
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("[RBAC] denied fields[%s] of inventory[%s] at %s", fieldList, type, path));
                    }
                    denyFieldsOfTheInventory(path, fieldList);
                }
            });
        })));
    }

    private void denyFieldsOfTheInventory(String path, String fieldList) {
        String[] fields = fieldList.split(",");
        Object bean = BeanUtils.getProperty(response, path);
        for (String field : fields) {
            field = field.trim();
            BeanUtils.setProperty(bean, field, null);
        }
    }

    private void denyTheInventory(String path) {
        Pattern p = Pattern.compile("(.*)\\[(\\d+)\\]$");
        Matcher m = p.matcher(path);

        if (m.matches()) {
            // this is a list
            String listPath = m.group(1);
            String index = m.group(2);

            List cl = (List) BeanUtils.getProperty(response, listPath);
            cl.remove(Integer.valueOf(index));
        } else {
            // not a list
            BeanUtils.setProperty(response, path, null);
        }
    }
}
