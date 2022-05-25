package org.infinispan.cli.commands.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.ConnectorCompleter;
import org.infinispan.cli.completers.IpFilterRuleCompleter;
import org.infinispan.cli.logging.Messages;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.IpFilterRule;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.1
 **/
@CommandLine.Command(name = Connector.CMD, description = "Performs operations on protocol connectors", subcommands = {Connector.Ls.class, Connector.Describe.class, Connector.Start.class, Connector.Stop.class, Connector.IpFilter.class})
public class Connector {

   public static final String CMD = "connector";
   public static final String TYPE = "type";
   public static final String NAME = "name";

   @CommandLine.Command(name = "ls", description = "Lists connectors")
   public static class Ls extends RestCallable {
      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.server().connectorNames();
      }
   }

   @CommandLine.Command(name = "describe", description = "Describes a connector")
   public static class Describe extends RestCallable {

      @CommandLine.Parameters(completionCandidates = ConnectorCompleter.class, arity = "1")
      String name;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.server().connector(name);
      }
   }

   @CommandLine.Command(name = "start", description = "Starts a connector")
   public static class Start extends RestCallable {

      @CommandLine.Parameters(completionCandidates = ConnectorCompleter.class, arity = "1")
      String name;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.server().connectorStart(name);
      }
   }

   @CommandLine.Command(name = "stop", description = "Stops a connector")
   public static class Stop extends RestCallable {

      @CommandLine.Parameters(completionCandidates = ConnectorCompleter.class, arity = "1")
      String name;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.server().connectorStop(name);
      }
   }

   @CommandLine.Command(name = "ipfilter", description = "Manages connector IP filters", subcommands = {IpFilter.Ls.class, IpFilter.Clear.class, IpFilter.Set.class})
   public static class IpFilter {

      @CommandLine.Command(name = "ls", description = "List all IP filters on a connector")
      public static class Ls extends RestCallable {

         @CommandLine.Parameters(completionCandidates = ConnectorCompleter.class, arity = "1")
         String name;

         @Override
         protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
            return client.server().connectorIpFilters(name);
         }
      }

      @CommandLine.Command(name = "clear", description = "Removes all IP Filters from a connector")
      public static class Clear extends RestCallable {

         @CommandLine.Parameters(completionCandidates = ConnectorCompleter.class, arity = "1")
         String name;

         @Override
         protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
            return client.server().connectorIpFiltersClear(name);
         }
      }

      @CommandLine.Command(name = "set", description = "Sets IP Filters on a connector")
      public static class Set extends RestCallable {

         @CommandLine.Parameters(completionCandidates = ConnectorCompleter.class, arity = "1")
         String name;

         @CommandLine.Option(names = {"-r", "--rule"}, description = "One or more filter rules as \"[ACCEPT|REJECT]/CIDR\"", completionCandidates = IpFilterRuleCompleter.class, arity = "1..*")
         String[] rules;

         @Override
         protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
            List<IpFilterRule> filterRules = new ArrayList<>(rules.length);
            for (String rule : rules) {
               int i = rule.indexOf('/');
               if (i < 0) {
                  throw Messages.MSG.illegalFilterRule(rule);
               }
               IpFilterRule.RuleType ruleType = IpFilterRule.RuleType.valueOf(rule.substring(0, i));
               filterRules.add(new IpFilterRule(ruleType, rule.substring(i + 1)));
            }
            return client.server().connectorIpFilterSet(name, filterRules);
         }
      }
   }
}
