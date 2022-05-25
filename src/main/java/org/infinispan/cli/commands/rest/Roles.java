package org.infinispan.cli.commands.rest;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.1
 **/

@CommandLine.Command(name = "roles", description = "Manages security roles", subcommands = {Roles.Ls.class, Roles.Grant.class, Roles.Deny.class})
public class Roles {

   @CommandLine.Command(name = "ls", description = "Lists roles assigned to principals")
   public static class Ls extends RestCallable {

      @CommandLine.Parameters(description = "The principal for which the roles should be listed. If unspecified all available roles are listed.")
      String principal;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.security().listRoles(principal);
      }
   }

   @CommandLine.Command(name = "grant", description = "Grants roles to principals")
   public static class Grant extends RestCallable {

      @CommandLine.Parameters(description = "The principal to which the roles should be granted", arity = "1")
      String principal;

      @CommandLine.Option(names = {"-r", "--role"}, arity = "1..*")
      String[] roles;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.security().grant(principal, Arrays.asList(roles));
      }
   }

   @CommandLine.Command(name = "deny", description = "Denies roles to principals")
   public static class Deny extends RestCallable {

      @CommandLine.Parameters(description = "The principal to which the roles should be denied", arity = "1")
      String principal;

      @CommandLine.Option(names = {"-r", "--role"}, arity = "1..*")
      String[] roles;


      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.security().deny(principal, Arrays.asList(roles));
      }
   }
}
