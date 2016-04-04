package controllers;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import helpers.AuthTest;
import helpers.ElasticsearchTestGrid;
import helpers.JsonTest;
import play.api.libs.Files.TemporaryFile;
import play.api.mvc.AnyContentAsMultipartFormData;
import play.api.mvc.MultipartFormData;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.FakeRequest;

public class FileControllerTest extends ElasticsearchTestGrid implements JsonTest {

  private FakeApplication mApp;

  @Before
  public void setUp() {
    mApp = fakeApplication();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void createResourceFromJson() {

    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {

        // setup test
        URL url = mApp.getWrappedApplication().resource("valid_import.pptx").get();
        File file = null;
        try {
          file = new File(url.toURI());
        } catch (URISyntaxException e) {
          e.printStackTrace();
        }
        TemporaryFile temporaryFile = new TemporaryFile(file);
        MultipartFormData.FilePart<?> filePath = new MultipartFormData.FilePart("file", "valid_import.pptx",
            new scala.Some<>("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            temporaryFile);
        List<MultipartFormData.FilePart<?>> fileParts = new ArrayList<>();
        fileParts.add(filePath);
        scala.collection.immutable.Seq<?> files = scala.collection.JavaConversions.asScalaBuffer(fileParts).toList();

        MultipartFormData formData = new MultipartFormData(null, files, null, null);
        AnyContentAsMultipartFormData body = new AnyContentAsMultipartFormData(formData);

        // run test
        String auth = AuthTest.getAuthString();
        FakeRequest fakeRequest = fakeRequest("POST", routes.FileController.uploadFilesFromMultipartForm().url())
            .withBody(body).withHeader("Authorization", "Basic " + auth);
        Result result = route(fakeRequest);

        assertEquals(201, status(result));

      }
    });

  }

}
