package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CacheCompleter;
import org.infinispan.cli.resources.CacheResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

@CommandLine.Command(name = "clearcache", description = "Clears the cache")
public class ClearCache extends RestCallable {

   @CommandLine.Parameters(description = "The name of the cache", completionCandidates = CacheCompleter.class)
   String name;

   @Override
   protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
      return client.cache(name != null ? name : CacheResource.cacheName(resource)).clear();
   }
}