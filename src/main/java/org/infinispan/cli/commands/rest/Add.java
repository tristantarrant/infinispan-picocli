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
@CommandLine.Command(name = "add", description = "Adds/subtracts a value to/from a counter")
public class Add extends RestCallable {

   @CommandLine.Parameters(description = "The name of the counter", completionCandidates = CounterCompleter.class)
   String counter;

   @CommandLine.Option(names = {"-q", "--quiet"}, description = "Does not display the value")
   boolean quiet;

   @CommandLine.Option(names = {"-d", "--delta"}, description = "The delta to add/subtract from/to the value. Defaults to adding 1", defaultValue = "1")
   long delta;

   @Override
   protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
      return client.counter(counter != null ? counter : CounterResource.counterName(resource)).add(delta);
   }

   @Override
   public ResponseMode getResponseMode() {
      return quiet ? ResponseMode.QUIET : ResponseMode.BODY;
   }
}
