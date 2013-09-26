/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.metrics.jersey;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.MetricsRegistry;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Filter that increments per-status-code counters.
 */
@ThreadSafe
final class HttpStatusCodeMetricResourceFilter implements ResourceFilter, ContainerResponseFilter {

    private final ConcurrentMap<Integer, Counter> counters = new ConcurrentHashMap<Integer, Counter>();

    private final Class<?> resourceClass;

    private final String metricBaseName;

    private final MetricsRegistry metricsRegistry;

    HttpStatusCodeMetricResourceFilter(MetricsRegistry metricsRegistry, String metricBaseName, Class<?> resourceClass) {
        this.metricsRegistry = metricsRegistry;
        this.metricBaseName = metricBaseName;
        this.resourceClass = resourceClass;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        // don't filter requests
        return null;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return this;
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        Integer status = response.getStatus();

        Counter counter = counters.get(status);
        if (counter == null) {
            // despite the method name, this actually will return a previously created metric with the same name
            Counter newCounter = metricsRegistry.newCounter(resourceClass, metricBaseName + " " + status + " counter");
            Counter otherCounter = counters.putIfAbsent(status, newCounter);
            if (otherCounter != null) {
                // we lost the race to set that counter, but shouldn't create a duplicate since Metrics.newCounter will do the right thing
                counter = otherCounter;
            } else {
                counter = newCounter;
            }
        }

        counter.inc();

        return response;
    }
}
