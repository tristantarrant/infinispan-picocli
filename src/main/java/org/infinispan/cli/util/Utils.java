package org.infinispan.cli.util;

import static org.infinispan.cli.logging.Messages.MSG;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.infinispan.cli.artifacts.MavenSettings;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.dataconversion.internal.Json;
import org.infinispan.commons.util.Util;

public class Utils {
   public static final int BUFFER_SIZE = 8192;

   public static String sha256(Path path) {
      return digest(path, "SHA-256");
   }

   public static String digest(Path path, String algorithm) {
      try (ByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
         MessageDigest digest = MessageDigest.getInstance(algorithm);
         if (channel instanceof FileChannel) {
            FileChannel fileChannel = (FileChannel) channel;
            MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            digest.update(byteBuffer);
         } else {
            ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
            while (channel.read(bb) != -1) {
               bb.flip();
               digest.update(bb);
               bb.flip();
            }
         }
         return Util.toHexString(digest.digest());
      } catch (NoSuchFileException e) {
         return null;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static Path downloadFile(URL url, Path dest, boolean verbose, boolean force) throws IOException {
      if (Files.exists(dest)) {
         if (force) {
            Files.delete(dest);
            if (verbose) {
               System.out.printf("Deleting previously downloaded '%s' for '%s'%n", dest, url);
            }
         } else {
            if (verbose) {
               System.out.printf("Using previously downloaded '%s' for '%s'%n", dest, url);
            }
            return dest;
         }
      }
      HttpURLConnection connection = (HttpURLConnection) MavenSettings.getSettings().openConnection(url);

      int statusCode = connection.getResponseCode();
      if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
         if (verbose) {
            System.out.printf("'%s' not found%n", url);
         }
         return null;
      }
      if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP
            || statusCode == HttpURLConnection.HTTP_MOVED_PERM) {
         url = new URL(connection.getHeaderField("Location"));
         dest = dest.resolveSibling(getFilenameFromURL(url));
         connection = (HttpURLConnection) url.openConnection();
      }

      try (InputStream bis = connection.getInputStream()) {
         Files.createDirectories(dest.getParent());
         Files.copy(bis, dest, StandardCopyOption.REPLACE_EXISTING);
         if (verbose) {
            System.out.printf("Downloaded '%s' to '%s'%n", url, dest);
         }
         return dest;
      }
   }

   public static String getFilenameFromURL(URL url) {
      String urlPath = url.getPath();
      return  urlPath.substring(urlPath.lastIndexOf('/') + 1);
   }

   public static RestResponse fetch(Supplier<CompletionStage<RestResponse>> responseFutureSupplier) throws IOException {
      return fetch(responseFutureSupplier.get());
   }

   public static RestResponse fetch(CompletionStage<RestResponse> responseFuture) throws IOException {
      try {
         return responseFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new IOException(e);
      } catch (ExecutionException e) {
         throw MSG.connectionFailed(e.getMessage());
      } catch (TimeoutException e) {
         throw new IOException(e);
      }
   }

   public static Map<String, List<String>> parseHeaders(RestResponse response) throws IOException {
      response = handleResponseStatus(response);
      if (response != null) {
         return response.headers();
      } else {
         return Collections.emptyMap();
      }
   }

   public static <T> T parseBody(RestResponse response, Class<T> returnClass) throws IOException {
      response = handleResponseStatus(response);
      if (response != null) {
         if (returnClass == InputStream.class) {
            return (T) response.getBodyAsStream();
         } else if (returnClass == String.class) {
            if (MediaType.APPLICATION_JSON.equals(response.contentType())) {
               Json json = Json.read(response.getBody());
               return (T) json.toPrettyString();
            } else {
               return (T) response.getBody();
            }
         } else {
            if (returnClass == Map.class) {
               return (T) Json.read(response.getBody()).asMap();
            }
            if (returnClass == List.class) {
               return (T) Json.read(response.getBody()).asList();
            }
         }
      }
      return null;
   }

   public static RestResponse handleResponseStatus(RestResponse response) throws IOException {
      switch (response.getStatus()) {
         case 200:
         case 201:
         case 202:
            return response;
         case 204:
            return null;
         case 401:
            throw MSG.unauthorized(response.getBody());
         case 403:
            throw MSG.forbidden(response.getBody());
         case 404:
            throw MSG.notFound(response.getBody());
         default:
            throw MSG.error(response.getBody());
      }
   }

   public static Iterable<String> getCacheKeys(RestClient client, String cache) throws IOException {
      return new IterableJsonReader(parseBody(fetch(() -> client.cache(cache).keys()), InputStream.class), s -> s == null || "_value".equals(s));
   }
}
