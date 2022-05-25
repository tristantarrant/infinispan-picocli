package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CounterCompleter;
import org.infinispan.cli.resources.CounterResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/

@CommandLine.Command(name = "reset", description = "Resets a counter to its initial value")
public class Reset extends RestCallable {

   @CommandLine.Parameters(completionCandidates = CounterCompleter.class)
   String counter;


   @Override
   protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
      return client.counter(counter == null ? CounterResource.counterName(resource) : counter).reset();
   }
}
