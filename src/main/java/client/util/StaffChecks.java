package client.util;

import client.network.AuthResult;
import client.network.ConfigApi;
import java.util.Collections;
import java.util.Set;

public class StaffChecks {
   public static Set getSet() {
      return Collections.emptySet();
   }

   public static boolean check() {
      AuthResult authresult = ConfigApi.getAuthResult();
      return authresult != null && authresult.isFlag() && authresult.check();
   }
}
