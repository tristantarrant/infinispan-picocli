package org.infinispan.cli.commands.rest;

import java.util.concurrent.CompletionStage;

import org.infinispan.cli.completers.CounterCompleter;
import org.infinispan.cli.resources.CounterResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestCounterClient;
import org.infinispan.client.rest.RestResponse;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
@CommandLine.Command(name = "cas", description = "Compares and sets counter values")
public class Cas extends RestCallable {

   @CommandLine.Parameters(completionCandidates = CounterCompleter.class)
   String counter;

   @CommandLine.Option(names = {"-q", "--quiet"}, description = "Does not display the value")
   boolean quiet;

   @CommandLine.Option(names = {"-e", "--expect"}, required = true, description = "The expected value")
   long expect;

   @CommandLine.Option(names = {"-v", "--value"}, required = true, description = "The new value")
   long value;

   @Override
   protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
      RestCounterClient cc = counter != null ? client.counter(counter) : client.counter(CounterResource.counterName(resource));
      if (quiet) {
         return cc.compareAndSet(expect, value);
      } else {
         return cc.compareAndSwap(expect, value);
      }
   }
}
