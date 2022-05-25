package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CacheCompleter;
import org.infinispan.cli.completers.CounterCompleter;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/

@CommandLine.Command(name = "drop", description = "Drops a cache or a counter", subcommands = {Drop.Cache.class, Drop.Counter.class})
public class Drop {

   @CommandLine.Command(name = "cache", description = "Drop a cache")
   public static class Cache extends RestCallable {

      @CommandLine.Parameters(arity = "1", completionCandidates = CacheCompleter.class, description = "The cache name")
      String name;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.cache(name).delete();
      }
   }

   @CommandLine.Command(name = "counter", description = "Drop a counter")
   public static class Counter extends RestCallable {

      @CommandLine.Parameters(arity = "1", completionCandidates = CounterCompleter.class, description = "The counter name")
      String name;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.counter(name).delete();
      }
   }
}
