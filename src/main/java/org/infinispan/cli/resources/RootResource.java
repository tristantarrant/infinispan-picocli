package org.infinispan.cli.resources;

import java.util.Arrays;

import org.infinispan.cli.logging.Messages;
import org.infinispan.client.rest.RestClient;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
public class RootResource extends AbstractResource {
   private final RestClient client;

   RootResource(RestClient client) {
      super(null, "");
      this.client = client;
   }

   @Override
   public Iterable<String> getChildrenNames() {
      return Arrays.asList(ContainersResource.NAME, ClusterResource.NAME, ServerResource.NAME);
   }

   @Override
   public Resource getChild(String name) {
      switch (name) {
         case ContainersResource.NAME:
            return new ContainersResource(this);
         case ClusterResource.NAME:
            return new ClusterResource(this);
         case ServerResource.NAME:
            return new ServerResource(this);
         default:
            throw Messages.MSG.noSuchResource(name);
      }
   }

   @Override
   RestClient client() {
      return client;
   }
}
