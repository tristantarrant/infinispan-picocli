package org.infinispan.cli.commands.rest;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.infinispan.cli.completers.BackupCompleter;
import org.infinispan.cli.completers.CacheCompleter;
import org.infinispan.cli.completers.CacheConfigurationCompleter;
import org.infinispan.cli.completers.CounterCompleter;
import org.infinispan.cli.completers.SchemaCompleter;
import org.infinispan.cli.completers.TaskCompleter;
import org.infinispan.cli.resources.ContainerResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.commons.util.Version;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import picocli.CommandLine;

/**
 * @author Ryan Emerson
 * @since 12.0
 **/

@CommandLine.Command(name = "backup", description = "Manages container backup creation and restoration",
      subcommands = {Backup.Create.class, Backup.Delete.class, Backup.Get.class, Backup.ListBackups.class, Backup.Restore.class})
public class Backup {

   public static final String CACHES = "caches";
   public static final String TEMPLATES = "templates";
   public static final String COUNTERS = "counters";
   public static final String PROTO_SCHEMAS = "proto-schemas";
   public static final String TASKS = "tasks";

   @CommandLine.Command(name = "delete", description = "Delete a backup on the server")
   public static class Delete extends RestCallable {

      @CommandLine.Parameters(description = "The name of the backup", completionCandidates = BackupCompleter.class, arity = "1")
      String name;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         String container = resource.findAncestor(ContainerResource.class).getName();
         context().out().printf("Deleting backup %s%n", name);
         return client.cacheManager(container).deleteBackup(this.name);
      }
   }

   @CommandLine.Command(name = "get", description = "Get a backup from the server")
   public static class Get extends RestCallable {
      public static final String NO_CONTENT = "no-content";

      @CommandLine.Parameters(description = "The name of the backup", completionCandidates = BackupCompleter.class, arity = "1")
      String name;

      @CommandLine.Option(description = "No content is downloaded, but the command only returns once the backup has finished", names = NO_CONTENT)
      boolean noContent;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         context().out().printf("Downloading backup %s%n", name);
         // Poll the backup's availability every 500 milliseconds with a maximum of 100 attempts
         return Flowable.timer(500, TimeUnit.MILLISECONDS, Schedulers.trampoline())
               .repeat(100)
               .flatMapSingle(Void -> Single.fromCompletionStage(client.cacheManager(containerName()).getBackup(name, noContent)))
               .takeUntil(rsp -> rsp.getStatus() != 202)
               .lastOrErrorStage();
      }

      @Override
      public ResponseMode getResponseMode() {
         return noContent ? ResponseMode.QUIET : ResponseMode.FILE;
      }
   }

   @CommandLine.Command(name = "ls", description = "List all backups on the server")
   public static class ListBackups extends RestCallable {
      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.cacheManager(containerName()).getBackupNames();
      }
   }

   @CommandLine.Command(name = "create", description = "Create a backup on the server")
   public static class Create extends AbstractResourceCommand {

      @CommandLine.Option(names = { "-d", "--dir"}, description = "The directory on the server to be used for creating and storing the backup")
      String dir;

      @CommandLine.Option(names = { "-n", "--name"}, description = "The name of the backup")
      String name;


      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         // If the backup name has not been specified generate one based upon the Infinispan version and timestamp
         String backupName = name != null ? name : String.format("%s-%tY%2$tm%2$td%2$tH%2$tM%2$tS", Version.getBrandName(), LocalDateTime.now());
         context().out().printf("Creating backup '%s'%n", backupName);
         return client.cacheManager(containerName()).createBackup(backupName, dir, createResourceMap());
      }
   }

   @CommandLine.Command(name = "restore", description = "Restore a backup")
   public static class Restore extends AbstractResourceCommand {

      @CommandLine.Parameters(description = "The path of the backup file ", arity = "1")
      Path path;

      @CommandLine.Option(names = { "-n", "--name"}, description = "Defines a name for the restore request.")
      String name;

      @CommandLine.Option(names = { "-u", "--upload"}, description = "Indicates that the path is a local file which must be uploaded to the server")
      boolean upload;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         Map<String, List<String>> resources = createResourceMap();
         // If the restore name has not been specified generate one based upon the Infinispan version and timestamp
         String restoreName = name != null ? name : String.format("%s-%tY%2$tm%2$td%2$tH%2$tM%2$tS", Version.getBrandName(), LocalDateTime.now());
         String container = containerName();
         if (upload) {
            context().out().printf("Uploading backup '%s' and restoring%n", path);
            File file = path.toFile();
            return client.cacheManager(container).restore(restoreName, file, resources).thenCompose(rsp -> pollRestore(restoreName, container, client, rsp));
         } else {
            context().out().printf("Restoring from backup '%s'%n", path);
            return client.cacheManager(container).restore(restoreName, path.toString(), resources).thenCompose(rsp -> pollRestore(restoreName, container, client, rsp));
         }
      }
   }

   private static CompletionStage<RestResponse> pollRestore(String restoreName, String container, RestClient c, RestResponse rsp) {
      if (rsp.getStatus() != 202) {
         return CompletableFuture.completedFuture(rsp);
      }
      // Poll the restore progress every 500 milliseconds with a maximum of 100 attempts
      return Flowable.timer(500, TimeUnit.MILLISECONDS, Schedulers.trampoline())
            .repeat(100)
            .flatMapSingle(Void -> Single.fromCompletionStage(c.cacheManager(container).getRestore(restoreName)))
            .takeUntil(r -> r.getStatus() != 202)
            .lastOrErrorStage();
   }

   private abstract static class AbstractResourceCommand extends RestCallable {
      @CommandLine.Option(description = "Comma separated list of caches to include, '*' indicates all available",
            completionCandidates = CacheCompleter.class, names = CACHES, arity = "0..*")
      String[] caches;

      @CommandLine.Option(description = "Comma separated list of cache templates to include, '*' indicates all available",
            completionCandidates = CacheConfigurationCompleter.class, names = TEMPLATES, arity = "0..*")
      String[] templates;

      @CommandLine.Option(description = "Comma separated list of counters to include, '*' indicates all available",
            completionCandidates = CounterCompleter.class, names = COUNTERS)
      String[] counters;

      @CommandLine.Option(description = "Comma separated list of proto schemas to include, '*' indicates all available",
            completionCandidates = SchemaCompleter.class, names = PROTO_SCHEMAS)
      String[] protoSchemas;

      @CommandLine.Option(description = "Comma separated list of tasks to include, '*' indicates all available",
            completionCandidates = TaskCompleter.class, names = TASKS)
      List<String> tasks;

      public Map<String, List<String>> createResourceMap() {
         Map<String, List<String>> resourceMap = new HashMap<>();
         if (caches != null) {
            resourceMap.put(CACHES, Arrays.asList(caches));
         }
         if (templates != null) {
            resourceMap.put(TEMPLATES, Arrays.asList(templates));
         }
         if (counters != null) {
            resourceMap.put(COUNTERS, Arrays.asList(counters));
         }
         if (protoSchemas != null) {
            resourceMap.put(PROTO_SCHEMAS, Arrays.asList(protoSchemas));
         }
         if (tasks != null) {
            resourceMap.put(TASKS, tasks);
         }
         return resourceMap;
      }
   }
}
