package org.infinispan.cli.commands.rest;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.TaskCompleter;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestEntity;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.commons.dataconversion.MediaType;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.1
 **/

@CommandLine.Command(name = "task", description = "Executes or manipulates server-side tasks", subcommands = {Task.Exec.class, Task.Upload.class})
public class Task {

   @CommandLine.Command(name = "exec", description = "Executes a server-side task")
   public static class Exec extends RestCallable {
      @CommandLine.Parameters(completionCandidates = TaskCompleter.class, arity = "1")
      String taskName;

      @CommandLine.Option(names = {"-P", "--parameters"}, description = "Task parameters")
      Map<String, String> parameters;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.tasks().exec(taskName, parameters == null ? Collections.emptyMap() : parameters);
      }
   }

   @CommandLine.Command(name = "upload", description = "Uploads a new script task to the server")
   public static class Upload extends RestCallable {
      @CommandLine.Parameters(description = "The task name", arity = "1")
      String taskName;

      @CommandLine.Option(names = {"-f", "--file"}, required = true)
      Path file;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.tasks().uploadScript(taskName, RestEntity.create(MediaType.TEXT_PLAIN, file.toFile()));
      }
   }
}
