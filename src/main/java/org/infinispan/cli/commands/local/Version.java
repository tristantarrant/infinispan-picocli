package org.infinispan.cli.commands.local;

import org.infinispan.cli.Context;
import org.infinispan.cli.commands.ContextAwareCallable;
import org.infinispan.cli.impl.KubernetesContext;

import io.fabric8.kubernetes.client.KubernetesClient;
import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
@CommandLine.Command(name = "version", description = "Shows version information")
public class Version extends ContextAwareCallable<Integer> {
   protected Integer call(Context context) throws Exception {
      context.out().println(String.format("CLI: %s", org.infinispan.commons.util.Version.printVersion()));
      if (context.isConnected()) {
         context.out().println("Server: " + context.client().getServerVersion());
      }
      if (context instanceof KubernetesContext) {
         KubernetesClient client = context.kubernetesClient();
         context.out().printf("Kubernetes %s.%s\n", client.getKubernetesVersion().getMajor(), client.getKubernetesVersion().getMinor());
      }
      return 0;
   }
}
