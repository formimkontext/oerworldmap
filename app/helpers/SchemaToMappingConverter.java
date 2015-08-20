package helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jdk.nashorn.internal.objects.Global;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import play.api.Play;
import scala.Option;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.processing.Processor;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonSchemaFactoryBuilder;
import com.github.fge.jsonschema.processors.data.FullData;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.sun.glass.ui.Application;

/**
 * Takes a JSON schema as input and outputs a JSON format that is applicable as
 * an Elasticsearch mapping.
 * 
 * @author pvb, fo
 *
 */
public class SchemaToMappingConverter {

  // private URL mSchemaUrl;
  // private JsonSchema mJsonSchema;
  private List<String> mMainEntitees = Arrays.asList(new String[] { "Organization", "Event",
      "Person", "Action", "WebPage", "Article", "Service" }); // TODO: unify
                                                              // with
                                                              // Resource.mIdentifiedTypes
  private JsonNode mJsonSchemaNode;
  private Map<String, JsonNode> mMainSchemaEntitees;
  private JsonNode mJsonNode;

  // private JsonSchemaFactoryBuilder mFactoryBuilder;
  // private JsonSchemaFactory mFactory;

  public void convert() throws IOException, ProcessingException, ParseException {

    // mSchemaUrl = getSchemaURL();
    mJsonSchemaNode = getJsonNode(FilesConfig.getSchema());
    
    mMainSchemaEntitees = new HashMap<>();
    for (String mainEntity : mMainEntitees) {
      JsonNode definitionsNode = mJsonSchemaNode.get("definitions").get(mainEntity);
      System.out.println(definitionsNode);
      mMainSchemaEntitees.put(mainEntity, mJsonSchemaNode.get(mainEntity));
    }
    // mFactory = JsonSchemaFactory.byDefault();
    // mJsonSchema = mFactory.getJsonSchema(mJsonSchemaNode);
    // Processor<FullData, FullData> processor = mFactory.getProcessor();
  }

  private URL getSchemaURL() throws IOException, ProcessingException {
    final File schemaFile = new File(FilesConfig.getSchema());
    URL url = new URL("file://" + schemaFile.getAbsolutePath());
    return url;
  }

  private JsonNode getJsonNode(String aFileName) throws IOException, ParseException {
    ObjectMapper mapper = new ObjectMapper();
    BufferedReader bufferedReader = new BufferedReader(
      new FileReader(new File(aFileName).getAbsolutePath()));
    return mapper.readTree(bufferedReader);
  }

}
