package org.infinispan.cli.commands.rest;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.ServerCompleter;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
@CommandLine.Command(name = "shutdown", description = "Shuts down individual servers or the entire cluster", subcommands = {Shutdown.Server.class, Shutdown.Cluster.class, Shutdown.Container.class})
public class Shutdown {

   @CommandLine.Command(name = "server", description = "Shuts down one or more individual servers")
   public static class Server extends RestCallable {

      @CommandLine.Parameters(description = "One or more servers to shutdown", completionCandidates = ServerCompleter.class, arity = "0..*")
      List<String> servers;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return servers == null || servers.isEmpty() ? client.server().stop() : client.cluster().stop(servers);
      }
   }

   @CommandLine.Command(name = "cluster", description = "Shuts down the entire cluster")
   public static class Cluster extends RestCallable {
      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.cluster().stop();
      }
   }

   @CommandLine.Command(name = "container", description = "Shuts down the container without terminating the server processes")
   public static class Container extends RestCallable {
      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.container().shutdown();
      }
   }
}
