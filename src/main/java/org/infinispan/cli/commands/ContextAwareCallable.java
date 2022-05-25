package org.infinispan.cli.commands;

import java.util.concurrent.Callable;

import org.infinispan.cli.Context;
import org.infinispan.cli.resources.ContainerResource;

import picocli.CommandLine;

/**
 * @since 14.0
 **/
@CommandLine.Command
public abstract class ContextAwareCallable<T> implements Callable<Integer> {
   @CommandLine.ParentCommand
   private Object parent;

   @Override
   public final Integer call() {
      Context context = context();
      try {
         T ret = call(context);
         if (ret != null) {
            String s = ret.toString();
            if (!s.isEmpty()) {
               context.out().println(s);
            }
         }
         return 0;
      } catch (Throwable t) {
         context.printError(t);
         return 1;
      } finally {

      }
   }

   protected Context context() {
      if (parent instanceof CLI) {
         return ((CLI) parent).context();
      } else {
         return ((ContextAwareCallable)parent).context();
      }
   }

   protected String containerName() {
      return context().resource().findAncestor(ContainerResource.class).getName();
   }

   protected abstract T call(Context context) throws Exception;
}
