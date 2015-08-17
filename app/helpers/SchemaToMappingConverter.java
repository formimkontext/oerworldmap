package helpers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;

/**
 * Takes a JSON schema as input and outputs a JSON format that is applicable as
 * an Elasticsearch mapping.
 * 
 * @author pvb, fo
 *
 */
public class SchemaToMappingConverter {

  private URL mSchemaUrl;
  private JsonSchema mJsonSchema;
  private JsonNode mJsonNode;

  public void convert() throws IOException, ProcessingException {
    
    mSchemaUrl = getSchemaURL();
    mJsonNode = getJsonNode(FilesConfig.getSchema());
    Iterator<JsonNode> iterator = mJsonNode.elements();
    while (iterator.hasNext()){
      Object element = iterator.next();
      if (element instanceof JSONObject){
        
      }
    }
  }

  private URL getSchemaURL() throws IOException, ProcessingException {
    final File schemaFile = new File(FilesConfig.getSchema());
    URL url = new URL("file://" + schemaFile.getAbsolutePath());
    return url;
  }

  private JsonNode getJsonNode(String aFileName) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(aFileName);     // TODO replace by common method after merge
    String json = IOUtils.toString(in, "UTF-8");
    return new ObjectMapper().readTree(json);
  }

}
