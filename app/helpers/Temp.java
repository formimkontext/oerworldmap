package helpers;

import java.util.*;
import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import org.eclipse.persistence.jaxb.JAXBContextProperties;

public class Temp {

  public static void main(String[] args) throws Exception {
    Map<String, Object> properties = new HashMap<String, Object>();
    // properties.put(JAXBContextProperties.MEDIA_TYPE, "application/json");
    // properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);
    JAXBContext jc = JAXBContext.newInstance(new Class[] { JsonSchema.class }, properties);

    // File file = new File("test/resources/article.json");
    File file = new File("test/resources/test.json");

    Unmarshaller unmarshaller = jc.createUnmarshaller();
    StreamSource json = new StreamSource(file.getAbsolutePath());
    JsonSchema jsonSchema = unmarshaller.unmarshal(json, JsonSchema.class).getValue();

    Marshaller marshaller = jc.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    marshaller.marshal(jsonSchema, System.out);
  }
}