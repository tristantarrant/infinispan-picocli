package org.infinispan.cli.commands.rest;

import static org.infinispan.cli.logging.Messages.MSG;
import static org.infinispan.cli.util.Utils.fetch;
import static org.infinispan.cli.util.Utils.parseBody;
import static org.infinispan.cli.util.Utils.parseHeaders;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.infinispan.cli.Context;
import org.infinispan.cli.commands.ContextAwareCallable;
import org.infinispan.cli.resources.CacheResource;
import org.infinispan.cli.resources.Resource;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.commons.dataconversion.internal.Json;
import org.infinispan.commons.util.Util;

/**
 * @since 14.0
 **/
public abstract class RestCallable extends ContextAwareCallable<String> {
   public enum ResponseMode {QUIET, BODY, FILE, HEADERS};

   protected abstract CompletionStage<RestResponse> call(RestClient client, Resource resource);

   public ResponseMode getResponseMode() {
      return ResponseMode.BODY;
   }

   @Override
   protected String call(Context context) throws Exception {
      RestResponse r = fetch(call(context.client(), context.resource()));
      StringBuilder sb = new StringBuilder();
      switch (getResponseMode()) {
         case BODY:
            String body = parseBody(r, String.class);
            if (body != null) {
               sb.append(body);
            }
            break;
         case FILE:
            String contentDisposition = parseHeaders(r).get("Content-Disposition").get(0);
            String filename = Util.unquote(contentDisposition.split("filename=")[1]);
            Path file = context.workingDir().resolve(filename);

            try (OutputStream os = Files.newOutputStream(file); InputStream is = parseBody(r, InputStream.class)) {
               byte[] buffer = new byte[8 * 1024];
               int bytesRead;
               while ((bytesRead = is.read(buffer)) != -1) {
                  os.write(buffer, 0, bytesRead);
               }
               sb.append(MSG.downloadedFile(filename));
            }
         case QUIET:
            break;
         case HEADERS:
            sb.append(Json.make(parseHeaders(r)).toPrettyString());
            break;
         default:
            throw new IllegalArgumentException(getResponseMode().name());
      }
      return sb.toString();
   }

   public Optional<String> getCacheName(Resource resource, String name) {
      return name == null ? CacheResource.findCacheName(resource) : Optional.of(name);
   }
}
