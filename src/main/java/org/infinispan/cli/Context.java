package org.infinispan.cli;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;

import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.commons.util.Util;

import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * @since 14.0
 **/
public class Context implements AutoCloseable {
   public enum Mode {
      BATCH,
      INTERACTIVE,
      KUBERNETES
   }

   // The singleton
   private static Context context;

   private final RestClient client;
   private Resource resource;
   private final Path workingDir;
   private final PrintWriter out;
   private final PrintWriter err;
   private final Mode mode;

   private Context(RestClient client) {
      this.client = client;
      this.workingDir = Paths.get(System.getProperty("user.dir", ""));
      this.resource = Resource.getRootResource(client);
      this.out = new PrintWriter(System.out);
      this.err = new PrintWriter(System.err);
   }

   @Override
   public void close() {
      Util.close(client);
   }

   public RestClient client() {
      return client;
   }

   public KubernetesClient kubernetesClient() {
      return null;
   }

   public boolean isConnected() {
      return client != null;
   }

   public Resource resource() {
      return resource;
   }

   public static Context get(Supplier<RestClient> clientSupplier) {
      if (context == null) {
         context = new Context(clientSupplier.get());
      }
      return context;
   }

   public static Optional<Context> get() {
      return Optional.ofNullable(context);
   }

   public Path workingDir() {
      return workingDir;
   }

   public PrintWriter out() {
      return out;
   }

   public PrintWriter err() {
      return err;
   }

   public String prompt() {
      StringBuilder prompt = new StringBuilder();
      if (client != null) {
         prompt.append("[").append(resource.getName()).append("]> ");
      } else {
         prompt.append("[").append("disconnected").append("]> ");
      }
      return prompt.toString();
   }

   public void printError(Throwable t) {
      String s = Util.getRootCause(t).getLocalizedMessage();
      err.println(s);
   }
}
