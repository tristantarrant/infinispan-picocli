package org.infinispan.cli.resources;

import static org.infinispan.cli.util.Utils.fetch;
import static org.infinispan.cli.util.Utils.parseBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.infinispan.cli.logging.Messages;
import org.infinispan.client.rest.RestTaskClient;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.1
 **/
public class TasksResource extends AbstractResource {
   public static final String NAME = "tasks";

   protected TasksResource(Resource parent) {
      super(parent, NAME);
   }

   @Override
   public Iterable<String> getChildrenNames() throws IOException {
      return taskNames();
   }

   private List<String> taskNames() throws IOException {
      List<Map<String, String>> list = parseBody(fetch(() -> client().tasks().list(RestTaskClient.ResultType.ALL)), List.class);
      return list.stream().map(i -> i.get("name")).collect(Collectors.toList());
   }

   @Override
   public Resource getChild(String name) throws IOException {
      if (Resource.PARENT.equals(name)) {
         return parent;
      } else if (taskNames().contains(name)) {
         return new TaskResource(this, name);
      } else {
         throw Messages.MSG.noSuchResource(name);
      }
   }

   @Override
   public String describe() {
      return NAME;
   }
}
