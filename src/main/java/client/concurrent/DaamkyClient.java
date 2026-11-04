package client.concurrent;

import b.ModInitializer;
import client.audio.SoundEngine;
import client.network.ServerUtil;
import client.transform.ClassRedefiner;
import client.util.HashUtil;
import client.util.InputCallbacks;
import client.util.KeyboardState;
import client.util.ModuleDispatcher;
import client.util.StaffChecks;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModInitializer
public class DaamkyClient {
   public static DaamkyClient INSTANCE;
   private ModuleRegistry moduleRegistry;
   private KeyboardState keyboardState;
   private ModuleDispatcher moduleDispatcher;
   private InputCallbacks inputCallbacks;
   private final ScheduledExecutorService scheduledExecutorService = new GuardedScheduler(Executors.newSingleThreadScheduledExecutor());
   private ConfigManager configManager;
   private HashUtil hashUtil;
   private AssetLoader assetLoader;
   private final boolean flag = false;

   public DaamkyClient() {
      if (INSTANCE == null) {
         this.update3();
      }
   }

   private void update() {
      INSTANCE = this;
      AssetIndex.update();
      SoundEngine.getInstance().update5();
      SoundEngine.getInstance().setFlag3(true);
      this.moduleRegistry = new ModuleRegistry();
      this.configManager = new ConfigManager(this.moduleRegistry);
      this.hashUtil = new HashUtil(this.configManager, this.scheduledExecutorService);
      this.moduleDispatcher = new ModuleDispatcher();
      this.moduleDispatcher.setModuleRegistry2(this.moduleRegistry);
      this.keyboardState = KeyboardState.getKeyboardState();
      this.inputCallbacks = new InputCallbacks();
      new ClientTicker(this.scheduledExecutorService);
      this.assetLoader = new AssetLoader();
      this.keyboardState.update3();
      this.scheduledExecutorService.schedule(() -> this.moduleRegistry.update7(), 3L, TimeUnit.SECONDS);
      ClassRedefiner.update5();
      ClassRedefiner.update2();
      SoundEngine.getInstance().setFlag3(false);
   }

   public ScheduledExecutorService getScheduledExecutorService() {
      return this.scheduledExecutorService;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public HashUtil getHashUtil() {
      return this.hashUtil;
   }

   public AssetLoader getAssetLoader() {
      return this.assetLoader;
   }

   public boolean check() {
      return false;
   }

   public InputCallbacks getInputCallbacks() {
      return this.inputCallbacks;
   }

   public static DaamkyClient getInstance() {
      return INSTANCE;
   }

   public ModuleRegistry getModuleRegistry() {
      return this.moduleRegistry;
   }

   public KeyboardState getKeyboardState() {
      return this.keyboardState;
   }

   public ModuleDispatcher getModuleDispatcher() {
      return this.moduleDispatcher;
   }

   private void update3() {
      try {
         ServerUtil.update2();
         boolean flagx = StaffChecks.check();
         if (!flagx) {
            System.exit(1);
            return;
         }

         this.update();
      } catch (Exception exception) {
         System.exit(1);
      }
   }
}
