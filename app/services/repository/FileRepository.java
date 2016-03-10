package services.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;

import helpers.JsonLdConstants;
import models.Resource;

public class FileRepository extends Repository implements Writable, Readable, FileStorage {

  private TypeReference<HashMap<String, Object>> mMapType = new TypeReference<HashMap<String, Object>>() {
  };

  public FileRepository(Config aConfiguration) {
    super(aConfiguration);

  }

  private Path getPath() {
    return Paths.get(mConfiguration.getString("filerepo.dir"));
  }

  public Path getFileRoot() {
    return Paths.get(mConfiguration.getString("files.rootdir"));
  }

  /**
   * Add a new resource to the repository.
   *
   * @param aResource
   */
  @Override
  public void addResource(@Nonnull final Resource aResource, @Nonnull final String aType) throws IOException {
    String id = aResource.getAsString(JsonLdConstants.ID);
    String encodedId = DigestUtils.sha256Hex(id);
    Path dir = Paths.get(getPath().toString(), aType);
    Path file = Paths.get(dir.toString(), encodedId);
    if (!Files.exists(dir)) {
      Files.createDirectory(dir);
    }
    Files.write(file, aResource.toString().getBytes());
  }

  /**
   * Get a Resource specified by the given identifier.
   * 
   * @param aId
   * @return the Resource by the given identifier or null if no such Resource
   *         exists.
   */
  @Override
  public Resource getResource(@Nonnull String aId) {
    ObjectMapper objectMapper = new ObjectMapper();
    Path resourceFile;
    try {
      resourceFile = getResourcePath(aId);
      Map<String, Object> resourceMap = objectMapper.readValue(resourceFile.toFile(), mMapType);
      return Resource.fromMap(resourceMap);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Query all resources of a given type.
   *
   * @param aType
   *          The type of the resources to get
   * @return All resources of the given type as a List.
   */
  @Override
  public List<Resource> getAll(@Nonnull String aType) {
    ArrayList<Resource> results = new ArrayList<>();
    Path typeDir = Paths.get(getPath().toString(), aType);
    DirectoryStream<Path> resourceFiles;
    try {
      resourceFiles = Files.newDirectoryStream(typeDir);
    } catch (IOException ex) {
      ex.printStackTrace();
      return results;
    }
    ObjectMapper objectMapper = new ObjectMapper();
    for (Path resourceFile : resourceFiles) {
      Map<String, Object> resourceMap;
      try {
        resourceMap = objectMapper.readValue(resourceFile.toFile(), mMapType);
      } catch (IOException ex) {
        ex.printStackTrace();
        continue;
      }
      results.add(Resource.fromMap(resourceMap));
    }
    return results;
  }

  /**
   * Delete a Resource specified by the given identifier.
   * 
   * @param aId
   * @return The resource that has been deleted.
   */
  @Override
  public Resource deleteResource(@Nonnull String aId) {
    Resource resource = this.getResource(aId);
    try {
      Files.delete(getResourcePath(aId));
    } catch (IOException e) {
      return null;
    }
    return resource;
  }

  private Path getResourcePath(@Nonnull final String aId) throws IOException {
    String encodedId = DigestUtils.sha256Hex(aId);
    DirectoryStream<Path> typeDirs = Files.newDirectoryStream(getPath(), new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path entry) throws IOException {
        return Files.isDirectory(entry);
      }
    });

    for (Path typeDir : typeDirs) {
      DirectoryStream<Path> resourceFiles = Files.newDirectoryStream(typeDir, new DirectoryStream.Filter<Path>() {
        @Override
        public boolean accept(Path entry) throws IOException {
          return (entry.getFileName().toString().equals(encodedId));
        }
      });
      for (Path resourceFile : resourceFiles) {
        return resourceFile;
      }
    }

    throw new IOException(aId + " not found.");

  }

  @Override
  public String addFile(@Nonnull File aFile, @Nonnull String aExtension) throws IOException {

    File dir = new File(getFileRoot().toString() + File.separator + aExtension.toLowerCase());
    if (!dir.exists()) {
      dir.mkdir();
    }

    String targetPath = dir + File.separator + generateId() + "." + aExtension.toLowerCase();
    new File(targetPath).createNewFile();
    FileOutputStream out = new FileOutputStream(targetPath);
    Files.copy(aFile.toPath(), out);

    return targetPath;
  }

  @Override
  public File getFile(String aName, String aContentType) throws IOException {
    return getFile(getFileRoot().toString() + File.separator);
  }

  public File getFile(String aFullQualifiedPath) throws IOException {
    File file = new File(aFullQualifiedPath);
    if (file.exists() && file.canRead()) {
      return file;
    }
    return null;
  }

  @Override
  public String deleteFile(String aPath) throws FileNotFoundException, IllegalAccessException {
    String result;
    File toBeDeleted = new File(aPath);
    if (!toBeDeleted.exists()) {
      throw new FileNotFoundException("File does not exist: " + aPath);
    } //
    else if (toBeDeleted.canWrite()) {
      throw new IllegalAccessException("File can not be deleted: " + aPath);
    } //
    else {
      toBeDeleted.delete();
      result = "File was deleted: " + aPath;
    }
    return result;
  }

  private static String generateId() {
    return UUID.randomUUID().toString();
  }

}
