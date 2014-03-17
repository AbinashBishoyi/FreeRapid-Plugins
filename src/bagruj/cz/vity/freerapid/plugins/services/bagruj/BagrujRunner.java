package cz.vity.freerapid.plugins.services.bagruj;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * @author JPEXS
 */
class BagrujRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BagrujRunner.class.getName());

    public BagrujRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        parseRedirection();
    }

    public void parseRedirection() throws Exception {
        GetMethod getMethod = getGetMethod(fileURL);
        int ret = client.makeRequest(getMethod, false);
        if (ret == 301) {
            String targetLink = getMethod.getResponseHeader("Location").getValue();
            logger.info("Redirection to:" + targetLink);
            getMethod.releaseConnection();
            if (targetLink.matches("http://(www\\.)?(uloz\\.to|ulozto\\.net|ulozto\\.cz|ulozto\\.sk)/.+")) {
                httpFile.setNewURL(new URL(targetLink));
                httpFile.setState(DownloadState.QUEUED);
                httpFile.setPluginID("");
            } else {
                throw new PluginImplementationException("Wrong redirection link");
            }
        } else {
            throw new PluginImplementationException("Redirection to uloz.to not found");
        }
    }

    public void run() throws Exception {
        super.run();
        parseRedirection();
    }

}