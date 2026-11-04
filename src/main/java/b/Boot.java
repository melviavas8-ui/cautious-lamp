package b;

import client.transform.ClassRedefiner;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;
import recovered.fabric.diagnostic.DaamkyDlcLog;
import recovered.fabric.offline.OfflineRuntime;

public final class Boot {
   private static final AtomicBoolean bootstrapped = new AtomicBoolean();
   private static volatile boolean sbProbed;
   private static volatile boolean sbAvailable;
   private static volatile boolean sbCreated;
   private static volatile boolean sbWantActive;

   private Boot() {
   }

   public static void shutdown() {
      DaamkyDlcLog.info("Boot.shutdown");
      OfflineRuntime.shutdown();
   }

   public static String get() {
      DaamkyDlcLog.once("hwid", "Boot.get requested");
      return OfflineRuntime.deviceId();
   }

   public static String call(int action, String payload, String session) {
      DaamkyDlcLog.info("Boot.call action=" + action + " session=" + (session != null));
      String response = OfflineRuntime.call(action, payload, session);
      DaamkyDlcLog.info("Boot.call action=" + action + " responseLength=" + response.length());
      return response;
   }

   public static String ip() {
      return OfflineRuntime.ip();
   }

   public static String pcName() {
      return OfflineRuntime.pcName();
   }

   public static String wsUrl() {
      return OfflineRuntime.wsUrl();
   }

   public static void bootstrap() {
      DaamkyDlcLog.info("Boot.bootstrap requested");
      if (!bootstrapped.compareAndSet(false, true)) {
         DaamkyDlcLog.info("Boot.bootstrap skipped");
      } else {
         String className = Boot.class.getName().replace('.', '/');
         int registered = 0;
         int failed = 0;

         for (Method method : Boot.class.getDeclaredMethods()) {
            if (Modifier.isNative(method.getModifiers()) && !method.getName().equals("reg")) {
               try {
                  String descriptor = descriptorOf(method);
                  reg(Boot.class, className, method.getName(), descriptor);
                  registered++;
                  DaamkyDlcLog.info("native registered " + method.getName() + descriptor);
               } catch (Throwable throwable) {
                  failed++;
                  DaamkyDlcLog.error("native registration failed " + method.getName(), throwable);
               }
            }
         }

         DaamkyDlcLog.info("native registration complete registered=" + registered + " failed=" + failed);
         installHooks();
         DaamkyDlcLog.info("Boot.bootstrap completed");
      }
   }

   private static String descriptorOf(Method method) {
      return DescriptorWriter.of(method);
   }

   private static void appendDesc(StringBuilder builder, Class<?> type) {
      DescriptorWriter.append(builder, type);
   }

   private static native void reg(Class<?> value, String text, String text2, String text3);

   public static native boolean nativeRedefineClass(Class<?> value, byte[] valueArray);

   public static native byte[] nativeDumpClassBytes(Class<?> value);

   public static native boolean nativePatchMethodReady();

   private static native boolean nativeAvailable();

   public static native boolean nativeUnload();

   private static native void nativePublish(int count, long time);

   private static native void nativeDestroy();

   private static native boolean nativeCreate(long time);

   private static native boolean nativeBeginFrame();

   private static native void nativeSetActive(boolean flag);

   public static synchronized boolean sbLoad() {
      if (sbProbed) {
         return sbAvailable;
      } else {
         sbProbed = true;

         try {
            sbAvailable = nativeAvailable();
         } catch (Throwable throwable) {
            DaamkyDlcLog.errorOnce("nativeAvailable", throwable);
            sbAvailable = false;
         }

         DaamkyDlcLog.info("shared buffer available=" + sbAvailable);
         return sbAvailable;
      }
   }

   public static synchronized boolean sbCreate(long window) {
      if (!sbAvailable) {
         return false;
      } else if (sbCreated) {
         return true;
      } else {
         try {
            sbCreated = nativeCreate(window);
            if (sbCreated) {
               nativeSetActive(sbWantActive);
            }
         } catch (Throwable throwable) {
            DaamkyDlcLog.errorOnce("nativeCreate", throwable);
            sbCreated = false;
         }

         DaamkyDlcLog.info("shared buffer created=" + sbCreated);
         return sbCreated;
      }
   }

   public static boolean sbBeginFrame() {
      try {
         return sbCreated && nativeBeginFrame();
      } catch (Throwable throwable) {
         DaamkyDlcLog.errorOnce("nativeBeginFrame", throwable);
         return false;
      }
   }

   public static void sbPublish(int texture, long fence) {
      if (sbCreated) {
         try {
            nativePublish(texture, fence);
         } catch (Throwable throwable) {
            DaamkyDlcLog.errorOnce("nativePublish", throwable);
         }
      }
   }

   public static void sbSetActive(boolean active) {
      sbWantActive = active;
      if (sbCreated) {
         try {
            nativeSetActive(active);
         } catch (Throwable throwable) {
            DaamkyDlcLog.errorOnce("nativeSetActive", throwable);
         }
      }
   }

   public static synchronized void sbDestroy() {
      sbWantActive = false;
      if (sbCreated) {
         sbCreated = false;

         try {
            nativeDestroy();
         } catch (Throwable throwable) {
            DaamkyDlcLog.errorOnce("nativeDestroy", throwable);
         }
      }
   }

   public static boolean isCallingOriginal() {
      try {
         return ClassRedefiner.check();
      } catch (Throwable throwable) {
         DaamkyDlcLog.error("hook installation check failed", throwable);
         return false;
      }
   }

   /**
    * The original build installed its hooks through the obfuscated "li.a()"; that class is the
    * recovered ClassRedefiner, so call it directly instead of resolving a name that no longer exists.
    */
   private static void installHooks() {
      DaamkyDlcLog.info("hook installation started");

      try {
         ClassRedefiner.update5();
         DaamkyDlcLog.info("hook installation completed");
      } catch (Throwable throwable) {
         DaamkyDlcLog.error("hook installation failed", throwable);
      }
   }
}
