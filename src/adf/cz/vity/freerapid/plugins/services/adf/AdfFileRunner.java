package cz.vity.freerapid.plugins.services.adf;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class AdfFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AdfFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();

            String eu = PlugUtils.getStringBetween(getContentAsString(), "var eu = '", "';");
            int idx1 = eu.indexOf("!HiTommy");
            if (idx1 != -1) {
                eu = eu.substring(0, idx1);
            }
            String a = "", b = "";
            for (int i = 0; i < eu.length(); i++) {
                if (i % 2 == 0) {
                    a += eu.charAt(i);
                } else {
                    b = eu.charAt(i) + b;
                }
            }
            eu = a + b;
            eu = new String(Base64.decodeBase64(eu));
            eu = eu.substring(2);

            String url = eu;
            if (url.contains("adf.ly/go.php")) {
                if (!makeRedirectedRequest(getGetMethod(url))) {
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                url = PlugUtils.getStringBetween(getContentAsString(), " URL=", "\"");
            }
            httpFile.setNewURL(new URL(url));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("that link has been deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}
