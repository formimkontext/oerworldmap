package controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import helpers.JSONForm;
import models.Resource;
import play.mvc.Result;

public class FileController extends OERWorldMap {

  public static Result uploadPictureFromMultipartForm() throws IOException {

    play.mvc.Http.MultipartFormData body = request().body().asMultipartFormData();
    play.mvc.Http.MultipartFormData.FilePart picture = null;
    if (null != body) {
      picture = body.getFile("picture");
    }
    if (picture != null) {
      String extension = picture.getFilename().substring(picture.getFilename().lastIndexOf("."));
      File file = picture.getFile();
      mBaseRepository.addFile(file, extension);
      return ok("File uploaded");
    } else {
      flash("error", "Missing file");
      Resource contextResource = getResource();
      return badRequest(render("Create failed", "feedback.mustache", getPictureScope(contextResource),
          getErrorMessages(contextResource)));
    }
  }

  private static Resource getResource() {
    Map<String, String[]> formUrlEncoded = request().body().asFormUrlEncoded();
    if (null == formUrlEncoded) {
      return null;
    }
    JsonNode json = JSONForm.parseFormData(formUrlEncoded, true);
    Resource resource = null;
    if (null != json) {
      resource = Resource.fromJson(json);
    }
    return resource;
  }

  private static Map<String, Object> getPictureScope(Resource aResource) {
    Map<String, Object> scope = new HashMap<>();
    if (null != aResource) {
      scope.put("resource", aResource);
    }
    return scope;
  }

  private static List<Map<String, Object>> getErrorMessages(Resource aResource) {
    List<Map<String, Object>> messages = new ArrayList<>();
    Map<String, Object> message = new HashMap<>();
    message.put("level", "warning");
    message.put("message", OERWorldMap.messages.getString("upload_error") + "<pre>" + aResource + "</pre>");
    messages.add(message);
    return messages;
  }

}
