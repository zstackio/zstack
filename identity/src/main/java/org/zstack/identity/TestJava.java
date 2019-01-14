package org.zstack.identity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Created by lining on 2019/1/14.
 */
public class TestJava {
    public static void main(String[] args) {
        String password = "123";

        if (password.matches("(?=.*[a-zA-Z]).*")) {
           System.out.println("dsf");
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
        System.out.println(new Timestamp(calendar.getTime().getTime()));

        Timestamp expire = new Timestamp(calendar.getTime().getTime());
        System.out.println(expire.before(new Timestamp(System.currentTimeMillis())));
        System.out.println(new Timestamp(System.currentTimeMillis()).before(expire));

        String white = "[{\"ipAddressRangeType\": \"IpRange1\", \"ip\": \"0.0.0.0 - 255.255.255.255\", \"state\": \"Enabled\"}]";
        Gson gson = new GsonBuilder().create();
        //IpWhiteListConfigList list = gson.fromJson(white, IpWhiteListConfigList.class);
        //System.out.println(list);
    }
}
