package helpers;

import java.util.*;
import javax.xml.bind.annotation.*;
import org.eclipse.persistence.oxm.annotations.XmlVariableNode;

@XmlAccessorType(XmlAccessType.FIELD)
public class JsonSchema {

  private String title;

  private String type;

  @XmlElementWrapper
  @XmlVariableNode("name")
  public Map<String, Property> properties;

  private List<String> required;

}