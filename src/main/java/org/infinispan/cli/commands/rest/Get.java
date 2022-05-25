package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CacheCompleter;
import org.infinispan.cli.logging.Messages;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 13.0
 **/
@CommandLine.Command(name = "get", description = "Gets the value of a key from a cache")
public class Get extends RestCallable {
   @CommandLine.Parameters(index = "0", description = "The cache name", completionCandidates = CacheCompleter.class)
   String cache;

   @CommandLine.Parameters(index = "1", description = "The key")
   String key;

   @Override
   protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
      return client.cache(getCacheName(resource, cache).orElseThrow(Messages.MSG::missingCacheName)).get(key);
   }
}
