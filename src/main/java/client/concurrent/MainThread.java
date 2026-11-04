package client.concurrent;

import client.module.Feature;
import java.util.concurrent.ScheduledExecutorService;

public final class MainThread {
   private MainThread() {
   }

   public static void onRunnable(Runnable runnable) {
      if (runnable != null) {
         if (Feature.mc == null) {
            runnable.run();
         } else {
            Feature.mc.execute(runnable);
         }
      }
   }

   public static void onRunnable2(Runnable runnable) {
      DaamkyClient daamkyclient = DaamkyClient.getInstance();
      if (daamkyclient != null) {
         ScheduledExecutorService scheduledexecutorservice = daamkyclient.getScheduledExecutorService();
         if (scheduledexecutorservice != null) {
            scheduledexecutorservice.execute(runnable);
         }
      }
   }
}
