package org.infinispan.cli.resources;

import static org.infinispan.cli.util.Utils.fetch;
import static org.infinispan.cli.util.Utils.parseHeaders;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.infinispan.commons.dataconversion.internal.Json;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
public class CacheKeyResource extends AbstractResource {
   public CacheKeyResource(CacheResource parent, String name) {
      super(parent, name);
   }

   @Override
   public boolean isLeaf() {
      return true;
   }

   @Override
   public String describe() throws IOException {
      Map<String, List<String>> headers = parseHeaders(fetch(() -> client().cache(getParent().getName()).head(name)));
      return Json.make(headers).toPrettyString();
   }
}
