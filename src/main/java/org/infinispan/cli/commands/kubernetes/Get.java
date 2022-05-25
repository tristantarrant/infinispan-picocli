package org.infinispan.cli.commands.kubernetes;

import static org.infinispan.cli.commands.kubernetes.Kube.INFINISPAN_CLUSTER_CRD;

import java.io.PrintWriter;
import java.util.List;

import org.infinispan.cli.Context;
import org.infinispan.cli.commands.ContextAwareCallable;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.0
 **/
@CommandLine.Command(
      name = "get",
      description = "Displays resources.",
      subcommands = {
            Get.Clusters.class,
      })
public class Get {

   @CommandLine.Command(name = "clusters", description = "Get clusters")
   public static class Clusters extends ContextAwareCallable<Integer> {

      @CommandLine.Option(names = {"-n", "--namespace"}, description = "Specifies the namespace where the cluster is running. Uses the default namespace if you do not specify one.")
      String namespace;

      @CommandLine.Option(names = {"-A", "--all-namespaces"}, description = "Displays the requested object(s) across all namespaces.")
      boolean allNamespaces;

      @CommandLine.Option(names = {"-s", "--secrets"}, description = "Displays all secrets that the cluster uses.")
      protected boolean secrets;

      @Override
      protected Integer call(Context context) throws Exception {
         KubernetesClient client = context.kubernetesClient();
         GenericKubernetesResourceList resource = allNamespaces ?
               client.genericKubernetesResources(INFINISPAN_CLUSTER_CRD).inAnyNamespace().list() :
               client.genericKubernetesResources(INFINISPAN_CLUSTER_CRD).inNamespace(Kube.getNamespaceOrDefault(client, namespace)).list();
         List<GenericKubernetesResource> items = resource.getItems();
         PrintWriter out = context.out();
         out.printf("%-32s %-16s %-9s %-16s%n", "NAME", "NAMESPACE", "STATUS", "SECRETS");
         items.forEach(item -> {
            String n = item.getMetadata().getName();
            String ns = item.getMetadata().getNamespace();
            List<Pod> pods = client.pods().inNamespace(ns).withLabel("infinispan_cr", n).list().getItems();
            long running = pods.stream().map(p -> p.getStatus()).filter(s -> "Running".equalsIgnoreCase(s.getPhase())).count();

            out.printf("%-32s %-16s %-9s", n, ns, running + "/" + pods.size());
            if (secrets) {
               String secretName = Kube.getProperty(item, "spec", "security", "endpointSecretName");
               Secret secret = Kube.getSecret(client, ns, secretName);
               Kube.decodeOpaqueSecrets(secret).entrySet().forEach(c -> out.printf("%n%-60s%-16s %-16s", "", c.getKey(), c.getValue()));
               out.println();
            } else {
               out.printf(" %-16s%n", "******");
            }
         });

         return 0;
      }
   }
}
