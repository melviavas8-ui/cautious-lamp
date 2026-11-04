package client.util;

import client.concurrent.DaamkyClient;
import net.fabricmc.api.ClientModInitializer;

public class ClientEntrypoint implements ClientModInitializer {
   public void onInitializeClient() {
      new DaamkyClient();
   }
}
