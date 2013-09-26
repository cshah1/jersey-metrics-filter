/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.metrics.jersey;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.api.model.AbstractSubResourceLocator;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import com.yammer.metrics.core.MetricsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Singleton
public final class HttpStatusCodeMetricResourceFilterFactory implements ResourceFilterFactory {

    private static final Logger logger = LoggerFactory.getLogger(HttpStatusCodeMetricResourceFilterFactory.class);

    private final ResourceMetricNamer namer;
    private final MetricsRegistry metricsRegistry;

    @Inject
    HttpStatusCodeMetricResourceFilterFactory(ResourceMetricNamer namer, MetricsRegistry metricsRegistry) {
        this.namer = namer;
        this.metricsRegistry = metricsRegistry;
    }

    @Override
    public List<ResourceFilter> create(AbstractMethod am) {

        // documented to only be AbstractSubResourceLocator, AbstractResourceMethod, or AbstractSubResourceMethod
        if (am instanceof AbstractSubResourceLocator) {
            // not actually invoked per request, nothing to do
            logger.debug("Ignoring AbstractSubResourceLocator " + am);
            return null;
        } else if (am instanceof AbstractResourceMethod) {
            String metricBaseName = namer.getMetricBaseName((AbstractResourceMethod) am);
            Class<?> resourceClass = am.getResource().getResourceClass();

            return Lists
                .<ResourceFilter>newArrayList(new HttpStatusCodeMetricResourceFilter(metricsRegistry, metricBaseName, resourceClass));
        } else {
            logger.warn("Got an unexpected instance of " + am.getClass().getName() + ": " + am);
            return null;
        }
    }
}
