package helpers;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class Property {
     
    @XmlTransient
    private String name;
 
    private String description;
     
    private String type;
     
    private Integer minimum;
 
}