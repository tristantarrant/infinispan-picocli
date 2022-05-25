package org.infinispan.cli.commands.kubernetes;

import static org.infinispan.cli.commands.kubernetes.Kube.DEFAULT_CLUSTER_NAME;
import static org.infinispan.cli.commands.kubernetes.Kube.INFINISPAN_CLUSTER_CRD;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.infinispan.cli.Context;
import org.infinispan.cli.commands.CLI;
import org.infinispan.cli.commands.ContextAwareCallable;
import org.infinispan.cli.impl.DefaultShell;
import org.infinispan.cli.logging.Messages;

import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.0
 **/
@CommandLine.Command(name = "shell", description = "Initiates an interactive shell with a service.")
public class Shell extends ContextAwareCallable {

   @CommandLine.Option(names = {"-n", "--namespace"}, description = "Specifies the namespace where the cluster is created. Uses the default namespace if you do not specify one.")
   String namespace;

   @CommandLine.Option(names = {"-p", "--pod-name"}, description = "Specifies to which pod you connect.")
   String podName;

   @CommandLine.Option(names = {"-u", "--username"}, description = "The username to use when connecting")
   String username;

   @CommandLine.Parameters(description = "Specifies the name of the service to connect to. Defaults to '" + DEFAULT_CLUSTER_NAME + "'", defaultValue = DEFAULT_CLUSTER_NAME, arity = "0..1")
   String name;

   @Override
   protected Object call(Context context) throws Exception {
      KubernetesClient client = context.kubernetesClient();
      namespace = Kube.getNamespaceOrDefault(client, namespace);
      GenericKubernetesResource infinispan = client.genericKubernetesResources(INFINISPAN_CLUSTER_CRD).inNamespace(namespace).withName(name).get();
      if (infinispan == null) {
         throw Messages.MSG.noSuchService(name, namespace);
      }
      String endpointSecretName = Kube.getProperty(infinispan, "spec", "security", "endpointSecretName");
      String certSecretName = Kube.getProperty(infinispan, "spec", "security", "endpointEncryption", "certSecretName");

      Pod pod;
      if (podName == null) {
         pod = client.pods().inNamespace(namespace).withLabel("infinispan_cr", name).list()
               .getItems().stream().filter(p -> "running".equalsIgnoreCase(p.getStatus().getPhase())).findFirst().orElse(null);
      } else {
         pod = client.pods().inNamespace(namespace).withName(podName).get();
      }
      if (pod == null) {
         throw Messages.MSG.noRunningPodsInService(name);
      }
      // Port forwarding mode
      List<ContainerPort> ports = pod.getSpec().getContainers().get(0).getPorts();
      // Find the `infinispan` port
      ContainerPort containerPort = ports.stream().filter(p -> "infinispan".equals(p.getName())).findFirst().get();
      try (LocalPortForward portForward = client.pods().inNamespace(namespace).withName(pod.getMetadata().getName()).portForward(containerPort.getContainerPort())) {
         StringBuilder connection = new StringBuilder();
         List<String> args = new ArrayList<>();
         if (certSecretName != null) {
            connection.append("https://");
            Secret secret = Kube.getSecret(client, namespace, certSecretName);
            final byte[] cert;
            final String suffix;
            if (secret.getData().containsKey("keystore.p12")) {
               cert = Base64.getDecoder().decode(secret.getData().get("keystore.p12"));
               suffix = ".p12";
               String password = new String(Base64.getDecoder().decode(secret.getData().get("password")));
               args.add("-s");
               args.add(password);
            } else {
               cert = new String(Base64.getDecoder().decode(secret.getData().get("tls.crt"))).getBytes(StandardCharsets.UTF_8);
               suffix = ".pem";
            }
            Path certPath = Files.createTempFile("clitrust", suffix, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            Files.write(certPath, cert);
            args.add("-t");
            args.add(certPath.toString());
            args.add("--hostname-verifier");
            args.add(".*");
         } else {
            connection.append("http://");
         }
         if (endpointSecretName != null) {
            Secret secret = Kube.getSecret(client, namespace, endpointSecretName);
            Map<String, String> credentials = Kube.decodeOpaqueSecrets(secret);
            if (username == null) {
               if (credentials.size() != 1) {
                  throw Messages.MSG.usernameRequired();
               } else {
                  Map.Entry<String, String> entry = credentials.entrySet().iterator().next();
                  connection.append(entry.getKey());
                  connection.append(':');
                  connection.append(entry.getValue());
                  connection.append('@');
               }
            } else {
               connection.append(username);
               if (credentials.containsKey(username)) {
                  connection.append(':');
                  connection.append(credentials.get(username));
               }
               connection.append('@');
            }
         }
         InetAddress localAddress = portForward.getLocalAddress();
         if (localAddress.getAddress().length == 4) {
            connection.append(localAddress.getHostAddress());
         } else {
            connection.append('[').append(localAddress.getHostAddress()).append(']');
         }
         connection.append(':');
         connection.append(portForward.getLocalPort());
         args.add("-c");
         args.add(connection.toString());
         Messages.CLI.debugf("cli %s", args);
         CLI.main(new DefaultShell(), args.toArray(new String[0]), System.getProperties(), false);
         return 0;
      } catch (Throwable t) {
         context.printError(t);
         return 1;
      }
   }
}
