package org.infinispan.cli.resources;

import static org.infinispan.cli.util.Utils.fetch;
import static org.infinispan.cli.util.Utils.parseBody;

import java.io.IOException;
import java.util.List;

import org.infinispan.cli.logging.Messages;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
public class ClusterResource extends AbstractResource {
   static final String NAME = "cluster";

   ClusterResource(Resource parent) {
      super(parent, NAME);
   }

   @Override
   public Iterable<String> getChildrenNames() throws IOException {
      return clusterNodes();
   }

   private List<String> clusterNodes() throws IOException {
      return parseBody(fetch(() -> client().caches()), List.class);
   }

   @Override
   public Resource getChild(String name) throws IOException {
      if (Resource.PARENT.equals(name)) {
         return parent;
      } else if (clusterNodes().contains(name)) {
         return new NodeResource(this, name);
      } else {
         throw Messages.MSG.noSuchResource(name);
      }
   }
}
