package controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

public class FileController extends OERWorldMap {

  public static Result uploadFilesFromMultipartForm() throws IOException {

    File file = null;
    String extension = null;

    List<FilePart> files = request().body().asMultipartFormData().getFiles();
    if (null == files || files.isEmpty()) {
      return badRequest("No file to be uploaded");
    }
    if (files.size() > 1) {
      return badRequest("Multiple file upload not supported");
    }
    FilePart filePart = files.get(0);
    if (filePart != null) {
      String filename = filePart.getFilename();
      if (null != filename && filename.lastIndexOf(".") > 0 && filename.lastIndexOf(".") < filename.length() - 1) {
        extension = filename.substring(filename.lastIndexOf(".") + 1);
      }
      file = filePart.getFile();
    }

    if (null != file && null != extension) {
      String location = mBaseRepository.addFile(file, extension);
      String path = Paths.get("").toAbsolutePath().toString();
      response().setHeader("Location", "file://" + path + File.separator + location);
      return created("File uploaded");
    } //
    else {
      return badRequest("Failed to upload file");
    }
  }

}
