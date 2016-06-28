package org.craftercms.search.batch.impl;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.exception.BatchIndexingException;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 2/6/16.
 */
public abstract class AbstractBatchIndexer implements BatchIndexer {

    private static final Log logger = LogFactory.getLog(AbstractBatchIndexer.class);

    protected SearchService searchService;
    protected List<String> includeFileNamePatterns;
    protected List<String> excludeFileNamePatterns;

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setIncludeFileNamePatterns(List<String> includeFileNamePatterns) {
        this.includeFileNamePatterns = includeFileNamePatterns;
    }

    public void setExcludeFileNamePatterns(List<String> excludeFileNamePatterns) {
        this.excludeFileNamePatterns = excludeFileNamePatterns;
    }

    @Override
    public int updateIndex(String indexId, String siteName, String rootFolder, List<String> fileNames,
                           boolean delete) throws BatchIndexingException {
        int updateCount = 0;

        for (String fileName : fileNames) {
            if (include(fileName) && doSingleFileUpdate(indexId, siteName, rootFolder, fileName, delete)) {
                updateCount++;
            }
        }

        if (delete) {
            logger.info(updateCount + " files deleted from index " + getIndexNameStr(indexId));
        } else {
            logger.info(updateCount + " files added to index " + getIndexNameStr(indexId));
        }

        return updateCount;
    }

    protected boolean doUpdate(String indexId, String siteName, String id, File file,
                               String xml) throws SearchException {
        try {
            searchService.update(indexId, siteName, id, xml, true);

            logger.info("File " + file + " added to index " + getIndexNameStr(indexId));

            return true;
        } catch (SearchException e) {
            logger.error("Error while adding file " + file + " to index " + getIndexNameStr(indexId), e);

            return false;
        }
    }

    protected boolean doUpdateFile(String indexId, String siteName, String id, File file) throws SearchException {
        try {
            searchService.updateFile(indexId, siteName, id, file);

            logger.info("File " + file + " added to index " + getIndexNameStr(indexId));

            return true;
        } catch (SearchException e) {
            logger.error("Error while adding file " + file + " to index " + getIndexNameStr(indexId), e);

            return false;
        }
    }

    protected boolean doUpdateFile(String indexId, String siteName, String id, File file,
                                   Map<String, List<String>> additionalFields) throws SearchException {
        try {
            searchService.updateFile(indexId, siteName, id, file, additionalFields);

            logger.info("File " + file + " added to index " + getIndexNameStr(indexId));

            return true;
        } catch (SearchException e) {
            logger.error("Error while adding file " + file + " to index " + getIndexNameStr(indexId), e);

            return false;
        }
    }

    protected boolean doDelete(String indexId, String siteName, String id, File file) throws SearchException {
        try {
            searchService.delete(indexId, siteName, id);

            logger.info("File " + file + " deleted from index " + getIndexNameStr(indexId));

            return true;
        } catch (SearchException e) {
            logger.error("Error while deleting file " + file + " from index " + getIndexNameStr(indexId), e);

            return false;
        }
    }

    protected String getIndexNameStr(String indexId) {
        return StringUtils.isNotEmpty(indexId)? "'" + indexId + "'": "default";
    }

    protected boolean include(String fileName) {
        boolean update = true;

        if (CollectionUtils.isNotEmpty(includeFileNamePatterns) &&
            !RegexUtils.matchesAny(fileName, includeFileNamePatterns)) {
            update = false;
        }
        if (CollectionUtils.isNotEmpty(excludeFileNamePatterns) &&
            RegexUtils.matchesAny(fileName, excludeFileNamePatterns)) {
            update = false;
        }

        return update;
    }

    protected abstract boolean doSingleFileUpdate(String indexId, String siteName, String rootFolder,
                                                  String fileName, boolean delete) throws BatchIndexingException;

}