package org.infinispan.cli.commands.rest;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestCacheClient;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestEntity;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.dataconversion.internal.Json;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 13.0
 **/
@CommandLine.Command(name = "create", description = "Creates caches/counters", subcommands = {Create.Cache.class, Create.Counter.class})
public class Create {

   @CommandLine.Command(name = "cache", description = "Create a cache")
   public static class Cache extends RestCallable {
      @CommandLine.Parameters(index = "0", description = "The cache name")
      String cache;

      static class Exclusive {
         @CommandLine.Option(names = {"-t", "--template"}, description = "The name of a template", required = true)
         String template;

         @CommandLine.Option(names = {"-f", "--file"}, description = "A file containing the cache configuration", required = true)
         File file;
      }

      @CommandLine.ArgGroup(multiplicity = "1")
      Exclusive exclusive;

      @CommandLine.Option(names = {"-v", "--volatile"})
      boolean volatileCache;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         RestCacheClient cache = client.cache(this.cache);
         CacheContainerAdmin.AdminFlag flags[] = volatileCache ? new CacheContainerAdmin.AdminFlag[]{CacheContainerAdmin.AdminFlag.VOLATILE} : new CacheContainerAdmin.AdminFlag[]{};
         if (exclusive.template != null) {
            return cache.createWithTemplate(exclusive.template, flags);
         } else {
            return cache.createWithConfiguration(RestEntity.create(exclusive.file), flags);
         }
      }
   }

   @CommandLine.Command(name = "counter", description = "Create a counter")
   public static class Counter extends RestCallable {
      public enum CounterType {
         WEAK, STRONG
      }

      public enum CounterStorage {
         VOLATILE, PERSISTENT
      }

      @CommandLine.Parameters(description = "The name of the counter")
      String name;

      @CommandLine.Option(names = {"-t", "--type"}, defaultValue = "STRONG", description = "Type of counter [weak|strong]")
      CounterType type;

      @CommandLine.Option(names = {"-i", "--initial-value"}, defaultValue = "0", description = "Initial value for the counter (defaults to 0)")
      Long initialValue;

      @CommandLine.Option(names = {"-s", "--storage"}, defaultValue = "VOLATILE", description = "persistent state PERSISTENT | VOLATILE (default)")
      CounterStorage storage;

      @CommandLine.Option(names = {"-u", "--upper-bound"}, description = "The upper bound of the counter")
      Long upperBound;

      @CommandLine.Option(names = {"-l", "--lower-bound"}, description = "The lower bound of the counter")
      Long lowerBound;

      @CommandLine.Option(names = {"-c", "--concurrency-level"}, defaultValue = "16", description = "Concurrency level for weak counters, defaults to 16")
      Integer concurrencyLevel;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         Json counterBody = Json.object()
               .set("initial-value", initialValue)
               .set("storage", storage.name().toLowerCase(Locale.ROOT))
               .set("name", name);
         if (type == CounterType.WEAK) {
            counterBody.set("concurrency-level", concurrencyLevel);
         }
         if (upperBound != null) {
            counterBody.set("upper-bound", upperBound);
         }
         if (lowerBound != null) {
            counterBody.set("lower-bound", lowerBound);
         }
         Json counter = Json.object().set(type.name().toLowerCase(Locale.ROOT) + "-counter", counterBody);
         return client.counter(name).create(RestEntity.create(MediaType.APPLICATION_JSON, counter.toString()));
      }
   }
}
