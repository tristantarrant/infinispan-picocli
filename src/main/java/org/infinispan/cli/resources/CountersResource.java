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
public class CountersResource extends AbstractResource {
   public static final String NAME = "counters";

   protected CountersResource(Resource parent) {
      super(parent, NAME);
   }

   @Override
   public Iterable<String> getChildrenNames() throws IOException {
      return counterNames();
   }

   private List<String> counterNames() throws IOException {
      return parseBody(fetch(() -> client().counters()), List.class);
   }

   @Override
   public Resource getChild(String name) throws IOException {
      if (Resource.PARENT.equals(name)) {
         return parent;
      } else if (counterNames().contains(name)) {
         return new CounterResource(this, name);
      } else {
         throw Messages.MSG.noSuchResource(name);
      }
   }
}
