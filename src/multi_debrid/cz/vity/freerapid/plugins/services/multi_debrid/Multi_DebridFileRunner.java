package cz.vity.freerapid.plugins.services.multi_debrid;

import cz.vity.freerapid.plugins.exceptions.BadLoginException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Multi_DebridFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Multi_DebridFileRunner.class.getName());
    final static String MD_API = "http://multi-debrid.com/api.php";

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod getMethod = stepLogin(getMethodBuilder()
                .setAction(MD_API))
                .setParameter("link", fileURL)
                .toGetMethod();
        if (!makeRequest(getMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error making API request");
        }
        checkProblems();
        final String dlLink = PlugUtils.getStringBetween(getContentAsString(), "\"link\":\"", "\"").replace("\\", "");
        final HttpMethod httpMethod = getGetMethod(dlLink);
        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();//if downloading failed
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Cannot login Check your username or pass")) {
            throw new BadLoginException("Cannot login, Check your username or API password");
        }
        if (contentAsString.contains("\"status\":\"error\",\"link\":null")) {
            throw new NotRecoverableDownloadException("Link is invalid, expired, or not supported by Multi-Debrid");
        }
    }

    private MethodBuilder stepLogin(MethodBuilder builder) throws Exception {
        synchronized (Multi_DebridFileRunner.class) {
            final Multi_DebridServiceImpl service = (Multi_DebridServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Multi-Debrid account login information!");
                }
            }
            return builder.setParameter("user", pa.getUsername()).setParameter("pass", pa.getPassword());
        }
    }

}