package cz.vity.freerapid.plugins.services.iskladka;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class IskladkaRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(IskladkaRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = checkURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {

            checkName(getContentAsString());
            GetMethod method = parseMethod(getContentAsString());
            downloadTask.sleep(getTimeToWait(getContentAsString()));
            httpFile.setState(DownloadState.GETTING);

            if (!tryDownloadAndSaveFile(method)) {
                boolean finish = false;
                int steps = 0;
                while (!finish && steps < 20 && getContentAsString().contains("ekejte!")) {
                    Matcher matcher = getMatcherAgainstContent("DL je ([0-9]+)");
                    if (matcher.find()) logger.info("Request wasnt final, length of queue is " + matcher.group(1));
                    GetMethod method2 = parseMethod(getContentAsString());
                    downloadTask.sleep(10 * getTimeToWait(getContentAsString()));
                    httpFile.setState(DownloadState.GETTING);
                    finish = tryDownloadAndSaveFile(method2);
                    steps++;
                }
                if (!finish) {
                    checkProblems();
                    throw new IOException("File input stream is empty.");
                }
            }
        } else
            throw new PluginImplementationException();
    }

    private String checkURL(String fileURL) {
        String newurl = fileURL.replaceFirst("iskladka.sk", "iskladka.cz");
        newurl = newurl.replaceFirst("download.php", "iCopy/index.php");
        return newurl;
    }

    private void checkName(String content) throws Exception {
        if (content.contains("bude zah")) {

            Matcher matcher = PlugUtils.matcher("\\?file=[0-9]+_(.*)$", fileURL);
            if (matcher.find()) {
                final String fn = URLDecoder.decode(matcher.group(1), "UTF-8");
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            }
        } else {
            checkProblems();
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

    }

    private GetMethod parseMethod(String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("var downloadCounterLink = '([^']*)';", content);
        if (!matcher.find()) {
            checkProblems();
            throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
        }
        String link = matcher.group(1);
        logger.info("Found downloadCounterLink: " + link);
        String ticket = "";
        matcher = PlugUtils.matcher("downloadTicket = '([^']*)'", content);
        if (matcher.find()) {
            ticket = matcher.group(1);
            logger.info("Found ticket: " + ticket);
        }
        if ("".equals(ticket)) {
            matcher = PlugUtils.matcher("ticket=([^']*)", content);
            if (matcher.find()) {
                ticket = matcher.group(1);
                logger.info("Found ticket: " + ticket);
            }
        }
        String target;
        matcher = PlugUtils.matcher("document.location.replace\\(\"([^?]*)\\?", content);
        if (matcher.find()) {
            target = matcher.group(1);
            logger.info("Found target URL: " + target);
        } else {
            logger.info("Not found target URL");
            logger.info(getContentAsString());
            throw new PluginImplementationException();
        }

        String finallink = target + "?file=" + link + "&ticket=" + ticket;
        final GetMethod method = getGetMethod(finallink);
        method.setFollowRedirects(true);
        return method;
    }

    private int getTimeToWait(String content) {
        int wait = 5;
        Matcher matcher = PlugUtils.matcher("downloadCounter = ([0-9]+)", content);
        if (matcher.find()) {
            String s = matcher.group(1);
            wait = new Integer(s);
            logger.info("Found number to wait: " + wait);
        }
        return wait;
    }


    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("Tento soubor ji. na .* nem.me");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Tento soubor již není k dispozici.</b><br>"));
        }
    }

}