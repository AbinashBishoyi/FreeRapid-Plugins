package cz.vity.freerapid.plugins.services.xfilesharing;

import cz.vity.freerapid.plugins.exceptions.BadLoginException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.net.URL;

/**
 * @author tong2shot
 */
public abstract class RegisteredUserRunner extends XFileSharingRunner implements RegisteredUser {
    private final static Logger logger = Logger.getLogger(RegisteredUserRunner.class.getName());

    protected final String loginURL; //ex : "http://www.ryushare.com/login.python" or "http://www.ddlstorage.com/login.html"
    protected final String loginAction; // ex : "http://www.ryushare.com"
    protected final Class runnerClass; //ex : RyuShareFileRunner.class
    protected final Class implClass; // ex : RyuShareServiceImpl.class

    public RegisteredUserRunner(String serviceTitle, String loginURL, String loginAction, Class runnerClass, Class implClass) {
        super(serviceTitle);
        this.loginURL = loginURL;
        this.loginAction = loginAction;
        this.runnerClass = runnerClass;
        this.implClass = implClass;
        registeredUser = this;
    }

    @Override
    protected void checkPrerequisites() throws PluginImplementationException {
        super.checkPrerequisites();
        if (loginURL == null) throw new PluginImplementationException("loginURL cannot be null");
        if (loginAction == null) throw new PluginImplementationException("loginAction cannot be null");
        if (runnerClass == null) throw new PluginImplementationException("runnerClass cannot be null");
        if (implClass == null) throw new PluginImplementationException("implClass cannot be null");
    }

    protected String getIncorrectLoginContains() {
        return "Incorrect Login or Password";
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
            final String cookieDomain = "." + new URL(getBaseURL()).getHost();
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
            if (getContentAsString().contains(getIncorrectLoginContains()))
                throw new BadLoginException("Invalid " + serviceTitle + " login information!");
            return true;
        }
    }
}
