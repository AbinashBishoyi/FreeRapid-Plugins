package cz.vity.freerapid.plugins.services.debriditalia;

import cz.vity.freerapid.plugins.exceptions.BadLoginException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class DebridItaliaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DebridItaliaFileRunner.class.getName());
    final static String MD_API = "http://debriditalia.com/api.php";

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod getMethod = stepLogin(getMethodBuilder()
                .setAction(MD_API))
                .setParameter("generate", "")
                .setParameter("link", fileURL)
                .toGetMethod();
        if (!makeRequest(getMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error making API request");
        }
        checkProblems();
        final String dlLink = getContentAsString();
        httpFile.setFileName(dlLink.substring(dlLink.lastIndexOf("/") + 1));
        final HttpMethod httpMethod = getMethodBuilder()
                .setAction(dlLink).setReferer("http://debriditalia.com")
                .toGetMethod();
        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();//if downloading failed
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("ERROR: login_error")) {
            throw new BadLoginException("Cannot login, Check your username or password");
        }
        if (contentAsString.contains("ERROR: not_available")) {
            throw new NotRecoverableDownloadException("Link is invalid, expired, or not supported by DebridItalia");
        }
    }

    private MethodBuilder stepLogin(MethodBuilder builder) throws Exception {
        synchronized (DebridItaliaFileRunner.class) {
            final DebridItaliaServiceImpl service = (DebridItaliaServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No DebridItalia account login information!");
                }
            }
            return builder.setParameter("u", pa.getUsername()).setParameter("p", pa.getPassword());
        }
    }

}