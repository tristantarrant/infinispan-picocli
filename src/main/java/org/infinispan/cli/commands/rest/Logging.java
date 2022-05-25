package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.LogAppenderCompleter;
import org.infinispan.cli.completers.LoggersCompleter;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 11.0
 **/

@CommandLine.Command(name = "logging", description = "Inspects/Manipulates the server logging configuration", subcommands = {Logging.Loggers.class, Logging.Appenders.class, Logging.Set.class, Logging.Remove.class})
public class Logging {

   @CommandLine.Command(name = "list-loggers", description = "Lists available loggers")
   public static class Loggers extends RestCallable {

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.server().logging().listLoggers();
      }
   }

   @CommandLine.Command(name = "list-appenders", description = "Lists available appenders")
   public static class Appenders extends RestCallable {

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.server().logging().listAppenders();
      }
   }

   @CommandLine.Command(name = "remove", description = "Removes a logger")
   public static class Remove extends RestCallable {

      @CommandLine.Parameters(arity = "1", completionCandidates = LoggersCompleter.class)
      String name;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.server().logging().removeLogger(name);
      }
   }

   @CommandLine.Command(name = "set", description = "Sets a logger")
   public static class Set extends RestCallable {
      enum Level {
         OFF,
         TRACE,
         DEBUG,
         INFO,
         WARN,
         ERROR,
         FATAL,
         ALL
      }


      @CommandLine.Parameters(completionCandidates = LoggersCompleter.class)
      String name;

      @CommandLine.Option(names = {"-l", "--level"}, description = "One of OFF, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, ALL")
      Level level;

      @CommandLine.Option(names = {"-a", "--appender"}, description = " One or more appender names", completionCandidates = LogAppenderCompleter.class, arity = "1..*")
      String[] appenders;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.server().logging().setLogger(name, level.name(), appenders);
      }
   }
}
