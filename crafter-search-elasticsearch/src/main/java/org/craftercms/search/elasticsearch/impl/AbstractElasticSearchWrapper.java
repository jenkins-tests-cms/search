/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.search.elasticsearch.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpHost;
import org.craftercms.search.elasticsearch.ElasticSearchWrapper;
import org.craftercms.search.elasticsearch.exception.ElasticSearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base implementation of {@link ElasticSearchWrapper}
 * @author joseross
 */
public abstract class AbstractElasticSearchWrapper implements ElasticSearchWrapper, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(AbstractElasticSearchWrapper.class);

    /**
     * The ElasticSearch client
     */
    protected RestHighLevelClient client;

    /**
     * The server urls for ElasticSearch
     */
    protected String[] serverUrls;

    /**
     * The filter queries to apply to all searches
     */
    protected String[] filterQueries;

    @Required
    public void setServerUrls(final String[] serverUrls) {
        this.serverUrls = serverUrls;
    }

    public void setFilterQueries(final String[] filterQueries) {
        this.filterQueries = filterQueries;
    }

    @Override
    public void afterPropertiesSet() {
        client = new RestHighLevelClient(RestClient.builder(
            Stream.of(serverUrls).map(HttpHost::create).toArray(HttpHost[]::new)));
    }

    /**
     * Updates the value of the index for the given request
     * @param request the request to update
     */
    protected abstract void updateIndex(SearchRequest request);

    /**
     * Updates the filter queries for the given request
     * @param request the request to update
     */
    protected void updateFilters(SearchRequest request) {
        if(ArrayUtils.isEmpty(filterQueries)) {
            logger.debug("No additional filter queries configured");
            return;
        }

        BoolQueryBuilder boolQueryBuilder;
        if(request.source().query() instanceof BoolQueryBuilder) {
            boolQueryBuilder = (BoolQueryBuilder) request.source().query();
        } else {
            boolQueryBuilder = new BoolQueryBuilder().must(request.source().query());
        }

        for(String filterQuery : filterQueries) {
            logger.debug("Adding filter query: {}", filterQuery);
            boolQueryBuilder.filter(new QueryStringQueryBuilder(filterQuery));
        }

        request.source().query(boolQueryBuilder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse search(final SearchRequest request, final RequestOptions options) {
        logger.debug("Performing search for request: {}", request);
        updateIndex(request);
        updateFilters(request);
        try {
            return client.search(request, options);
        } catch (Exception e) {
            throw new ElasticSearchException(request.indices()[0], "Error executing search request", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse search(final Map<String, Object> request, final RequestOptions options) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(request);
            return search(json, options);
        } catch (IOException e) {
            throw new ElasticSearchException(null, "Error parsing request " + request, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse search(final String request, final RequestOptions options) {
        SearchModule module = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
        try {
            SearchSourceBuilder builder =
                SearchSourceBuilder.fromXContent(XContentFactory.xContent(XContentType.JSON)
                    .createParser(new NamedXContentRegistry(module.getNamedXContents()),
                        DeprecationHandler.THROW_UNSUPPORTED_OPERATION, request));
            return search(new SearchRequest().source(builder), options);
        } catch (IOException e) {
            throw new ElasticSearchException(null, "Error parsing request " + request, e);
        }
    }

}