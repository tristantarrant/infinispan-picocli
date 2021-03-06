package org.infinispan.cli.resources;

import static org.infinispan.cli.util.Utils.fetch;
import static org.infinispan.cli.util.Utils.parseBody;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
public class CounterResource extends AbstractResource {
   public CounterResource(CountersResource parent, String name) {
      super(parent, name);
   }

   @Override
   public Iterable<String> getChildrenNames() throws IOException {
      return Collections.singletonList(parseBody(fetch(() -> client().counter(name).get()), String.class));
   }

   @Override
   public boolean isLeaf() {
      return true;
   }

   @Override
   public String describe() throws IOException {
      return parseBody(fetch(() -> client().counter(name).configuration()), String.class);
   }

   public static String counterName(Resource resource) {
      return resource.findAncestor(CounterResource.class).getName();
   }
}
