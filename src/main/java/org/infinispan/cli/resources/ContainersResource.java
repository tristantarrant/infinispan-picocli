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
public class ContainersResource extends AbstractResource {
   public static final String NAME = "containers";

   ContainersResource(Resource parent) {
      super(parent, NAME);
   }

   @Override
   public Iterable<String> getChildrenNames() throws IOException {
      return containerNames();
   }

   private List<String> containerNames() throws IOException {
      return parseBody(fetch(() -> client().cacheManagers()), List .class);
   }

   @Override
   public Resource getChild(String name) throws IOException {
      if (Resource.PARENT.equals(name)) {
         return parent;
      } else if (containerNames().contains(name)) {
         return new ContainerResource(this, name);
      } else {
         throw Messages.MSG.noSuchResource(name);
      }
   }
}
