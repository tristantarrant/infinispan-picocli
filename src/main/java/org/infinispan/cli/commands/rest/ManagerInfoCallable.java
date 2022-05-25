package org.infinispan.cli.commands.rest;

import static org.infinispan.cli.util.Utils.fetch;
import static org.infinispan.cli.util.Utils.parseBody;

import java.util.Map;

import org.infinispan.cli.Context;
import org.infinispan.cli.commands.ContextAwareCallable;
import org.infinispan.cli.resources.ContainerResource;

/**
 * @since 14.0
 **/
public abstract class ManagerInfoCallable extends ContextAwareCallable<String> {

   @Override
   protected final String call(Context context) throws Exception {
      ContainerResource container = context.resource().findAncestor(ContainerResource.class);
      Map<String, ?> cacheManagerInfo = parseBody(fetch(() -> context.client().cacheManager(container.getName()).info()), Map.class);
      return call(cacheManagerInfo);
   }

   protected abstract String call(Map<String,?> info);
}
