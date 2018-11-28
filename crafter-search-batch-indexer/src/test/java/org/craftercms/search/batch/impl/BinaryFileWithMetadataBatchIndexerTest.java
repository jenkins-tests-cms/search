package org.craftercms.search.batch.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.craftercms.core.service.Content;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.rest.v3.requests.SearchRequest;
import org.craftercms.search.rest.v3.requests.SearchResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BinaryFileWithMetadataBatchIndexer}.
 *
 * @author avasquez
 */
public class BinaryFileWithMetadataBatchIndexerTest extends BatchIndexerTestBase {

    private static final String SITE_NAME = "test";
    private static final String INDEX_ID = SITE_NAME;
    private static final String METADATA_XML_FILENAME = "metadata.xml";
    private static final String METADATA_WITH_REMOVED_BINARIES_XML_FILENAME = "metadata-with-removed-binaries.xml";
    private static final String BINARY_FILENAME1 = "crafter-wp-7-reasons.pdf";
    private static final String BINARY_FILENAME2 = "crafter-wp-wem-v2.pdf";
    private static final String BINARY_FILENAME3 = "notes.txt";

    private BinaryFileWithMetadataBatchIndexer batchIndexer;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        batchIndexer = getBatchIndexer();
    }

    @Test
    public void testUpdateMetadata() {
        setupBinariesSearchResults();

        UpdateSet updateSet = new UpdateSet(Collections.singletonList(METADATA_XML_FILENAME), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(3, updateStatus.getAttemptedUpdatesAndDeletes());
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME1));
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME2));
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME3));
        verify(searchService).updateContent(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(Content.class), eq(getExpectedMetadata()));
        verify(searchService).updateContent(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME2), any(Content.class), eq(getExpectedMetadata()));
        verify(searchService).updateContent(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME3), any(Content.class), eq(getExpectedMetadata()));
    }

    @Test
    public void testUpdateMetadataWithRemovedBinaries() {
        setupBinariesSearchResults();

        UpdateSet updateSet = new UpdateSet(Collections.singletonList(METADATA_WITH_REMOVED_BINARIES_XML_FILENAME), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(3, updateStatus.getAttemptedUpdatesAndDeletes());
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME1));
        assertTrue(updateStatus.getSuccessfulDeletes().contains(BINARY_FILENAME2));
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME3));
        verify(searchService).updateContent(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(Content.class), eq(getExpectedMetadataWithRemovedBinaries()));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME2));
        verify(searchService).updateContent(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME3), any(Content.class));
    }

    @Test
    public void testUpdateBinary() throws Exception {
        setupMetadataSearchResult();

        UpdateSet updateSet = new UpdateSet(Collections.singletonList(BINARY_FILENAME1), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME1));
        verify(searchService).updateContent(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(Content.class), eq(getExpectedMetadata()));
    }

    @Test
    public void testDeleteBinary() throws Exception {
        UpdateSet updateSet = new UpdateSet(Collections.emptyList(), Collections.singletonList(BINARY_FILENAME1));
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertTrue(updateStatus.getSuccessfulDeletes().contains(BINARY_FILENAME1));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1));
    }

    @Test
    public void testDeleteMetadata() throws Exception {
        setupBinariesSearchResults();

        UpdateSet updateSet = new UpdateSet(Collections.emptyList(), Collections.singletonList(METADATA_XML_FILENAME));
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(3, updateStatus.getAttemptedUpdatesAndDeletes());
        assertTrue(updateStatus.getSuccessfulDeletes().contains(BINARY_FILENAME1));
        assertTrue(updateStatus.getSuccessfulDeletes().contains(BINARY_FILENAME2));
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME3));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME2));
        verify(searchService).updateContent(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME3), any(Content.class));
    }

    protected SearchService getSearchService() throws Exception {
        SearchService searchService = mock(SearchService.class);
        when(searchService.createRequest()).thenReturn(mock(SearchRequest.class));

        return searchService;
    }

    protected void setupBinariesSearchResults() {
        Map<String, Object> binary1Doc = Collections.singletonMap("localId", BINARY_FILENAME1);
        Map<String, Object> binary2Doc = Collections.singletonMap("localId", BINARY_FILENAME2);
        Map<String, Object> binary3Doc = Collections.singletonMap("localId", BINARY_FILENAME3);

        List<Map<String, Object>> documents = Arrays.asList(binary1Doc, binary2Doc, binary3Doc);
        SearchResponse response = new SearchResponse();
        response.setItems(documents);

        when(searchService.search(any(SearchRequest.class))).thenReturn(response);
    }

    protected void setupMetadataSearchResult() {
        Map<String, Object> doc = getExpectedMetadata().toSingleValueMap();
        List<Map<String, Object>> documents = Collections.singletonList(doc);
        SearchResponse response = new SearchResponse();
        response.setItems(documents);

        when(searchService.search(any(SearchRequest.class))).thenReturn(response);
    }

    protected BinaryFileWithMetadataBatchIndexer getBatchIndexer() throws Exception {
        BinaryFileWithMetadataBatchIndexer batchIndexer = new BinaryFileWithMetadataBatchIndexer();
        batchIndexer.setMetadataPathPatterns(Collections.singletonList(".*metadata.*\\.xml$"));
        batchIndexer.setBinaryPathPatterns(Arrays.asList(".*\\.pdf$", ".*\\.txt$"));
        batchIndexer.setChildBinaryPathPatterns(Collections.singletonList(".*\\.pdf$"));
        batchIndexer.setIncludePropertyPatterns(Collections.singletonList("copyright.*"));
        batchIndexer.setExcludePropertyPatterns(Collections.singletonList("copyright\\.ignore"));
        batchIndexer.setReferenceXPaths(Collections.singletonList("//file"));

        return batchIndexer;
    }

    protected MultiValueMap<String, Object> getExpectedMetadata() {
        MultiValueMap<String, Object> metadata = new LinkedMultiValueMap<>();
        metadata.set("copyright.company", "Crafter Software");
        metadata.set("copyright.text", "All rights reserved");
        metadata.set("copyright.year", "2017");
        metadata.set("metadataPath", METADATA_XML_FILENAME);

        return metadata;
    }

    protected MultiValueMap<String, Object> getExpectedMetadataWithRemovedBinaries() {
        MultiValueMap<String, Object> metadata = getExpectedMetadata();
        metadata.set("metadataPath", METADATA_WITH_REMOVED_BINARIES_XML_FILENAME);

        return metadata;
    }

}
