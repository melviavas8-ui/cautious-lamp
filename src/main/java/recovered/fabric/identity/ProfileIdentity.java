package recovered.fabric.identity;

public final class ProfileIdentity {
   private ProfileIdentity() {
   }

   public static String username() {
      return "source by @fuckahivina";
   }

   public static int uid() {
      return 0;
   }

   public static String hwid() {
      return DeviceIdentity.get();
   }

   public static String role() {
      return "Developer$$";
   }

   public static String subscription() {
      return "Lifetime";
   }
}
