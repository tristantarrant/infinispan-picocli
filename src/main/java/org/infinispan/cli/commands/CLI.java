package org.infinispan.cli.commands;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.fusesource.jansi.AnsiConsole;
import org.infinispan.cli.Context;
import org.infinispan.cli.commands.kubernetes.Kube;
import org.infinispan.cli.commands.local.Benchmark;
import org.infinispan.cli.commands.rest.Add;
import org.infinispan.cli.commands.rest.Alter;
import org.infinispan.cli.commands.rest.Availability;
import org.infinispan.cli.commands.rest.Backup;
import org.infinispan.cli.commands.rest.Caches;
import org.infinispan.cli.commands.rest.Cas;
import org.infinispan.cli.commands.rest.ClearCache;
import org.infinispan.cli.commands.rest.Connector;
import org.infinispan.cli.commands.rest.Create;
import org.infinispan.cli.commands.rest.DataSource;
import org.infinispan.cli.commands.rest.Drop;
import org.infinispan.cli.commands.rest.Get;
import org.infinispan.cli.commands.rest.Logging;
import org.infinispan.cli.commands.rest.Migrate;
import org.infinispan.cli.commands.rest.Put;
import org.infinispan.cli.commands.rest.Query;
import org.infinispan.cli.commands.rest.Rebalance;
import org.infinispan.cli.commands.rest.Remove;
import org.infinispan.cli.commands.rest.Reset;
import org.infinispan.cli.commands.rest.Roles;
import org.infinispan.cli.commands.rest.Schema;
import org.infinispan.cli.commands.rest.Server;
import org.infinispan.cli.commands.rest.Shutdown;
import org.infinispan.cli.commands.rest.Site;
import org.infinispan.cli.commands.rest.Stats;
import org.infinispan.cli.commands.rest.Task;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.configuration.RestClientConfigurationBuilder;
import org.infinispan.commons.jdkspecific.ProcessInfo;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.shell.jline3.PicocliCommands;

@Command(
      name = "cli",
      mixinStandardHelpOptions = true,
      subcommands = {
            Add.class,
            Alter.class,
            Availability.class,
            Backup.class,
            Benchmark.class,
            Caches.class,
            Cas.class,
            ClearCache.class,
            Connector.class,
            Create.class,
            DataSource.class,
            Drop.class,
            Get.class,
            Logging.class,
            Migrate.class,
            Put.class,
            Query.class,
            Rebalance.class,
            Remove.class,
            Reset.class,
            Roles.class,
            Schema.class,
            Server.class,
            Shutdown.class,
            Site.class,
            Stats.class,
            Task.class
      }
)
public class CLI implements Callable<Integer> {
   private Context.Mode mode = Context.Mode.BATCH;

   @Option(names = {"-c", "--connection"}, description = "A Hot Rod connection URL", defaultValue = "http://localhost:11222")
   private String connection = "http://localhost:11222";

   @Option(names = {"-u", "--username"}, description = "User name")
   String username;

   @Option(names = {"-p", "--password"}, description = "Password", interactive = true, arity = "0..1")
   char[] password;

   @Option(names = {"-v", "--verbose"}, description = "Enables verbose logging of command execution")
   boolean verbose;

   @Override
   public Integer call() {
      // Interactive mode
      mode = Context.Mode.INTERACTIVE;
      AnsiConsole.systemInstall();
      Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));

      PicocliCommands.PicocliCommandsFactory factory = new PicocliCommands.PicocliCommandsFactory();

      CommandLine cmd = new CommandLine(this, factory);
      PicocliCommands picocliCommands = new PicocliCommands(cmd);
      Parser parser = new DefaultParser();
      try (Terminal terminal = TerminalBuilder.builder().build()) {
         SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
         systemRegistry.setCommandRegistries(picocliCommands);
         systemRegistry.register("help", picocliCommands);

         LineReader reader = LineReaderBuilder.builder()
               .terminal(terminal)
               .completer(systemRegistry.completer())
               .parser(parser)
               .variable(LineReader.LIST_MAX, 50)
               .build();
         factory.setTerminal(terminal);

         String line;
         while (true) {
            try {
               systemRegistry.cleanUp();
               line = reader.readLine(context().prompt(), null, (MaskingCallback) null, null);
               systemRegistry.execute(line);
            } catch (UserInterruptException e) {
               // Ignore
            } catch (EndOfFileException e) {
               return 0;
            } catch (Exception e) {
               systemRegistry.trace(e);
            }
         }
      } catch (Throwable t) {
         return 1;
      } finally {
         AnsiConsole.systemUninstall();
      }
   }

   public Context context() {
      return Context.get(() -> {
         RestClientConfigurationBuilder builder = new RestClientConfigurationBuilder();
         builder.uri(connection);
         if (username != null) {
            builder.security().authentication().username(username);
         }
         if (password != null) {
            builder.security().authentication().password(password);
         }
         return RestClient.forConfiguration(builder.build());
      });
   }

   private static boolean isKubernetesMode() {
      return ProcessInfo.getInstance().getName().contains("kubectl") || Boolean.getBoolean("infinispan.cli.kubernetes");
   }

   public static void main(String... args) {
      int exitCode = new CommandLine(isKubernetesMode() ? new Kube() : new CLI()).execute(args);
      System.exit(exitCode);
   }
}