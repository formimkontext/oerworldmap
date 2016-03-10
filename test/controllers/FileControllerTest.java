package controllers;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

import org.junit.Test;

import helpers.AuthTest;
import helpers.ElasticsearchTestGrid;
import helpers.JsonTest;
import play.mvc.Result;

public class FileControllerTest extends ElasticsearchTestGrid implements JsonTest {

  @Test
  public void createResourceFromJson() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        String auth = AuthTest.getAuthString();
        Result data = route(
            fakeRequest(routes.FileController.uploadFilesFromMultipartForm()).header("Accept", "application/json"));
      }
    });
  }

}
