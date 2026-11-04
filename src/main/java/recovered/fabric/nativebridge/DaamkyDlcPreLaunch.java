package recovered.fabric.nativebridge;

import b.Boot;
import java.nio.file.Path;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import recovered.fabric.diagnostic.DaamkyDlcLog;

public final class DaamkyDlcPreLaunch implements PreLaunchEntrypoint {
   public void onPreLaunch() {
      boolean enabled = NativeLibrary.enabled();
      DaamkyDlcLog.info("preLaunch nativeEnabled=" + enabled);
      if (enabled) {
         try {
            Path library = NativeLibrary.extract().toAbsolutePath();
            DaamkyDlcLog.info("native extracted " + library);
            System.load(library.toString());
            DaamkyDlcLog.info("native library loaded");
            Boot.bootstrap();
            DaamkyDlcLog.info("preLaunch completed");
         } catch (Throwable throwable) {
            DaamkyDlcLog.error("native initialization failed", throwable);
         }
      }
   }
}
