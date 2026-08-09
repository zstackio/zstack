package org.zstack.test.integration.network.sdnController;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Paths;

public class SdnControllerServiceConfigTest {
    @Test
    public void pullResourceApiIsRoutedToSdnControllerService() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(
                Paths.get("../conf/serviceConfig/sdnController.xml").toFile());

        NodeList messages = document.getElementsByTagNameNS("*", "message");
        for (int i = 0; i < messages.getLength(); i++) {
            Element message = (Element) messages.item(i);
            NodeList names = message.getElementsByTagNameNS("*", "name");
            if (names.getLength() == 0) {
                continue;
            }
            Node name = names.item(0);
            if ("org.zstack.sdnController.header.APIPullSdnControllerMsg"
                    .equals(name.getTextContent().trim())) {
                Assert.assertEquals(0,
                        message.getElementsByTagNameNS("*", "serviceId").getLength());
                return;
            }
        }

        Assert.fail("APIPullSdnControllerMsg is not routed to SdnController service");
    }
}
