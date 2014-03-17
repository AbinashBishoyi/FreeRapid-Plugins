package cz.vity.freerapid.plugins.services.egofiles_premium;

import cz.vity.freerapid.plugins.exceptions.BadLoginException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class EgoFilesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EgoFilesFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".egofiles.com", "lang", "en", "/", 86400, false));
        login();
        setFileStreamContentTypes("text/plain");
        final HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void login() throws Exception {
        synchronized (EgoFilesFileRunner.class) {
            EgoFilesServiceImpl service = (EgoFilesServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No EgoFiles Premium account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction("http://egofiles.com/ajax/register.php")
                    .setParameter("log", "1")
                    .setParameter("loginV", pa.getUsername())
                    .setParameter("passV", pa.getPassword())
                    .toPostMethod();
            method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("\"error\"")) {
                throw new BadLoginException("Invalid EgoFiles Premium account login information!");
            }
        }
    }

}