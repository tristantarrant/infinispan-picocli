package org.infinispan.cli.commands.rest;

import static org.infinispan.cli.logging.Messages.MSG;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CdContextCompleter;
import org.infinispan.cli.resources.CacheResource;
import org.infinispan.cli.resources.ContainerResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/

@CommandLine.Command(name = "stats", description = "Shows cache and container statistics")
public class Stats extends RestCallable {

   @CommandLine.Parameters(description = "The path of the resource", completionCandidates = CdContextCompleter.class)
   String name;

   @Override
   protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
      try {
         Resource r = resource.getResource(name);
         if (r instanceof CacheResource) {
            return client.cache(r.getName()).stats();
         } else if (r instanceof ContainerResource) {
            return client.cacheManager(r.getName()).stats();
         } else {
            String name = r.getName();
            throw MSG.invalidResource(name.isEmpty() ? "/" : name);
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
