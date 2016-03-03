package services.repository;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * @author pvb
 */
public interface FileStorage {

  /**
   * Add a new file to the repository.
   * 
   * @param aFile
   *          The file to be added
   * @param aExtension
   *          The extension of the file.
   * @return The path of the file added to the repository.
   */
  String addFile(@Nonnull File aFile, @Nonnull String aExtension) throws IOException;

  /**
   * Get a file from the permanent repository.
   *
   * @param aName
   *          The name to the file to be returned.
   * @param aContentType
   *          The content Type to the file to be returned.
   * @return The file that has been found or null if a such file has not been
   *         found.
   */
  File getFile(String aName, String aContentType) throws IOException;

  /**
   * Deletes a file from the permanent repository.
   *
   * @param aPath
   *          The path to the file to be deleted.
   * @return a String providing information about the success of the deletion.
   */
  String deleteFile(@Nonnull String aPath) throws IOException;

}
