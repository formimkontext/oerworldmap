package services.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.typesafe.config.Config;

import helpers.JsonLdConstants;
import helpers.UniversalFunctions;
import models.Record;
import models.Resource;
import models.ResourceList;
import play.Logger;
import services.ResourceDenormalizer;

public class BaseRepository extends Repository
    implements Readable, Writable, Queryable, Aggregatable {

  private static ElasticsearchRepository mElasticsearchRepo;
  private static FileRepository mFileRepo;

  public BaseRepository(Config aConfiguration) {
    super(aConfiguration);
    mElasticsearchRepo = new ElasticsearchRepository(aConfiguration);
    mFileRepo = new FileRepository(aConfiguration);
  }

  @Override
  public Resource deleteResource(@Nonnull String aId) throws IOException {
    Resource resource = getResource(aId);
    if (null == resource) {
      return null;
    }
    int level = 0;
    List<Resource> innerResources = getInnerResources(resource, level);
    for (Resource innerResource : innerResources) {
      Resource repoResource = getRepoEntry(innerResource);
      if (null != repoResource) {
        deleteReferences(repoResource, level + 1, aId, repoResource);
      }
    }
    // finally delete the resource itself
    mElasticsearchRepo.deleteResource(aId + "." + Record.RESOURCEKEY);
    return mFileRepo.deleteResource(aId + "." + Record.RESOURCEKEY);
  }

  private void deleteReferences(Resource aResource, int aLevel, String aId, Resource aStoreResource)
      throws IOException {
    for (Iterator<Map.Entry<String, Object>> it = aResource.entrySet().iterator(); it.hasNext();) {
      Map.Entry<String, Object> entry = it.next();
      if (entry.getValue() instanceof Resource) {
        Resource innerResource = (Resource) entry.getValue();
        if (innerResource.getAsString(JsonLdConstants.ID).equals(aId)) {
          it.remove();
          postRemoveFirstLevelReferenceEntry(aResource, aLevel, aId);
          reAddResource(aStoreResource);
        } //
        else if (aLevel > 1) {
          deleteReferences(innerResource, aLevel, aId, aResource);
        }
      } //
      else if (entry.getValue() instanceof List<?>) {
        List<?> list = (List<?>) entry.getValue();
        for (Object item : list) {
          if (item instanceof Resource) {
            Resource innerResource = (Resource) item;
            if (innerResource.getAsString(JsonLdConstants.ID).equals(aId)) {
              list.remove(item);
              if (list.isEmpty()) {
                it.remove();
              }
              postRemoveFirstLevelReferenceEntry(aResource, aLevel, aId);
              reAddResource(aStoreResource);
            } //
            else if (aLevel > 1) {
              deleteReferences(innerResource, aLevel, aId, aResource);
            }
          }
        }
      }
    }
  }

  private void postRemoveFirstLevelReferenceEntry(Resource aResource, int aLevel, String aId)
      throws IOException {
    List<Resource> otherInnerResources = getInnerResources(aResource, aLevel);
    for (Resource otherInnerResource : otherInnerResources) {
      Resource repoResource = getRepoEntry(otherInnerResource);
      if (null != repoResource) {
        deleteReferences(repoResource, aLevel + 1, aId, aResource);
      }
    }
  }

  private void reAddResource(Resource aResource) throws IOException {
    String type = aResource.getAsString(JsonLdConstants.TYPE);
    addResource(getRecord(aResource), type);
  }

  private List<Resource> getInnerResources(Resource aResource, int aLevel) {
    List<Resource> result = new ArrayList<>();
    if (aLevel > 1) {
      return result;
    }
    for (Entry<String, Object> entry : aResource.entrySet()) {
      if (entry.getValue() instanceof Resource) {
        result.add((Resource) entry.getValue());
      } else if (entry.getValue() instanceof List<?>) {
        List<?> innerList = (List<?>) entry.getValue();
        for (Object item : innerList) {
          if (item instanceof Resource) {
            result.add((Resource) item);
          }
        }
      }
    }
    return result;
  }

  private Resource getRepoEntry(Resource aResourceStub) {
    String id = aResourceStub.getAsString(JsonLdConstants.ID);
    if (null == id) {
      return null;
    } else {
      return getResource(id);
    }
  }

  public void addResource(Resource aResource) throws IOException {
    List<Resource> denormalizedResources = ResourceDenormalizer.denormalize(aResource, this);
    for (Resource dnr : denormalizedResources) {
      if (dnr.hasId()) {
        Resource rec = getRecord(dnr);
        // Extract the type from the resource, otherwise everything will be
        // typed WebPage!
        String type = dnr.getAsString(JsonLdConstants.TYPE);
        addResource(rec, type);
      }
    }
  }

  @Override
  public void addResource(@Nonnull Resource aResource, @Nonnull String aType) throws IOException {
    mElasticsearchRepo.addResource(aResource, aType);
    // FIXME: As is the case for getResource, this may result in too many open
    // files
    // mFileRepo.addResource(aResource, aType);
  }

  public ProcessingReport validateAndAdd(Resource aResource) throws IOException {
    List<Resource> denormalizedResources = ResourceDenormalizer.denormalize(aResource, this);
    ProcessingReport processingReport = new ListProcessingReport();
    for (Resource dnr : denormalizedResources) {
      try {
        processingReport.mergeWith(dnr.validate());
      } catch (ProcessingException e) {
        Logger.error(e.toString());
      }
    }
    if (!processingReport.isSuccess()) {
      return processingReport;
    }
    for (Resource dnr : denormalizedResources) {
      if (dnr.hasId()) {
        Resource rec = getRecord(dnr);
        // Extract the type from the resource, otherwise everything will be
        // typed WebPage!
        String type = dnr.getAsString(JsonLdConstants.TYPE);
        addResource(rec, type);
      }
    }
    return processingReport;
  }

  @Override
  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
      Map<String, ArrayList<String>> aFilters) {
    ResourceList resourceList;
    try {
      resourceList = mElasticsearchRepo.query(aQueryString, aFrom, aSize, aSortOrder, aFilters);
    } catch (IOException | ParseException e) {
      Logger.error(e.toString());
      return null;
    }
    // set this manually so that the filteredQueryString does not become visible
    resourceList.setSearchTerms(aQueryString);
    // members are Records, unwrap to plain Resources
    List<Resource> resources = new ArrayList<>();
    resources.addAll(getResources(resourceList.getItems()));
    resourceList.setItems(resources);
    return resourceList;
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    Resource resource = mElasticsearchRepo.getResource(aId + "." + Record.RESOURCEKEY);
    if (resource == null || resource.isEmpty()) {
      // FIXME: This may lead to inconsistencies (too many open files) when ES
      // and FS are out of sync
      // resource = mFileRepo.getResource(aId + "." + Record.RESOURCEKEY);
    }
    if (resource != null) {
      resource = (Resource) resource.get(Record.RESOURCEKEY);
    }
    return resource;
  }

  private Resource getRecord(Resource aResource) {
    String id = (String) aResource.get(JsonLdConstants.ID);
    Resource record;
    if (null != id) {
      record = getRecordFromRepo(id);
      if (null == record) {
        record = new Record(aResource);
      } else {
        record.put("dateModified", UniversalFunctions.getCurrentTime());
        record.put(Record.RESOURCEKEY, aResource);
      }
    } else {
      record = new Record(aResource);
    }
    return record;
  }

  private Resource getRecordFromRepo(String aId) {
    Resource record = mElasticsearchRepo.getResource(aId + "." + Record.RESOURCEKEY);
    if (record == null || record.isEmpty()) {
      record = mFileRepo.getResource(aId + "." + Record.RESOURCEKEY);
    }
    return record;
  }

  private List<Resource> getResources(List<Resource> aRecords) {
    List<Resource> resources = new ArrayList<Resource>();
    for (Resource rec : aRecords) {
      resources.add((Resource) rec.get(Record.RESOURCEKEY));
    }
    return resources;
  }

  @Override
  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder) throws IOException {
    return mElasticsearchRepo.aggregate(aAggregationBuilder);
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) {
    List<Resource> resources = new ArrayList<Resource>();
    try {
      resources = mElasticsearchRepo.getAll(aType);
    } catch (IOException e) {
      Logger.error(e.toString());
    }
    return resources;
  }

}
