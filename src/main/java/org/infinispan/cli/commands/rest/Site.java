package org.infinispan.cli.commands.rest;

import static org.infinispan.cli.logging.Messages.MSG;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CacheCompleter;
import org.infinispan.cli.completers.SiteCompleter;
import org.infinispan.cli.resources.CacheResource;
import org.infinispan.cli.resources.ContainerResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestCacheClient;
import org.infinispan.client.rest.RestCacheManagerClient;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.configuration.cache.XSiteStateTransferMode;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/

@CommandLine.Command(name = "site", description = "Manages backup sites", subcommands = {Site.Status.class, Site.BringOnline.class, Site.TakeOffline.class, Site.PushSiteState.class, Site.CancelPushState.class, Site.CancelReceiveState.class, Site.PushSiteStatus.class, Site.ClearPushStateStatus.class, Site.View.class, Site.Name.class, Site.StateTransferMode.class, Site.IsRelayNode.class, Site.RelayNodes.class})
public class Site {

   static class Exclusive {
      @CommandLine.Option(names = {"-c", "--cache"}, completionCandidates = CacheCompleter.class, description = "The cache name.")
      String cache;

      @CommandLine.Option(names = {"-a", "--all-caches"}, description = "Invoke operation in all caches.")
      boolean allCaches;
   }

   @CommandLine.Command(name = "status", description = "Shows site status")
   public static class Status extends RestCallable {
      @CommandLine.ArgGroup(multiplicity = "1")
      Exclusive exclusive;

