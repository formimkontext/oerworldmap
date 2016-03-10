package helpers;

import java.util.Base64;

import controllers.Global;

public class AuthTest {
  public static String getAuthString() {
    String email = Global.getConfig().getString("admin.user");
    String pass = Global.getConfig().getString("admin.pass");
    String authString = email.concat(":").concat(pass);
    return Base64.getEncoder().encodeToString(authString.getBytes());
  }
}
