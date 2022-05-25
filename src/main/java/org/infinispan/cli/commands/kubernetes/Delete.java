package org.infinispan.cli.commands.kubernetes;

import static org.infinispan.cli.commands.kubernetes.Kube.DEFAULT_CLUSTER_NAME;
import static org.infinispan.cli.commands.kubernetes.Kube.INFINISPAN_CLUSTER_CRD;

import org.infinispan.cli.Context;
import org.infinispan.cli.commands.ContextAwareCallable;

import io.fabric8.kubernetes.client.KubernetesClient;
import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.0
 **/
@CommandLine.Command(
      name = "delete",
      description = "Deletes resources.",
      subcommands = {
            Delete.Cluster.class,
      })
public class Delete {
   @CommandLine.Command(name = "cluster", description = "Deletes a cluster")
   public static class Cluster extends ContextAwareCallable {

      @CommandLine.Option(names = {"-n", "--namespace"}, description = "Specifies the namespace of the cluster to delete. Uses the default namespace if you do not specify one.")
      String namespace;

      @CommandLine.Parameters(description = "Specifies the name of the cluster to delete. Defaults to '" + DEFAULT_CLUSTER_NAME + "'", defaultValue = DEFAULT_CLUSTER_NAME)
      String name;

      @Override
      protected Object call(Context context) throws Exception {
         KubernetesClient client = context.kubernetesClient();
         client.genericKubernetesResources(INFINISPAN_CLUSTER_CRD).inNamespace(Kube.getNamespaceOrDefault(client, namespace)).withName(name).delete();
         return 0;
      }
   }
}
