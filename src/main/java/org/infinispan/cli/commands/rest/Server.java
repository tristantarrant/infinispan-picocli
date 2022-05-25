package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 11.0
 **/

@CommandLine.Command(name = "server", description = "Obtains information about the server", subcommands = {Connector.class, DataSource.class, Server.Report.class})
public class Server {
   @CommandLine.Command(name = "report", description = "Obtains an aggregate report from the server")
   public static class Report extends RestCallable {


      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.server().report();
      }

      @Override
      public ResponseMode getResponseMode() {
         return ResponseMode.FILE;
      }
   }
}
