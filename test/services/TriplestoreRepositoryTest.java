package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import helpers.JsonTest;
import models.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;
import services.repository.TriplestoreRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * Created by fo on 10.12.15.
 */
public class TriplestoreRepositoryTest implements JsonTest {

  private Config config = ConfigFactory.load(ClassLoader.getSystemClassLoader(), "test.conf");

  @Test
  public void testAddResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");

    Model expected = ModelFactory.createDefaultModel();
    RDFDataMgr.read(expected, "TriplestoreRepositoryTest/testAddResource.IN.1.nt", Lang.NTRIPLES);

    assertTrue(actual.isIsomorphicWith(expected));

  }

  @Test
  public void testAddResourceWithReferences() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource resource3 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResourceWithReferences.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");
    triplestoreRepository.addResource(resource3, "Person");

    Model expected = ModelFactory.createDefaultModel();
    RDFDataMgr.read(expected, "TriplestoreRepositoryTest/testAddResourceWithReferences.IN.1.nt", Lang.NTRIPLES);

    assertTrue(actual.isIsomorphicWith(expected));

  }

  @Test
  public void testUpdateResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource update1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testUpdateResource.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");
    triplestoreRepository.addResource(update1, "Person");

    Model expected = ModelFactory.createDefaultModel();
    RDFDataMgr.read(expected, "TriplestoreRepositoryTest/testUpdateResource.IN.1.nt", Lang.NTRIPLES);

    assertTrue(actual.isIsomorphicWith(expected));

  }

  @Test
  public void testUpdateResourceWithReferences() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource resource3 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResourceWithReferences.IN.1.json");
    Resource update1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testUpdateResourceWithReferences.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");
    triplestoreRepository.addResource(resource3, "Person");
    triplestoreRepository.addResource(update1, "Person");

    Model expected = ModelFactory.createDefaultModel();
    RDFDataMgr.read(expected, "TriplestoreRepositoryTest/testUpdateResourceWithReferences.IN.1.nt", Lang.NTRIPLES);

    assertTrue(actual.isIsomorphicWith(expected));

  }

  @Test
  public void testGetResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");

    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config);
    triplestoreRepository.addResource(resource1, "Person");

    Resource back = triplestoreRepository.getResource(resource1.getId());
    // FIXME: remove when proper @context is returned
    resource1.remove("@context");
    assertEquals(resource1, back);

  }

  @Test
  public void testGetUpdatedResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource resource3 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResourceWithReferences.IN.1.json");
    Resource update1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testUpdateResourceWithReferences.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");
    triplestoreRepository.addResource(resource3, "Person");
    triplestoreRepository.addResource(update1, "Person");

    Resource expected = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testGetUpdatedResource.OUT.1.json");
    Resource back = triplestoreRepository.getResource(resource1.getId());

    assertEquals(expected, back);

  }

  @Test
  public void testCheckoutResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource update1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testUpdateResource.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");
    TriplestoreRepository.Diff diff = triplestoreRepository.addResource(update1);

    Resource back = triplestoreRepository.checkoutResource(resource1.getId(), diff);

    // FIXME: remove when proper @context is returned
    resource1.remove("@context");
    assertEquals(resource1, back);

  }

  @Test
  public void testIndexResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource update1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResourceWithReferences.IN.1.json");
    Resource update2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testUpdateResourceWithReferences.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);

    TriplestoreRepository.Diff diff1 = triplestoreRepository.addResource(resource1);
    List<String> index1 = triplestoreRepository.index(diff1);
    assertEquals(1, index1.size());
    assertTrue(index1.contains("info:alice"));

    TriplestoreRepository.Diff diff2 = triplestoreRepository.addResource(resource2);
    List<String> index2 = triplestoreRepository.index(diff2);
    assertEquals(1, index2.size());
    assertTrue(index2.contains("info:bob"));

    TriplestoreRepository.Diff diff3 = triplestoreRepository.addResource(update1);
    List<String> index3 = triplestoreRepository.index(diff3);
    assertEquals(3, index3.size());
    assertTrue(index3.contains("info:alice"));
    assertTrue(index3.contains("info:carol"));
    assertTrue(index3.contains("info:bob"));

    TriplestoreRepository.Diff diff4 = triplestoreRepository.addResource(update2);
    List<String> index4 = triplestoreRepository.index(diff4);
    assertEquals(3, index4.size());
    assertTrue(index4.contains("info:alice"));
    assertTrue(index4.contains("info:carol"));
    assertTrue(index4.contains("info:bob"));

  }


  @Test
  public void testGetLog() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource update1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testUpdateResource.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");

    triplestoreRepository.getLog(resource1.getId());

  }

}