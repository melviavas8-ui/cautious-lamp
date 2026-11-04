package client.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FriendNames {
   private static final FriendNames INSTANCE = new FriendNames();

   public void onStringString(String text, String text2) {
      DaamkyFriend.getInstance().onStringString3(text2, text);
   }

   public void onString(String text) {
      DaamkyFriend.getInstance().removeString(text);
   }

   public String getStringByString(String text) {
      return DaamkyFriend.getInstance().getStringByString(text);
   }

   public String getStringByString2(String text) {
      return DaamkyFriend.getInstance().getStringByString2(text);
   }

   public void onStringString2(String text, String text2) {
      DaamkyFriend.getInstance().onStringString5(text2, text);
   }

   public String getStringByString3(String text) {
      String s = DaamkyFriend.getInstance().getStringByString4(text);
      return s == null ? text : s;
   }

   public void onString2(String text) {
      DaamkyFriend.getInstance().addString2(text);
   }

   public static FriendNames getInstance() {
      return INSTANCE;
   }

   public List getList() {
      DaamkyFriend daamkyfriend = DaamkyFriend.getInstance();
      ArrayList arraylist = new ArrayList(daamkyfriend.getSet());
      arraylist.sort(Comparator.<String, String>comparing(var1x -> {
         String s = daamkyfriend.getStringByString2(var1x);
         return s == null ? "" : s;
      }).reversed().thenComparing(Comparator.naturalOrder()));
      return arraylist;
   }
}
