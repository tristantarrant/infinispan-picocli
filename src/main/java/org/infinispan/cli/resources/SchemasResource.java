package org.infinispan.cli.resources;

import static org.infinispan.cli.util.Utils.getCacheKeys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.infinispan.cli.logging.Messages;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
public class SchemasResource extends AbstractResource {
   public static final String NAME = "schemas";
   public static final String PROTOBUF_METADATA_CACHE_NAME = "___protobuf_metadata";

   protected SchemasResource(Resource parent) {
      super(parent, NAME);
   }

   @Override
   public Iterable<String> getChildrenNames() throws IOException {
      List<String> schemas = new ArrayList<>();
      getCacheKeys(client(), PROTOBUF_METADATA_CACHE_NAME).forEach(schemas::add);
      return schemas;
   }

   @Override
   public Resource getChild(String name) {
      if (Resource.PARENT.equals(name)) {
         return parent;
      } else {
         throw Messages.MSG.noSuchResource(name);
      }
   }

   @Override
   public boolean isLeaf() {
      return true;
   }

   @Override
   public String describe() throws IOException {
      return NAME;//TODO
   }
}
