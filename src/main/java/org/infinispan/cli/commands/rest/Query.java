package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CacheCompleter;
import org.infinispan.cli.resources.CacheResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

@CommandLine.Command(name = "query", description = "Queries a cache")
public class Query extends RestCallable {

   @CommandLine.Parameters(description = "The Ickle query")
   String query;

   @CommandLine.Option(names = {"-c", "--cache"}, description = "The name of the cache to query", completionCandidates = CacheCompleter.class)
   String cache;

   @CommandLine.Option(names = {"-m", "--max-results"}, defaultValue = "10")
   Integer maxResults;

   @CommandLine.Option(names = {"-o", "--offset"}, defaultValue = "0")
   Integer offset;

   @Override
   protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
      return client.cache(cache != null ? cache : CacheResource.cacheName(resource)).query(query, maxResults, offset);
   }
}