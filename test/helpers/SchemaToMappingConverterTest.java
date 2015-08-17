package helpers;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

public class SchemaToMappingConverterTest {

  @Test
  public void testCreateJsonSchema() throws IOException, ProcessingException {
    SchemaToMappingConverter converter = new SchemaToMappingConverter();
    converter.convert();
    // if everything went well...
    Assert.assertTrue(true);
  }

  
  
}
