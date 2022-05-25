package org.infinispan.cli.commands.rest;

import java.nio.file.Path;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CacheCompleter;
import org.infinispan.cli.completers.CacheConfigurationAttributeCompleter;
import org.infinispan.cli.logging.Messages;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestEntity;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 13.0
 **/
@CommandLine.Command(name = "alter", description = "Alters a configuration", subcommands = {Alter.Cache.class})
public class Alter {

   @CommandLine.Command(name = "cache", description = "Alters a cache configuration")
   public static class Cache extends RestCallable {
      
      static class Exclusive {
         @CommandLine.Option(names = {"--attribute"}, description = "The configuration attribute", completionCandidates = CacheConfigurationAttributeCompleter.class, required = true)
         String attribute;

         @CommandLine.Option(names = {"-f", "--file"}, required = true)
         Path file;
      }

      @CommandLine.ArgGroup(multiplicity = "1")
      Exclusive exclusive;

      @CommandLine.Parameters(completionCandidates = CacheCompleter.class, description = "The cache name")
      String cache;

      @CommandLine.Option(names = {"--value"}, description = "The value for the configuration attribute")
      String value;



      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         String n = getCacheName(resource, cache).orElseThrow(Messages.MSG::missingCacheName);
         if (exclusive.file != null) {
            return client.cache(n).updateWithConfiguration(RestEntity.create(exclusive.file.toFile()));
         } else {
            return client.cache(n).updateConfigurationAttribute(exclusive.attribute, value);
         }
      }
   }
}