      @CommandLine.Option(names = {"-s", "--site"}, completionCandidates = SiteCompleter.class, description = "The remote backup name.")
      String site;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         if (exclusive.allCaches) {
            RestCacheManagerClient cm = restCacheManagerClient(client, resource);
            return site == null ? cm.backupStatuses() : cm.backupStatus(site);
         }
         RestCacheClient c = restCacheClient(client, resource, exclusive.cache);
         return site == null ? c.xsiteBackups() : c.backupStatus(site);
      }
   }

   @CommandLine.Command(name = "bring-online", description = "Brings a site online")
   public static class BringOnline extends RestCallable {
      @CommandLine.ArgGroup(multiplicity = "1")
      Exclusive exclusive;

      @CommandLine.Option(required = true, names = {"-s", "--site"}, completionCandidates = SiteCompleter.class, description = "The remote backup name.")
      String site;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return exclusive.allCaches ?
               restCacheManagerClient(client, resource).bringBackupOnline(site) :
               restCacheClient(client, resource, exclusive.cache).bringSiteOnline(site);
      }
   }

   @CommandLine.Command(name = "take-offline", description = "Takes a site offline")
   public static class TakeOffline extends RestCallable {
      @CommandLine.ArgGroup(multiplicity = "1")
      Exclusive exclusive;

      @CommandLine.Option(required = true, names = {"-s", "--site"}, completionCandidates = SiteCompleter.class, description = "The remote backup name.")
      String site;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return exclusive.allCaches ?
               restCacheManagerClient(client, resource).takeOffline(site) :
               restCacheClient(client, resource, exclusive.cache).takeSiteOffline(site);
      }
   }

   @CommandLine.Command(name = "push-site-state", description = "Starts pushing state to a site")
   public static class PushSiteState extends RestCallable {
      @CommandLine.ArgGroup(multiplicity = "1")
      Exclusive exclusive;

      @CommandLine.Option(required = true, names = {"-s", "--site"}, completionCandidates = SiteCompleter.class, description = "The remote backup name.")
      String site;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return exclusive.allCaches ?
               restCacheManagerClient(client, resource).pushSiteState(site) :
               restCacheClient(client, resource, exclusive.cache).pushSiteState(site);
      }
   }

   @CommandLine.Command(name = "cancel-push-state", description = "Cancels pushing state to a site")
   public static class CancelPushState extends RestCallable {
      @CommandLine.ArgGroup(multiplicity = "1")
      Exclusive exclusive;

      @CommandLine.Option(required = true, names = {"-s", "--site"}, completionCandidates = SiteCompleter.class, description = "The remote backup name.")
      String site;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return exclusive.allCaches ?
               restCacheManagerClient(client, resource).cancelPushState(site) :
               restCacheClient(client, resource, exclusive.cache).cancelPushState(site);
      }
   }

   @CommandLine.Command(name = "cancel-receive-state", description = "Cancels receiving state to a site")
   public static class CancelReceiveState extends RestCallable {
      @CommandLine.Option(required = true, names = {"-c", "--cache"}, completionCandidates = CacheCompleter.class, description = "The cache name.")
      String cache;

      @CommandLine.Option(required = true, names = {"-s", "--site"}, completionCandidates = SiteCompleter.class, description = "The remote backup name.")
      String site;


      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.cache(cache).cancelReceiveState(site);
      }
   }

   @CommandLine.Command(name = "push-site-status", description = "Shows the status of pushing to a site")
   public static class PushSiteStatus extends RestCallable {
      @CommandLine.Option(required = true, names = {"-c", "--cache"}, completionCandidates = CacheCompleter.class, description = "The cache name.")
      String cache;


      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.cache(getCacheName(resource, cache).orElseThrow(MSG::missingCacheName)).pushStateStatus();
      }
   }

   @CommandLine.Command(name = "clear-push-site-status", description = "Clears the push state status")
   public static class ClearPushStateStatus extends RestCallable {
      @CommandLine.Option(required = true, names = {"-c", "--cache"}, completionCandidates = CacheCompleter.class, description = "The cache name.")
      String cache;


      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         return client.cache(cache).clearPushStateStatus();
      }
   }

   @CommandLine.Command(name = "view", description = "Prints the global sites view")
   public static class View extends ManagerInfoCallable {
      @Override
      protected String call(Map<String, ?> info) {
         return (String) info.get("sites_view");
      }
   }

   @CommandLine.Command(name = "name", description = "Prints the local site name")
   public static class Name extends ManagerInfoCallable {
      @Override
      protected String call(Map<String, ?> info) {
         return (String) info.get("local_site");
      }
   }

   @CommandLine.Command(name = "state-transfer-mode", description = "Controls the cross-site state transfer mode.", subcommands = {GetStateTransferMode.class, SetStateTransferMode.class})
   public static class StateTransferMode {
   }

   @CommandLine.Command(name = "get", description = "Retrieves the cross-site state transfer mode.")
   public static class GetStateTransferMode extends RestCallable {

      @CommandLine.Option(names = {"-c", "--cache"}, completionCandidates = CacheCompleter.class, description = "The cache name.")
      String cache;

      @CommandLine.Option(names = {"-s", "--site"}, required = true, completionCandidates = SiteCompleter.class, description = "The remote backup name.")
      String site;

      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         String cacheName = getCacheName(resource, cache).orElseThrow(MSG::illegalContext);
         return client.cache(cacheName).xSiteStateTransferMode(site);
      }
   }

   @CommandLine.Command(name = "set", description = "Sets the cross-site state transfer mode.")
   public static class SetStateTransferMode extends RestCallable {

      @CommandLine.Option(names = {"-c", "--cache"}, completionCandidates = CacheCompleter.class, description = "The cache name.")
      String cache;

      @CommandLine.Option(names = {"-s", "--site"}, required = true, completionCandidates = SiteCompleter.class, description = "The remote backup name.")
      String site;

      @CommandLine.Option(names = {"-m", "--mode"}, required = true, description = "The state transfer mode to set.")
      protected XSiteStateTransferMode mode;


      @Override
      protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
         String cacheName = getCacheName(resource, cache).orElseThrow(MSG::illegalContext);
         return client.cache(cacheName).xSiteStateTransferMode(site, mode);
      }
   }

   @CommandLine.Command(name = "relay-nodes", description = "Returns the list of relay nodes.")
   public static class RelayNodes extends ManagerInfoCallable {
      @Override
      protected String call(Map<String, ?> info) {
         return (String) info.get("relay_nodes_address");
      }
   }

   @CommandLine.Command(name = "is-relay-node", description = "Returns true if the node handles relay messages between clusters.")
   public static class IsRelayNode extends ManagerInfoCallable {
      @Override
      protected String call(Map<String, ?> info) {
         boolean isRelayNode = info.containsKey("relay_node") && (boolean) info.get("relay_node");
         return Boolean.toString(isRelayNode);
      }
   }

   private static RestCacheManagerClient restCacheManagerClient(RestClient client, Resource resource) {
      return ContainerResource.findContainerName(resource).map(client::cacheManager).orElseThrow(MSG::illegalContext);
   }

   private static RestCacheClient restCacheClient(RestClient client, Resource resource, String cacheName) {
      return cacheName == null ? CacheResource.findCacheName(resource).map(client::cache).orElseThrow(MSG::missingCacheName) : client.cache(cacheName);
   }
}
