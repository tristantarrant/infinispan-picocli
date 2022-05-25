package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CacheCompleter;
import org.infinispan.cli.resources.CacheResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/

@CommandLine.Command(name = "remove", description = "Removes an entry from the cache", aliases = "rm")
public class Remove extends RestCallable {

   @CommandLine.Parameters(arity = "1")
   String key;

   @CommandLine.Option(names = {"-c", "--cache"}, completionCandidates = CacheCompleter.class)
   String cache;

   @Override
   protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
      return client.cache(cache != null ? cache : CacheResource.cacheName(resource)).remove(key);
   }

}
