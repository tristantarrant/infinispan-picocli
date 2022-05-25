package org.infinispan.cli.resources;

import static org.infinispan.cli.logging.Messages.MSG;
import static org.infinispan.cli.util.Utils.fetch;
import static org.infinispan.cli.util.Utils.parseBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.infinispan.client.rest.RestTaskClient;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.1
 **/
public class TaskResource extends AbstractResource {
   public TaskResource(TasksResource parent, String name) {
      super(parent, name);
   }

   @Override
   public boolean isLeaf() {
      return true;
   }

   @Override
   public String describe() throws IOException {
      List<Map<String, Object>> list = parseBody(fetch(() -> client().tasks().list(RestTaskClient.ResultType.ALL)), List.class);
      Optional<Map<String, Object>> task = list.stream().filter(i -> name.equals(i.get("name"))).findFirst();
      return task.map(Object::toString).orElseThrow(() -> MSG.noSuchResource(name));
   }
}
