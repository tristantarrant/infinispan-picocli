package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CacheCompleter;
import org.infinispan.cli.resources.CacheResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestCacheClient;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * Manage a cache's Availability.
 *
 * @author Ryan Emerson
 * @since 13.0
 */

@CommandLine.Command(name = "availability", description = "Manage availability of clustered caches in network partitions.")
public class Availability extends RestCallable {
   enum AvailabilityMode {
      AVAILABLE,
      DEGRADED_MODE,
   }

   @CommandLine.Parameters(completionCandidates = CacheCompleter.class)
   String cache;

   @CommandLine.Option(names = { "-m", "--mode"})
   AvailabilityMode mode;


   @Override
   protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
      RestCacheClient cacheClient = client.cache(cache != null ? cache : CacheResource.cacheName(resource));
      return mode == null ?
            cacheClient.getAvailability() :
            cacheClient.setAvailability(mode.toString());
   }
}
