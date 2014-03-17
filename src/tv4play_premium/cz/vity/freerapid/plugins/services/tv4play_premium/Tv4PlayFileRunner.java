package cz.vity.freerapid.plugins.services.tv4play_premium;

import cz.vity.freerapid.plugins.exceptions.BadLoginException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class Tv4PlayFileRunner extends cz.vity.freerapid.plugins.services.tv4play.Tv4PlayFileRunner {
    private final static Logger logger = Logger.getLogger(Tv4PlayFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        login();
        super.run();
    }

    private void login() throws Exception {
        synchronized (Tv4PlayFileRunner.class) {
            Tv4PlayServiceImpl service = (Tv4PlayServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Tv4Play account login information!");
                }
            }
            HttpMethod method = getMethodBuilder()
                    .setAction("https://www.tv4play.se/session/new?redirect_url=%2F%3F")
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            method = getMethodBuilder()
                    .setActionFromFormByName("login_form", true)
                    .setParameter("user_name", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            if (getContentAsString().contains("Inloggningen misslyckades")) {
                throw new BadLoginException("Invalid Tv4Play account login information!");
            }
        }
    }

    @Override
    protected void checkProblems(final String content) throws ErrorDuringDownloadingException {
        if (content.contains("SESSION_NOT_AUTHENTICATED")) {
            throw new BadLoginException("Error logging in, account not premium?");
        }
        super.checkProblems(content);
    }

}