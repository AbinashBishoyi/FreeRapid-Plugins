package cz.vity.freerapid.plugins.services.mediafire;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class MediafireRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MediafireRunner.class.getName());

    public MediafireRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        int result = client.makeRequest(getMethod, true);
        if (result == HttpStatus.SC_OK || result == HttpStatus.SC_NOT_FOUND) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    public void run() throws Exception {
        super.run();

        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        int result = client.makeRequest(getMethod, true);
        if (result == HttpStatus.SC_OK || result == HttpStatus.SC_NOT_FOUND) {
            checkNameAndSize(getContentAsString());
            if (getContentAsString().contains("cu(")) {
                Matcher matcher = getMatcherAgainstContent("cu\\('([^']+)','([^']+)','([^']+)'\\)");

                if (!matcher.find()) {
                    throw new PluginImplementationException();
                }
                String qk = matcher.group(1);
                String pk = matcher.group(2);
                String r = matcher.group(3);
                String url = "http://www.mediafire.com/dynamic/download.php?qk=" + qk + "&pk=" + pk + "&r=" + r;
                logger.info("Sript target URL " + url);
                GetMethod method = getGetMethod(url);


                if (makeRequest(method)) {
                    matcher = getMatcherAgainstContent("href=.\"http://\"([^\"]*)\"");

                    if (!matcher.find()) {
                        throw new PluginImplementationException();
                    }
                    String finalLink = "http://" + parseLink(matcher.group(1));
                    logger.info("Final URL " + finalLink);

                    GetMethod method2 = getGetMethod(finalLink);
                    client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain");
                    if (!tryDownloadAndSaveFile(method2)) {
                        checkProblems();
                        logger.info(getContentAsString());
                        throw new PluginImplementationException();
                    }


                } else {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }


            }
        } else
            throw new PluginImplementationException();
    }


    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("mediafire.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("The key you provided for file download was invalid") || content.contains("How can MediaFire help you?")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>The file was removed.</b><br>"));
        }

        Matcher matcher = PlugUtils.matcher("You requested: ([^ ]+) \\(([0-9.]+ .B)\\)", content);
        // odebiram jmeno
        String fn;
        if (matcher.find()) {
            fn = matcher.group(1);
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
            Long a = PlugUtils.getFileSizeFromString(matcher.group(2));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

        }

        // konec odebirani jmena

    }

    String parseLink(String rawlink) throws Exception {

        String link = "";

        Matcher matcher = PlugUtils.matcher("([^']*)'([^']*)'", rawlink);
        while (matcher.find()) {

            Matcher matcher1 = PlugUtils.matcher("\\+\\s*(\\w+)", matcher.group(1));
            while (matcher1.find()) {

                link = link + (getVar(matcher1.group(1)));
            }
            link = link + matcher.group(2);

        }
        matcher = PlugUtils.matcher("([^']*)'$", rawlink);
        if (matcher.find()) {

            Matcher matcher1 = PlugUtils.matcher("\\+\\s*(\\w+)", matcher.group(1));
            if (matcher1.find()) {

                link = link + (getVar(matcher1.group(1)));
            }


        }

        return link;
    }

    private String getVar(String s) throws PluginImplementationException {

        Matcher matcher = PlugUtils.matcher("var " + s + "\\s*=\\s*'([^']*)'", getContentAsString());
        if (matcher.find()) {
            return matcher.group(1);
        } else
            throw new PluginImplementationException("Parameter " + s + " was not found");
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("The key you provided for file download was invalid");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>The file was removed</b><br>"));
        }

    }

}
