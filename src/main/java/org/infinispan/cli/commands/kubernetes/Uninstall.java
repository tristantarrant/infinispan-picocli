package org.infinispan.cli.commands.kubernetes;

import java.util.Map;
import java.util.concurrent.Callable;

import org.infinispan.cli.commands.CLI;
import org.infinispan.cli.logging.Messages;
import org.infinispan.commons.util.Version;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.0
 **/
@CommandLine.Command(name = "uninstall", description = "Removes the Operator.")
public class Uninstall implements Callable<Integer> {
   @CommandLine.ParentCommand
   CLI cli;

   @CommandLine.Option(names = {"-n", "--namespace"}, description = "Specifies the namespace where the Operator is installed, if you did not install it globally.")
   String namespace;

   @Override
   public Integer call() throws Exception {
      KubernetesClient client = cli.getKubernetesCLient();
      if (namespace == null) {
         namespace = Kube.defaultOperatorNamespace(client);
      } else {
         // We need to remove the operator group
         client.genericKubernetesResources(Kube.OPERATOR_OPERATORGROUP_CRD).inNamespace(namespace).withName(Version.getProperty("infinispan.olm.name")).delete();
      }
      // Obtain the CSV for the subscription
      Resource<GenericKubernetesResource> subscription = client.genericKubernetesResources(Kube.OPERATOR_SUBSCRIPTION_CRD).inNamespace(namespace).withName(Version.getProperty("infinispan.olm.name"));
      GenericKubernetesResource sub = subscription.get();
      if (sub == null) {
         throw Messages.MSG.noOperatorSubscription(namespace);
      }
      Map<String, Object> status = (Map<String, Object>) sub.getAdditionalProperties().get("status");
      String csv = (String) status.get("installedCSV");
      boolean deleted = subscription.delete();
      if (deleted) {
         // Now delete the CSV
         deleted = client.genericKubernetesResources(Kube.OPERATOR_CLUSTERSERVICEVERSION_CRD).inNamespace(namespace).withName(csv).delete();
         return deleted ? 0 : 1;
      } else {
         return 1;
      }
   }
}
