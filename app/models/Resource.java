package models;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import helpers.FilesConfig;
import helpers.JsonLdConstants;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import play.Logger;
import play.Play;

public class Resource extends HashMap<String, Object>implements Comparable<Resource> {

  /**
   *
   */
  private static final long serialVersionUID = -6177433021348713601L;

  // identified ("primary") data types that get an ID
  private static final List<String> mIdentifiedTypes = new ArrayList<>(Arrays.asList(
      "Organization", "Event", "Person", "Action", "WebPage", "Article", "Service", "ConceptScheme", "Concept",
    "Comment"));

  private static JsonNode mSchemaNode = null;

  static {
    try {
      mSchemaNode = new ObjectMapper().readTree(Paths.get(FilesConfig.getSchema()).toFile());
    } catch (IOException e) {
      Logger.error(e.toString());
    }
  }

  /**
   * Constructor which sets up a random UUID.
   *
   * @param type
   *          The type of the resource.
   */
  public Resource(final String type) {
    this(type, null);
  }

  /**
   * Constructor.
   *
   * @param aType
   *          The type of the resource.
   * @param aId
   *          The id of the resource.
   */
  public Resource(final String aType, final String aId) {
    if (null != aType) {
      this.put(JsonLdConstants.TYPE, aType);
    }
    if (null != aId) {
      this.put(JsonLdConstants.ID, aId);
    } else if (mIdentifiedTypes.contains(aType)) {
      this.put(JsonLdConstants.ID, generateId());
    }
  }

  private static String generateId() {
    return "urn:uuid:" + UUID.randomUUID().toString();
  }

  /**
   * Convert a Map of String/Object to a Resource, assuming that all Object
   * values of the map are properly represented by the toString() method of
   * their class.
   *
   * @param aProperties
   *          The map to create the resource from
   * @return a Resource containing all given properties
   */
  @SuppressWarnings("unchecked")
  public static Resource fromMap(Map<String, Object> aProperties) {

    if (aProperties == null) {
      return null;
    }

    String type = (String) aProperties.get(JsonLdConstants.TYPE);
    String id = (String) aProperties.get(JsonLdConstants.ID);
    Resource resource = new Resource(type, id);

    for (Map.Entry<String, Object> entry : aProperties.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key.equals(JsonLdConstants.ID) && !mIdentifiedTypes.contains(type)) {
        continue;
      }
      if (value instanceof Map<?, ?>) {
        resource.put(key, Resource.fromMap((Map<String, Object>) value));
      } else if (value instanceof List<?>) {
        List<Object> vals = new ArrayList<>();
        for (Object v : (List<?>) value) {
          if (v instanceof Map<?, ?>) {
            vals.add(Resource.fromMap((Map<String, Object>) v));
          } else {
            vals.add(v);
          }
        }
        resource.put(key, vals);
      } else {
        resource.put(key, value);
      }
    }

