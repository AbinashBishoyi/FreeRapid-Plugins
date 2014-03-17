package cz.vity.freerapid.plugins.services.xfilesharingcommon;

import cz.vity.freerapid.plugins.exceptions.BadLoginException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.lang.reflect.Method;

/**
 * @author tong2shot
 */
public class RegisteredUserRunner extends XFileSharingCommonFileRunner implements RegisteredUser {
    protected final String loginURL; //ex : "http://www.ryushare.com/login.python" or "http://www.ddlstorage.com/login.html"
    protected final String loginAction; // ex : "http://www.ryushare.com"
    protected final Class runnerClass; //ex : RyuShareFileRunner.class
    protected final Class implClass; // ex : RyuShareServiceImpl.class

    public RegisteredUserRunner(String cookieDomain, String serviceTitle, String loginURL, String loginAction, Class runnerClass, Class implClass) {
        super();
        this.cookieDomain = cookieDomain;
        this.serviceTitle = serviceTitle;
        this.loginURL = loginURL;
        this.loginAction = loginAction;
        this.runnerClass = runnerClass;
        this.implClass = implClass;
        registeredUser = this;
    }

    @Override
    public boolean login() throws Exception {
        synchronized (runnerClass) {
            Method getConfig = implClass.getMethod("getConfig");
            PremiumAccount pa = (PremiumAccount) getConfig.invoke(getPluginService());

            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(loginURL)
                    .setAction(loginAction)
                    .setParameter("op", "login")
                    .setParameter("redirect", "")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("submit", "")
                    .toPostMethod();
            addCookie(new Cookie(cookieDomain, "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(cookieDomain, "xfss", "", "/", null, false));
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new BadLoginException("Invalid " + serviceTitle + " registered account login information!");
            return true;
        }
    }
}
