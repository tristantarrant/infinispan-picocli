package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.DataSourceCompleter;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 13.0
 **/
@CommandLine.Command(name = DataSource.CMD, description = "Performs operations on data sources", subcommands = {DataSource.Ls.class, DataSource.Test.class})
public class DataSource {

   public static final String CMD = "datasource";

   @CommandLine.Command(name = "ls", description = "Lists data sources")
   public static class Ls extends RestCallable {

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.server().dataSourceNames();
      }
   }

   @CommandLine.Command(name = "test", description = "Tests a data source")
   public static class Test extends RestCallable {

      @CommandLine.Parameters(arity = "1", description = "The name of the data source", completionCandidates = DataSourceCompleter.class)
      String name;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.server().dataSourceTest(name);
      }
   }
}
