package org.infinispan.cli.commands.kubernetes;

import static org.infinispan.cli.commands.kubernetes.Kube.DEFAULT_CLUSTER_NAME;
import static org.infinispan.cli.commands.kubernetes.Kube.INFINISPAN_CLUSTER_CRD;

import java.util.Map;

import org.infinispan.cli.Context;
import org.infinispan.cli.commands.ContextAwareCallable;
import org.infinispan.cli.logging.Messages;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.0
 **/

@CommandLine.Command(
      name = "create",
      description = "Creates resources.",
      subcommands = {
            Create.Cluster.class,
      })
public class Create {

   public enum Expose {
      LoadBalancer,
      NodePort,
      Route;
   }

   public enum Encryption {
      None,
      Secret,
      Service;
   }

   @CommandLine.Command(name = "cluster", description = "Creates a cluster")
   public static class Cluster extends ContextAwareCallable<Integer> {

      @CommandLine.Option(names = { "-n", "--namespace" }, description = "Specifies the namespace where the cluster is created. Uses the default namespace if you do not specify one.")
      String namespace;

      @CommandLine.Option(names = { "-r", "--replicas"}, description = "Specifies the number of replicas. Defaults to 1.", defaultValue = "1")
      int replicas;

      @CommandLine.Option(names = "expose-type", description = "Makes the service available on the network through a LoadBalancer, NodePort, or Route.")
      Expose exposeType;

      @CommandLine.Option(names = "expose-port", defaultValue = "0", description = "Sets the network port where the service is available. You must set a port if the expose type is LoadBalancer or NodePort.")
      int exposePort;

      @CommandLine.Option(names = "expose-host", description = "Optionally sets the hostname if the expose type is Route.")
      String exposeHost;

      @CommandLine.Option(names = "encryption-type", description = "The type of encryption: one of None, Secret, Service")
      Encryption encryptionType;

      @CommandLine.Option(names = "encryption-secret", description = "The name of the secret containing the TLS certificate")
      String encryptionSecret;

      @CommandLine.Parameters(description = "Defines the cluster name. Defaults to '" + DEFAULT_CLUSTER_NAME + "'", defaultValue = DEFAULT_CLUSTER_NAME)
      String name;

      @CommandLine.Option(names = {"-P", "--spec-properties"}, description = "Sets a spec property. Use the '.' as separator for child nodes")
      Map<String, String> specProperties;


      @Override
      protected Integer call(Context context) throws Exception {
         KubernetesClient client = context.kubernetesClient();
         namespace = Kube.getNamespaceOrDefault(client, namespace);
         GenericKubernetesResource infinispan = new GenericKubernetesResource();
         infinispan.setKind(INFINISPAN_CLUSTER_CRD.getKind());
         ObjectMeta metadata = new ObjectMeta();
         metadata.setName(name);
         metadata.setNamespace(namespace);
         infinispan.setMetadata(metadata);
         GenericKubernetesResource spec = new GenericKubernetesResource();
         infinispan.setAdditionalProperty("spec", spec);
         spec.setAdditionalProperty("replicas", replicas);
         if (exposeType != null) {
            GenericKubernetesResource expose = new GenericKubernetesResource();
            spec.setAdditionalProperty("expose", expose);
            expose.setAdditionalProperty("type", exposeType);
            switch (exposeType) {
               case LoadBalancer:
                  if (exposePort == 0) {
                     throw Messages.MSG.exposeTypeRequiresPort(exposeType);
                  } else {
                     expose.setAdditionalProperty("port", exposePort);
                  }
                  break;
               case NodePort:
                  if (exposePort == 0) {
                     throw Messages.MSG.exposeTypeRequiresPort(exposeType);
                  } else {
                     expose.setAdditionalProperty("nodePort", exposePort);
                  }
                  break;
            }
            if (exposeHost != null) {
               expose.setAdditionalProperty("host", exposeHost);
            }
         }
         if (encryptionType != null) {
            GenericKubernetesResource security = new GenericKubernetesResource();
            spec.setAdditionalProperty("security", security);
            GenericKubernetesResource encryption = new GenericKubernetesResource();
            security.setAdditionalProperty("endpointEncryption", encryption);
            encryption.setAdditionalProperty("type", encryptionType);
            if (encryptionType == Encryption.Secret) {
               if (encryptionSecret != null) {
                  encryption.setAdditionalProperty("certSecretName", encryptionSecret);
               } else {
                  throw Messages.MSG.encryptionTypeRequiresSecret(encryptionType);
               }
            }
         }
         if (specProperties != null) {
            specProperties.forEach((k, v) -> Kube.setProperty(spec, v, k.split("\\.")));
         }
         client.genericKubernetesResources(INFINISPAN_CLUSTER_CRD).inNamespace(namespace).create(infinispan);
         return 0;
      }

   }
}
