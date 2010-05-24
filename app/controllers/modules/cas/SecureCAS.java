/**
 *  This file is part of LogiSima-play-cas.
 *
 *  LogiSima-play-cas is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LogiSima-play-cas is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LogiSima-play-cas.  If not, see <http://www.gnu.org/licenses/>.
 */
package controllers.modules.cas;

import play.Logger;
import play.modules.cas.CASUtils;
import play.modules.cas.Security;
import play.modules.cas.annotation.Check;
import play.modules.cas.models.CASUser;
import play.mvc.Before;
import play.mvc.Controller;

/**
 * This class is a part of the play module secure-cas. It add the ability to
 * check if the user have access to the request. If the user is note logged, it
 * redirect the user to the CAS login page and authenticate it.
 * 
 * @author bsimard
 * 
 */
public class SecureCAS extends Controller {

    /**
     * Action for the login route. We simply redirect to CAS login page.
     * 
     * @throws Throwable
     */
    public static void login() throws Throwable {
        // we put into session the url we come from
        flash.put("url", request.method == "GET" ? request.url : "/");

        // we redirect the user to the cas login page
        String casLoginUrl = CASUtils.getCasLoginUrl(false);
        redirect(casLoginUrl);
    }

    /**
     * Action for the logout route. We simply redirect the user to CAS logout
     * page.
     * 
     * @throws Throwable
     */
    public static void logout() throws Throwable {
        // we clear session
        session.clear();

        // we invoke the implementation of "onDisconnected"
        Security.invoke("onDisconnected");

        // we redirect to the cas logout page.
        String casLogoutUrl = CASUtils.getCasLogoutUrl();
        redirect(casLogoutUrl);
    }

    /**
     * Action when the user authentification or checking rights fails.
     * 
     * @throws Throwable
     */
    public static void fail() throws Throwable {
        render();
    }
    
    /**
     * Action for the CAS return.
     * 
     * @throws Throwable
     */
    public static void authenticate() throws Throwable {
        Boolean isAuthenticated = Boolean.FALSE;
        String ticket = params.get("ticket");
        if (ticket != null) {
            Logger.debug("[SecureCAS]: Try to validate ticket " + ticket);
            CASUser user = CASUtils.valideCasTicket(request, ticket);
            if(user != null){
                isAuthenticated = Boolean.TRUE;
                session.put("username", user.getUsername());
                // we invoke the implementation of onAuthenticate 
                Security.invoke("onAuthenticated", user);
            }
        }
        
        if(isAuthenticated){
            // we redirect to the original URL
            String url = flash.get("url");
            if (url == null) {
                url = "/";
            }
            Logger.debug("[SecureCAS]: redirect to url -> " + url);
            redirect(url);
        }
        else{
            error();
        }
    }

    /**
     * Method that do CAS Filter and check rights.
     * 
     * @throws Throwable
     */
    @Before(unless = { "login", "logout", "fail", "authenticate" })
    static void filter() throws Throwable {
        Logger.debug("[SecureCAS]: CAS Filter for URL -> " + request.url);

        // if user is authenticated, the username is in session !
        if (session.contains("username")) {
            // We check the user's profile
            Check check = getActionAnnotation(Check.class);
            if (check != null) {
                check(check);
            }
        } else {
            Logger.debug("[SecureCAS]: user is not authenticated");
            // we put into session the url we come from
            flash.put("url", request.method == "GET" ? request.url : "/");
            flash.put("params", params);

            // we redirect the user to the cas login page
            String casLoginUrl = CASUtils.getCasLoginUrl(true);
            redirect(casLoginUrl);
        }
    }

    /**
     * Function to check the rights of the user. See your implementation of the
     * Security class with the method check.
     * 
     * @param check
     * @throws Throwable
     */
    private static void check(Check check) throws Throwable {
        for (String profile : check.value()) {
            boolean hasProfile = (Boolean) Security.invoke("check", profile);
            if (!hasProfile) {
                Security.invoke("onCheckFailed", profile);
            }
        }
    }

}