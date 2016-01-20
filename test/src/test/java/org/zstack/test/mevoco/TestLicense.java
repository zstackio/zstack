package org.zstack.test.mevoco;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.zstack.license.LicenseChecker;
import org.zstack.license.LicenseInfo;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by frank on 12/30/2015.
 */
public class TestLicense {
    @Test
    public void test() throws Exception {
        File licpath = PathUtil.findFileOnClassPath("license.txt", true);
        File privpath = PathUtil.findFileOnClassPath("priv.key", true);
        File capath = PathUtil.findFileOnClassPath("ca.pem", true);

        FileInputStream lic = new FileInputStream(licpath);
        FileInputStream priv = new FileInputStream(privpath);
        FileInputStream ca = new FileInputStream(capath);

        //SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        //Date d = f.parsed("2015-12-30T11:25:22+08:00", new ParsePosition(0));
        //DateFormat df = DateFormat.getDateInstance();
        //Date d = df.parse("2015-12-30T11:25:22+08:00");
        //System.out.println(d.toString());

        DateTime d = new DateTime("2015-12-30T11:25:22+08:00");
        System.out.println(d.toString());

        LicenseChecker checker = new LicenseChecker(lic, priv, ca);
        LicenseInfo info = checker.getLicenseInfo();

        System.out.println("user: " + info.getUser());
        System.out.println("hostnum: " + info.getHostNum());
        System.out.println("type: " + info.getLicenseType());
        System.out.println("issue time: " + info.getIssueTime());
        System.out.println("expired time: " + info.getExpireTime());

        for (Object o : info.getThumbprint().entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            System.out.println(pair.getKey() + " = " + pair.getValue());
        }

        ca.close();
        priv.close();
        lic.close();

        String pkey = FileUtils.readFileToString(privpath);
        String licreq = "eyJ0aHVtYnByaW50IjoiZXlKMlpYSnphVzl1SWpvaU1DNHhJaXdpYUc5emRHNWhiV1VpT2lKc2IyTmhiR2h2YzNRaUxDSmpjSFZ6SWpvaU5DSXNJbU53ZFcxdlpHVnNJam9pU1c1MFpXd29VaWtnUTI5eVpTaFVUU2tnYVRjdE5qVXdNRlVnUTFCVklFQWdNaTQxTUVkSWVpSXNJbTFsYldsdWEySWlPaUkwTURNek5qTTJJbjA9IiwicHVia2V5IjoiLS0tLS1CRUdJTiBSU0EgUFVCTElDIEtFWS0tLS0tXG5NSUlCSERBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVFrQU1JSUJCQUtCL0Rmd1I2c0NGZDVleDdWcHpYa2dLd25zXG4rKzhyWElkblBKQ0swaStvaHVVUWl5b1hKUnhwMmJqZXJsM1JSbW1WeGtjMlRQeU1xWjVhR0hMMElpdHhPVlVkXG5JcmtHblFVeHEyc2JGRHJIVXplNHA0MWtRWStWbTB4REo1Tmk0bTRQZUp4eDVBVkNaMkVCSzN0RlA2REl4R0ZpXG5VUDdvMGdGeFhLU1VvVzFncFhITjNpbUJSUWh5cEYrQldmQ0pmeThSL2tPbGI1KytCZHovc2EvRzByMTNHQU5aXG5qUkZuVFhSMDZoaWd3TzI2a0ttcTlsdDd2UVNaSXpGaDJVbzZGdzlIL0M3bkk2U00rZDE4R3drdXVNUUIyZFZVXG55S2FkK1h3dE1xN1JqT2xrdGdUcE56SEpMa05IdUJHcEZveE1yLzRtdUxKYVRCUHdSR202M1VqMWp3SURBUUFCXG4tLS0tLUVORCBSU0EgUFVCTElDIEtFWS0tLS0tXG4ifQ==";
        Map<String, String> m = new HashMap<String, String>();
        m.put("privateKey", pkey);
        m.put("license", licreq);
        String jstr = JSONObjectUtil.toJsonString(m);
        String encoded = DatatypeConverter.printBase64Binary(jstr.getBytes());
        System.out.println(encoded);
        byte[] decoded = DatatypeConverter.parseBase64Binary(encoded);
        String jstr1 = new String(decoded);
        System.out.println(jstr1);
        m = JSONObjectUtil.toObject(jstr1, HashMap.class);
        System.out.println(m.get("privateKey"));
        System.out.println(m.get("license"));
    }
}
