package recovered.fabric.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class DeviceIdentity {
   private static final String value = create();

   private DeviceIdentity() {
   }

   public static String get() {
      return value;
   }

   private static String create() {
      String source = String.join("|", property("user.name"), property("os.name"), property("os.arch"), environment("COMPUTERNAME"));

      try {
         byte[] bytes = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
         return HexFormat.of().formatHex(bytes);
      } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
         return Integer.toHexString(source.hashCode());
      }
   }

   private static String property(String name) {
      return System.getProperty(name, "unknown");
   }

   private static String environment(String name) {
      return System.getenv().getOrDefault(name, "unknown");
   }
}
