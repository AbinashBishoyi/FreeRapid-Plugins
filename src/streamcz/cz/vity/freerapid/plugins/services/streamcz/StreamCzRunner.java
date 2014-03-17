package cz.vity.freerapid.plugins.services.streamcz;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class StreamCzRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(StreamCzRunner.class.getName());

    public StreamCzRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkName(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    public void run() throws Exception {
        super.run();

        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRedirectedRequest(getMethod)) {
            if (getContentAsString().contains("stream.cz")) {
                checkName(getContentAsString());

                Matcher matcher = getMatcherAgainstContent("pFlvID=([^&]*)&");
                if (!matcher.find()) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("Id parameter not found.");
                }

                String flvId = matcher.group(1);
                String finalURL = "http://cdn-dispatcher.stream.cz/?id=" + flvId;
                GetMethod finalMethod = client.getGetMethod(finalURL);

                if (!tryDownloadAndSaveFile(finalMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }

            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
        } else
            throw new PluginImplementationException();
    }

    private String sicherName(String s) {
        Matcher matcher = PlugUtils.matcher("/[0-9]+-([^/]*)", s);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "file01.flv";
    }

    private void checkName(String content) throws Exception {

        if (!content.contains("stream.cz")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (getContentAsString().contains("404 nenalezeno")) {
            throw new URLNotAvailableAnymoreException("<b>404 nenalezeno.</b><br>");
        }

        Matcher matcher = PlugUtils.matcher("filename=([^&]*)&", content);
        // odebiram jmeno
        String fn;
        if (matcher.find()) {
            fn = matcher.group(1);
        } else fn = sicherName(fileURL);
        logger.info("File name " + fn);
        httpFile.setFileName(fn);
        // konec odebirani jmena


    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("404 nenalezeno")) {
            throw new URLNotAvailableAnymoreException("<b>404 nenalezeno.</b><br>");
        }
    }

}