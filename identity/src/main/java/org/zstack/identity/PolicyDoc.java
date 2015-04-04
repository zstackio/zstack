package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.PolicyInventory;
import org.zstack.header.identity.PolicyVO;
import org.zstack.identity.schema.Policy;
import org.zstack.identity.schema.StatementType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PolicyDoc extends PolicyInventory {
    private static final CLogger logger = Utils.getLogger(PolicyDoc.class);

    @Autowired
    private ErrorFacade errf;

    public static class Statement {
        private String effect;
        private List<String> roles;

        public String getEffect() {
            return effect;
        }

        public void setEffect(String effect) {
            this.effect = effect;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }

    private static final String SCHEMA_NAME = "policy.xsd";
    private static final URL schemaUrl;
    static {
        schemaUrl = PolicyDoc.class.getClassLoader().getResource(SCHEMA_NAME);
        if (schemaUrl == null) {
            throw new IllegalArgumentException(String.format("Can not find policy schema file[%s] in classpath", SCHEMA_NAME));
        }
    }

    private List<Statement> statements = new ArrayList<Statement>();

    public PolicyDoc(PolicyVO vo) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance("org.zstack.identity.schema");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader sr = new StringReader(vo.getData());
        Policy p = (Policy) unmarshaller.unmarshal(sr);
        for (StatementType s : p.getStatement()) {
            Statement st = new Statement();
            st.effect = s.getEffect();
            st.roles = s.getRole();
            statements.add(st);
        }

        super.setAccountUuid(vo.getAccountUuid());
        super.setData(vo.getData());
        super.setType(vo.getType().toString());
        super.setUuid(vo.getUuid());
    }

    public static ErrorCode validateXmlData(String data) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaUrl);
            JAXBContext context = JAXBContext.newInstance("org.zstack.identity.schema");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader sr = new StringReader(data);
            unmarshaller.setSchema(schema);
            unmarshaller.unmarshal(sr);
            return null;
        } catch (Exception e) {
            logger.debug("", e);
            return new ErrorCode();
        }
    }

    public static String generatePolicyXmlData(String effect, List<String> roles) {
        try {
            Policy p = new Policy();
            StatementType st = new StatementType();
            st.setEffect(effect);
            st.getRole().addAll(roles);
            p.getStatement().add(st);
            JAXBContext context = JAXBContext.newInstance("org.zstack.identity.schema");
            Marshaller marshaller = context.createMarshaller();
            StringWriter sw = new StringWriter();
            marshaller.marshal(p, sw);
            return sw.toString();
        } catch (Exception e) {
            throw new CloudRuntimeException("Cannot generate policy xml data", e);
        }
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public void setStatements(List<Statement> statements) {
        this.statements = statements;
    }
}
