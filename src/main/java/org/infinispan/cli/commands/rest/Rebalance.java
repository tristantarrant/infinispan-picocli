package org.infinispan.cli.commands.rest;

import static org.infinispan.cli.logging.Messages.MSG;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CdContextCompleter;
import org.infinispan.cli.resources.CacheResource;
import org.infinispan.cli.resources.ContainerResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Ryan Emerson
 * @since 13.0
 */
@CommandLine.Command(name = "rebalance", description = "Manage rebalance behaviour", subcommands = {Rebalance.Enable.class, Rebalance.Disable.class})
public class Rebalance {

   @CommandLine.Command(name = "enable", description = "Enable rebalancing")
   public static class Enable extends RestCallable {
      @CommandLine.Parameters(description = "The path of the resource", completionCandidates = CdContextCompleter.class)
      String name;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         try {
            Resource r = resource.getResource(name);
            if (r instanceof CacheResource) {
               return client.cache(r.getName()).enableRebalancing();
            } else if (r instanceof ContainerResource) {
               return client.cacheManager(r.getName()).enableRebalancing();
            } else {
               String name = r.getName();
               throw MSG.invalidResource(name.isEmpty() ? "/" : name);
            }
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }
   }

   @CommandLine.Command(name = "disable", description = "Disable rebalancing")
   public static class Disable extends RestCallable {
      @CommandLine.Parameters(description = "The path of the resource", completionCandidates = CdContextCompleter.class)
      String name;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         try {
            Resource r = resource.getResource(name);
            if (r instanceof CacheResource) {
               return client.cache(r.getName()).disableRebalancing();
            } else if (r instanceof ContainerResource) {
               return client.cacheManager(r.getName()).disableRebalancing();
            } else {
               String name = r.getName();
               throw MSG.invalidResource(name.isEmpty() ? "/" : name);
            }
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }
   }
}
