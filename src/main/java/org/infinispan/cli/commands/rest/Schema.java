package org.infinispan.cli.commands.rest;

import java.nio.file.Path;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestEntity;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.commons.dataconversion.MediaType;

import picocli.CommandLine;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/

@CommandLine.Command(name = "schema", description = "Manipulates protobuf schemas")
public class Schema extends RestCallable {
   @CommandLine.Parameters(description = "The name of the schema", arity = "1")
   String name;

   @CommandLine.Option(names = {"-u", "--upload"}, description = "The protobuf file to upload")
   Path upload;

   @Override
   protected CompletionStage<RestResponse> call(RestClient client, Resource resource) {
      return client.schemas().put(name, RestEntity.create(MediaType.TEXT_PLAIN, upload.toFile()));
   }
}
