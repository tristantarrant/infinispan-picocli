package org.infinispan.cli.completers;

import java.util.Collections;
import java.util.Iterator;

/**
 * @since 14.0
 **/
public class ConnectorCompleter implements Iterable<String> {
   @Override
   public Iterator<String> iterator() {
      return Collections.emptyIterator();
   }
}
