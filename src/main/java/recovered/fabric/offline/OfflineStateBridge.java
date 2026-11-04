package recovered.fabric.offline;

import client.data.DaamkyFriend;
import java.util.Set;
import recovered.fabric.diagnostic.DaamkyDlcLog;

/**
 * Loads the friend list at system initialization. The original build reached this through the
 * obfuscated "eV" class, which the recovery turned into {@link DaamkyFriend}.
 */
public final class OfflineStateBridge {
   private OfflineStateBridge() {
   }

   public static void initialize() {
      try {
         DaamkyFriend friends = DaamkyFriend.getInstance();
         friends.update2();
         Set<String> names = friends.getSet();
         DaamkyDlcLog.info("offline friends loaded count=" + names.size());
      } catch (Throwable throwable) {
         DaamkyDlcLog.errorOnce("offline friends load", root(throwable));
      }
   }

   private static Throwable root(Throwable error) {
      return error.getCause() == null ? error : error.getCause();
   }
}
