package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import helpers.ElasticsearchTestGrid;
import helpers.JsonLdConstants;
import helpers.JsonTest;
import models.Resource;

public class BaseRepositoryTest extends ElasticsearchTestGrid implements JsonTest {

  @Test
  public void testResourceWithIdentifiedSubObject() throws IOException {
    Resource resource = new Resource("Person", "info:id001");
    resource.put(JsonLdConstants.CONTEXT, "http://schema.org/");
    String property = "attended";
    resource.put(property, new Resource("Event", "info:OER15"));
    Resource expected1 = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.1.json");
    Resource expected2 = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.2.json");
    mBaseRepo.addResource(resource, new HashMap<>());
    Assert.assertEquals(expected1, mBaseRepo.getResource("info:id001"));
    Assert.assertEquals(expected2, mBaseRepo.getResource("info:OER15"));
  }

  @Test
  public void testResourceWithUnidentifiedSubObject() throws IOException {
    Resource resource = new Resource("Person", "info:id002");
    resource.put(JsonLdConstants.CONTEXT, "http://schema.org/");
    Resource value = new Resource("Foo", null);
    resource.put("attended", value);
    Resource expected = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithUnidentifiedSubObject.OUT.1.json");
    mBaseRepo.addResource(resource, new HashMap<>());
    Assert.assertEquals(expected, mBaseRepo.getResource("info:id002"));
  }

  @Test
  public void testDeleteResourceWithMentionedResources() throws IOException, InterruptedException {
    // setup: 1 Person ("in1") who has 2 affiliations
    Resource in = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceWithMentionedResources.IN.1.json");
    Resource expected1 = getResourceFromJsonFile(
        "BaseRepositoryTest/testDeleteResourceWithMentionedResources.OUT.1.json");
    Resource expected2 = getResourceFromJsonFile(
        "BaseRepositoryTest/testDeleteResourceWithMentionedResources.OUT.2.json");

    mBaseRepo.addResource(in, new HashMap<>());
    // delete affiliation "Oh No Company" and check whether it has been removed
    // from referencing resources
    Resource toBeDeleted = mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70987");
    // FIXME: Thread.sleep to be deleted when Repo synchronization is
    // triggerable
    Thread.sleep(1000);
    mBaseRepo.deleteResource(toBeDeleted.getAsString(JsonLdConstants.ID));
    Resource result1 = mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70456");
    Resource result2 = mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70123");
    Assert.assertEquals(expected1, result1);
    Assert.assertEquals(expected2, result2);
    Assert.assertNull(mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70987"));
  }

  @Test
  public void testDeleteLastResourceInList() throws IOException, InterruptedException {
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteLastResourceInList.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteLastResourceInList.DB.2.json");
    Resource out = getResourceFromJsonFile("BaseRepositoryTest/testDeleteLastResourceInList.OUT.1.json");
    mBaseRepo.addResource(db1, new HashMap<>());
    mBaseRepo.addResource(db2, new HashMap<>());
    // FIXME: Thread.sleep to be deleted when Repo synchronization is
    // triggerable
    Thread.sleep(1000);
    mBaseRepo.deleteResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665");
    Assert.assertNull(mBaseRepo.getResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665"));
    Assert.assertEquals(out, mBaseRepo.getResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e25503"));
  }

  @Test
  public void testDeleteResourceFromList() throws IOException, InterruptedException {
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.DB.2.json");
    Resource db3 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.DB.2.json");
    Resource out1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.OUT.2.json");
    mBaseRepo.addResource(db1, new HashMap<>());
    mBaseRepo.addResource(db2, new HashMap<>());
    mBaseRepo.addResource(db3, new HashMap<>());
    // FIXME: Thread.sleep to be deleted when Repo synchronization is
    // triggerable
    Thread.sleep(1000);
    mBaseRepo.deleteResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665");
    Assert.assertNull(mBaseRepo.getResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665"));
    Assert.assertEquals(out1, mBaseRepo.getResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e25503"));
    Assert.assertEquals(out2, mBaseRepo.getResource("urn:uuid:7cfb9aab-1a3f-494c-8fb1-64755faf180c"));
  }

  @Test
  public void testRemoveReference() throws IOException {
    Resource in = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.IN.json");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.DB.2.json");
    Resource out1 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.OUT.2.json");
    mBaseRepo.addResource(db1, new HashMap<>());
    mBaseRepo.addResource(db2, new HashMap<>());
    mBaseRepo.addResource(in, new HashMap<>());
    Resource get1 = mBaseRepo.getResource(out1.getAsString(JsonLdConstants.ID));
    Resource get2 = mBaseRepo.getResource(out2.getAsString(JsonLdConstants.ID));
    assertEquals(out1, get1);
    assertEquals(out2, get2);
  }

  @Test
  public void testChangeResourceTypeSimple() throws IOException {
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testChangeResourceTypeSimple.DB.1.json");
    Resource update1 = getResourceFromJsonFile("BaseRepositoryTest/testChangeResourceTypeSimple.IN.1.json");
    assertEquals(db1.getId(), update1.getId());
    assertNotEquals(db1.getAsString(JsonLdConstants.TYPE), update1.getAsString(JsonLdConstants.TYPE));
    mBaseRepo.addResource(db1, new HashMap<>());
    mBaseRepo.addResource(update1, new HashMap<>());
    Resource get1 = mBaseRepo.getResource(update1.getAsString(JsonLdConstants.ID));
    assertEquals(update1, get1);
  }

  @Test
  public void testChangeResourceTypeMismatch() throws IOException {
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testChangeResourceTypeMismatch.DB.1.json");
    mBaseRepo.addResource(db1, new HashMap<>());
    // change type:
    db1.put(JsonLdConstants.TYPE, "Organization");
    // try to update changed resource
    mBaseRepo.validateAndAdd(db1, new HashMap<>());
    // resource should not be updated due to data model mismatch: Organization
    // may not have a provider
    Resource get1 = mBaseRepo.getResource(db1.getId());
    assertEquals("Service", get1.getAsString(JsonLdConstants.TYPE));
  }

}
