package org.infinispan.cli.commands.kubernetes;

import java.util.List;
import java.util.Optional;

import org.infinispan.cli.Context;
import org.infinispan.cli.commands.ContextAwareCallable;
import org.infinispan.cli.logging.Messages;
import org.infinispan.commons.util.Version;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.0
 **/
@CommandLine.Command(name = "install", description = "Installs the Operator.")
public class Install extends ContextAwareCallable {

   @CommandLine.Option(names = {"-n", "--namespace"}, description = "Specifies a namespace for the Operator. Defaults to installing the Operator in the default Operator namespace.")
   String namespace;

   @CommandLine.Option(names = {"-c", "--channel"}, description = "Selects the channel to install and subscribes to upgrades for that release stream. If not specified, the latest stable channel is installed.")
   String channel;

   @CommandLine.Option(defaultValue = "false", names = {"-m", "--manual"}, description = "Requires approval before applying upgrades from the Operator subscription. Defaults to automatic approval.")
   boolean manual;

   @CommandLine.Option(names = "--source", description = "Specifies the CatalogSource for the Operator subscription. Selects an environment-dependent CatalogSource by default.")
   String source;

   @CommandLine.Option(names = "--source-namespace", description = "Specifies the namespace of the subscription source. Selects an environment-dependent namespace by default.")
   String sourceNamespace;

   @CommandLine.Option(names = "--target-namespaces", description = "Specifies the namespaces that the Operator watches. You must set a target namespace if you install the Operator in a specific namespace.")
   List<String> targetNamespaces;

   @Override
   protected Object call(Context context) throws Exception {
      if (namespace != null && (targetNamespaces == null || targetNamespaces.isEmpty())) {
         throw Messages.MSG.noTargetNamespaces();
      }
      KubernetesClient client = context.kubernetesClient();
      if (source == null) {
         // Determine whether this is OpenShift or K8S+OLM
         List<GenericKubernetesResource> sources = client.genericKubernetesResources(Kube.OPERATOR_CATALOGSOURCE_CRD).inAnyNamespace().list().getItems();
         Optional<GenericKubernetesResource> catalog = sources.stream().filter(cs -> Version.getProperty("infinispan.olm.k8s.source").equals(cs.getMetadata().getName())).findFirst();
         if (!catalog.isPresent()) {
            catalog = sources.stream().filter(cs -> Version.getProperty("infinispan.olm.openshift.source").equals(cs.getMetadata().getName())).findFirst();
         }
         if (catalog.isPresent()) {
            GenericKubernetesResource catalogSource = catalog.get();
            source = catalogSource.getMetadata().getName();
            sourceNamespace = catalogSource.getMetadata().getNamespace();
         } else {
            throw Messages.MSG.noCatalog();
         }
      }
      String olmName = Version.getProperty("infinispan.olm.name");
      if (namespace == null) {
         namespace = Kube.defaultOperatorNamespace(client);
      } else {
         // Non-global, we need to create an operator group
         GenericKubernetesResource group = new GenericKubernetesResource();
         group.setKind(Kube.OPERATOR_OPERATORGROUP_CRD.getKind());
         ObjectMeta groupMetadata = new ObjectMeta();
         groupMetadata.setName(olmName);
         groupMetadata.setNamespace(namespace);
         group.setMetadata(groupMetadata);
         GenericKubernetesResource groupSpec = new GenericKubernetesResource();
         groupSpec.setAdditionalProperty("targetNamespaces", targetNamespaces);
         group.setAdditionalProperty("spec", groupSpec);
         client.genericKubernetesResources(Kube.OPERATOR_OPERATORGROUP_CRD).inNamespace(namespace).createOrReplace(group);
      }

      GenericKubernetesResource subscription = new GenericKubernetesResource();
      subscription.setKind(Kube.OPERATOR_SUBSCRIPTION_CRD.getKind());
      ObjectMeta subscriptionMetadata = new ObjectMeta();
      subscriptionMetadata.setName(olmName);
      subscriptionMetadata.setNamespace(namespace);
      subscription.setMetadata(subscriptionMetadata);
      GenericKubernetesResource subscriptionSpec = new GenericKubernetesResource();
      subscriptionSpec.setAdditionalProperty("name", olmName);
      subscriptionSpec.setAdditionalProperty("installPlanApproval", manual ? "Manual" : "Automatic");
      subscriptionSpec.setAdditionalProperty("source", source);
      subscriptionSpec.setAdditionalProperty("sourceNamespace", sourceNamespace);
      if (channel != null) {
         subscriptionSpec.setAdditionalProperty("channel", channel);
      }
      subscription.setAdditionalProperty("spec", subscriptionSpec);
      client.genericKubernetesResources(Kube.OPERATOR_SUBSCRIPTION_CRD).inNamespace(namespace).createOrReplace(subscription);
      return 0;
   }
}
