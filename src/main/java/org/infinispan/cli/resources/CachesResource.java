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
public class CachesResource extends AbstractResource {
   public static final String NAME = "caches";

   protected CachesResource(Resource parent) {
      super(parent, NAME);
   }

   @Override
   public Iterable<String> getChildrenNames() throws IOException {
      return cacheNames();
   }

   private List<String> cacheNames() throws IOException {
      return parseBody(fetch(() -> client().caches()), List.class);
   }

   @Override
   public Resource getChild(String name) throws IOException {
      if (Resource.PARENT.equals(name)) {
         return parent;
      } else if (cacheNames().contains(name)) {
         return new CacheResource(this, name);
      } else {
         throw Messages.MSG.noSuchResource(name);
      }
   }
}
