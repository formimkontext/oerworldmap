package controllers;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import helpers.AuthTest;
import helpers.ElasticsearchTestGrid;
import helpers.JsonLdConstants;
import helpers.JsonTest;
import models.Resource;
import play.mvc.Result;

/**
 * @author fo
 */
public class ResourceIndexTest extends ElasticsearchTestGrid implements JsonTest {

  @Test
  public void createResourceFromFormUrlEncoded() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        String auth = AuthTest.getAuthString();
        Map<String, String> data = new HashMap<>();
        data.put(JsonLdConstants.TYPE, "Person");
        data.put(JsonLdConstants.ID, UUID.randomUUID().toString());
        data.put("email", "foo1@bar.com");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.create().url())
            .withHeader("Authorization", "Basic " + auth).withFormUrlEncodedBody(data));
        assertEquals(201, status(result));
      }
    });
  }

  @Test
  public void createResourceFromJson() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        String auth = AuthTest.getAuthString();
        Resource organization = getResourceFromJsonFileUnsafe("SchemaTest/testOrganization.json");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.create().url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(organization.toJson()));
        assertEquals(201, status(result));
      }
    });
  }

  @Test
  public void updateResourceFromJson() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        Resource organization = getResourceFromJsonFileUnsafe("SchemaTest/testOrganization.json");
        String auth = AuthTest.getAuthString();
        Result createResult = route(fakeRequest("POST", routes.ResourceIndex.create().url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(organization.toJson()));
        assertEquals(201, status(createResult));
        Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.update(organization.getId()).url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(organization.toJson()));
        assertEquals(200, status(updateResult));
      }
    });
  }

  @Test
  public void updatePersonFromJsonAuthorized() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        String auth = AuthTest.getAuthString();
        Resource person = new Resource("Person");
        person.put("email", Global.getConfig().getString("admin.user"));
        Result createResult = route(fakeRequest("POST", routes.ResourceIndex.create().url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(person.toJson()));
        assertEquals(201, status(createResult));
        Result updateResult = route(fakeRequest("POST", routes.UserIndex.update(person.getId()).url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(person.toJson()));
        assertEquals(200, status(updateResult));
      }
    });
  }

  @Test
  public void updatePersonFromJsonUnauthorized() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        String auth = AuthTest.getAuthString();
        Resource person = new Resource("Person");
        person.put("email", "foo@bar.de");
        Result createResult = route(fakeRequest("POST", routes.ResourceIndex.create().url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(person.toJson()));
        assertEquals(201, status(createResult));
        Result updateResult = route(fakeRequest("POST", routes.UserIndex.update(person.getId()).url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(person.toJson()));
        assertEquals(403, status(updateResult));
      }
    });
  }

  @Test
  public void updateNonexistentResourceFromJson() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        Resource organization = getResourceFromJsonFileUnsafe("SchemaTest/testOrganization.json");
        String auth = AuthTest.getAuthString();
        Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.update(organization.getId()).url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(organization.toJson()));
        assertEquals(400, status(updateResult));
      }
    });
  }

}