    return resource;

  }

  public static Resource fromJson(JsonNode aJson) {
    Map<String, Object> resourceMap = new ObjectMapper().convertValue(aJson,
        new TypeReference<HashMap<String, Object>>() {
        });
    return fromMap(resourceMap);
  }

  public static Resource fromJson(String aJsonString) {
    try {
      return fromJson(new ObjectMapper().readTree(aJsonString));
    } catch (IOException e) {
      Logger.error(e.toString());
      return null;
    }
  }

  public static Resource fromJson(InputStream aInputStream) throws IOException {
    String json = IOUtils.toString(aInputStream, "UTF-8");
    aInputStream.close();
    return Resource.fromJson(json);
  }

  public ProcessingReport validate() {
    ProcessingReport report = new ListProcessingReport();
    try {
      String type = this.getAsString(JsonLdConstants.TYPE);
      if (null == type) {
        report.error(new ProcessingMessage()
            .setMessage("No type found for ".concat(this.toString()).concat(", cannot validate")));
      } else if (null != mSchemaNode) {
        JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema(mSchemaNode, "/definitions/".concat(type));
        report = schema.validate(toJson());
      } else {
        Logger.warn("No JSON schema present, validation disabled.");
      }
    } catch (ProcessingException e) {
      e.printStackTrace();
    }
    return report;
  }

  /**
   * Get a JsonNode representation of the resource.
   *
   * @return JSON JsonNode
   */
  public JsonNode toJson() {
    return new ObjectMapper().convertValue(this, JsonNode.class);
  }

  /**
   * Get an RDF representation of the resource.
   *
   * @return Model The RDF Model
   */
  public Model toModel() {

    Model model = ModelFactory.createDefaultModel();
    InputStream stream = new ByteArrayInputStream(this.toString().getBytes(StandardCharsets.UTF_8));
    RDFDataMgr.read(model, stream, Lang.JSONLD);
    return model;

  }

  /**
   * Get a JSON string representation of the resource.
   *
   * @return JSON string
   */
  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();
    String output;
    try {
      output = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(toJson());
    } catch (JsonProcessingException e) {
      output = toJson().toString();
      e.printStackTrace();
    }
    return output;
  }

  public String getAsString(final Object aKey) {
    Object result = get(aKey);
    return (result == null) ? null : result.toString();
  }

  public List<Resource> getAsList(final Object aKey) {
    List<Resource> list = new ArrayList<>();
    Object result = get(aKey);
    if (null == result || !(result instanceof List<?>)) {
      return list;
    }
    for (Object value : (List<?>) result) {
      if (value instanceof Resource) {
        list.add((Resource) value);
      }
    }
    return list;
  }

  public List<String> getIdList(final Object aKey) {
    List<String> ids = new ArrayList<>();
    Object result = get(aKey);
    if (null == result || !(result instanceof List<?>)) {
      return ids;
    }
    for (Object value : (List<?>) result) {
      if (value instanceof Resource) {
        ids.add(((Resource) value).getAsString(JsonLdConstants.ID));
      }
    }
    return ids;
  }

  public Resource getAsResource(final Object aKey) {
    Object result = get(aKey);
    return (null == result || !(result instanceof Resource)) ? null : (Resource) result;
  }


  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(final Object aOther) {
    if (!(aOther instanceof Resource)) {
      return false;
    }
    final Resource other = (Resource) aOther;
    if (other.size() != this.size()) {
      return false;
    }
    final Iterator<Map.Entry<String, Object>> thisIt = this.entrySet().iterator();
    while (thisIt.hasNext()) {
      final Map.Entry<String, Object> pair = thisIt.next();
      if (pair.getValue() instanceof List<?>) {
        if (!(other.get(pair.getKey()) instanceof List<?>)) {
          return false;
        }
        List<Object> list = (List<Object>) pair.getValue();
        List<Object> otherList = (List<Object>) other.get(pair.getKey());
        if (list.size() != otherList.size() || !list.containsAll(otherList) || !otherList.containsAll(list)) {
          return false;
        }
      } else if (!pair.getValue().equals(other.get(pair.getKey()))) {
        return false;
      }
    }
    return true;
  }

  public boolean hasId() {
    return containsKey(JsonLdConstants.ID);
  }

  public String getId() {
    return getAsString(JsonLdConstants.ID);
  }

  public String getType() {
    return getAsString(JsonLdConstants.TYPE);
  }

  public void merge(Resource aOther) {
    for (Entry<String, Object> entry : aOther.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public int compareTo(Resource aOther) {
    if (hasId() && aOther.hasId()) {
      return getAsString(JsonLdConstants.ID).compareTo(aOther.getAsString(JsonLdConstants.ID));
    }
    return toString().compareTo(aOther.toString());
  }

  /**
   * Get a flat String representation of this Resource, whereby any keys are
   * dismissed. Furthermore, value information can be dropped by specifying its
   * fields respectively keys names. This is useful for "static" values like e.
   * g. of the field "type".
   *
   * @param aFieldSeparator
   *          a String indicating the beginning of new information, resulting
   *          from a new Resource's field. Note, that this separator might also
   *          appear within the Resource's fields themselves.
   * @param aDropFields
   *          a List<String> specifying, which field's values should be excluded
   *          from the resulting String representation
   * @return a flat String representation of this Resource. In case there is no
   *         information to be returned, the result is an empty String.
   */
  public String getValuesAsFlatString(String aFieldSeparator, List<String> aDropFields) {
    String fieldSeparator = null == aFieldSeparator ? "" : aFieldSeparator;

    StringBuffer result = new StringBuffer();

    for (Entry<String, Object> entry : entrySet()) {
      if (!aDropFields.contains(entry.getKey())) {
        Object value = entry.getValue();
        if (value instanceof String) {
          if (!"".equals(value)) {
            result.append(value).append(fieldSeparator).append(" ");
          }
        } //
        else if (value instanceof Resource) {
          result.append(((Resource) value).getValuesAsFlatString(fieldSeparator, aDropFields));
        } //
        else if (value instanceof List<?>) {
          result.append("[");
          for (Object innerValue : (List<?>) value) {
            if (innerValue instanceof String) {
              if (!"".equals(innerValue)) {
                result.append(innerValue).append(fieldSeparator).append(" ");
              }
            } //
            else if (innerValue instanceof Resource) {
              result.append(
                  ((Resource) innerValue).getValuesAsFlatString(fieldSeparator, aDropFields));
            }
          }
          if (result.length() > 1 && result.charAt(result.length() - 1) != '[') {
            result.delete(result.length() - 2, result.length());
          }
          result.append("]").append(fieldSeparator).append(" ");
        }
      }
    }
    // delete last separator
    if (result.length() > 1) {
      result.delete(result.length() - 2, result.length());
    }
    return result.toString();
  }

}
