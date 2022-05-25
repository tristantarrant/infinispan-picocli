package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 13.0
 **/
@CommandLine.Command(name = "caches", description = "Lists the available cache names")
public class Caches extends RestCallable {

   @Override
   protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
      return client.caches();
   }
}
