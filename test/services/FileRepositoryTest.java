package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import helpers.ElasticsearchTestGrid;
import helpers.UniversalFunctions;
import models.Resource;
import services.repository.FileRepository;

public class FileRepositoryTest extends ElasticsearchTestGrid {

  private static FileRepository resourceRepository;
  private static Resource resource;

  @BeforeClass
  public static void setUpDir() throws IOException {
    UniversalFunctions.deleteDirectory(new File(mConfig.getString("filerepo.dir").concat("/Person")));
    resourceRepository = new FileRepository(mConfig);
    resource = new Resource("Person", "1");
    resource.put("name", "John Doe");
    resourceRepository.addResource(resource, "Person");
  }

  @Test
  public void testGetResource() throws IOException {
    Resource fromStore = resourceRepository.getResource("1");
    assertTrue(resource.equals(fromStore));
  }

  @Test
  public void testGetAll() throws IOException {
    List<Resource> results = resourceRepository.getAll("Person");
    assertEquals(results.size(), 1);
  }

  @Test
  public void testAddFile() throws IOException {
    URL url = new URL("https://oerworldmap.files.wordpress.com/2015/03/rob.jpeg?w=128&h=127");
    BufferedImage img = ImageIO.read(url);
    File file = new File("downloaded_testfile.jpg");
    if (!file.exists()) {
      file.createNewFile();
    }
    ImageIO.write(img, "jpg", file);
    String fullPath = resourceRepository.addFile(file, "JPG");
    String fileID = fullPath.substring(fullPath.lastIndexOf("/"));
    File check = new File(
        resourceRepository.getFileRoot().toString() + File.separator + "JPG".toLowerCase() + File.separator + fileID);
    Assert.assertTrue(check.exists());
    Assert.assertTrue(check.canRead());
  }

  @AfterClass
  public static void tearDownDir() throws IOException {
    UniversalFunctions.deleteDirectory(new File(mConfig.getString("filerepo.dir").concat("/Person")));
  }

}
