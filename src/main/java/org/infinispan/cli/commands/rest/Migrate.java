package org.infinispan.cli.commands.rest;

import java.nio.file.Path;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CacheCompleter;
import org.infinispan.cli.logging.Messages;
import org.infinispan.cli.resources.CacheResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestEntity;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 11.0
 **/

@CommandLine.Command(name = "migrate", description = "Migration operations", subcommands = {Migrate.Cluster.class})
public class Migrate {

   @CommandLine.Command(name = "cluster", description = "Performs data migration between clusters", subcommands = {Cluster.ClusterConnect.class, Cluster.ClusterDisconnect.class, Cluster.ClusterSourceConnection.class, Cluster.ClusterSynchronize.class})
   public static class Cluster {

      @CommandLine.Command(name = "connect", description = "Connects to a source cluster")
      public static class ClusterConnect extends RestCallable {

         @CommandLine.Option(completionCandidates = CacheCompleter.class, names = {"-c", "--cache"}, description = "The name of the cache.")
         String cache;

         @CommandLine.Option(names = {"-f", "--file"}, description = "JSON containing a 'remote-store' element with the configuration")
         Path file;

         @Override
         protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
            if (file == null) {
               throw Messages.MSG.illegalCommandArguments();
            }
            RestEntity restEntity = RestEntity.create(file.toFile());
            return client.cache(cache != null ? cache : CacheResource.cacheName(resource)).connectSource(restEntity);
         }
      }

      @CommandLine.Command(name = "disconnect", description = "Disconnects from a source cluster")
      public static class ClusterDisconnect extends RestCallable {

         @CommandLine.Option(completionCandidates = CacheCompleter.class, names = {"-c", "--cache"}, description = "The name of the cache.")
         String cache;


         @Override
         protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
            return client.cache(cache != null ? cache : CacheResource.cacheName(resource)).disconnectSource();
         }
      }


      @CommandLine.Command(name = "source-connection", description = "Obtains the remote store configuration if a cache is connected to another cluster")
      public static class ClusterSourceConnection extends RestCallable {

         @CommandLine.Option(completionCandidates = CacheCompleter.class, names = {"-c", "--cache"}, description = " The name of the cache.")
         String cache;


         @Override
         protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
            return client.cache(cache != null ? cache : CacheResource.cacheName(resource)).sourceConnection();
         }
      }

      @CommandLine.Command(name = "synchronize", description = "Synchronizes data from a source to a target cluster")
      public static class ClusterSynchronize extends RestCallable {

         @CommandLine.Option(completionCandidates = CacheCompleter.class, names = {"-c", "--cahe"})
         String cache;

         @CommandLine.Option(names = {"-b", "--read-batch"}, description = "The amount of entries to process in a batch")
         Integer readBatch;

         @CommandLine.Option(names = {"-t", "--threads"}, description = " The number of threads to use.Defaults to the number of cores on the server")
         Integer threads;

         @Override
         protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
            return client.cache(cache != null ? cache : CacheResource.cacheName(resource)).synchronizeData(readBatch, threads);
         }
      }
   }
}
