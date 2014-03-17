package cz.vity.freerapid.plugins.services.iskladka;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class IskladkaRunner {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.iskladka.IskladkaRunner.class.getName());
    private HttpDownloadClient client;
    private HttpFileDownloader downloader;
    private HttpFile httpFile;

    public void run(HttpFileDownloader downloader) throws Exception {

        httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        this.downloader = downloader;
        String fileURL = httpFile.getFileUrl().toString();
        fileURL = checkURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            if (client.getContentAsString().contains("bude zah")) {
                Matcher matcher = PlugUtils.matcher("\\?file=[0-9]+_(.*)$", fileURL);
                if (matcher.find()) {
                    final String fn = URLDecoder.decode(matcher.group(1), "UTF-8");
                    logger.info("File name " + fn);
                    httpFile.setFileName(fn);
                }
                GetMethod method = parseMethod(client.getContentAsString());
                downloader.sleep(getTimeToWait(client.getContentAsString()));
                if (downloader.isTerminated())
                    throw new InterruptedException();
                httpFile.setState(DownloadState.GETTING);

                if (!trydownload(method)) {
                    boolean finish = false;
                    int steps = 0;
                    while (!finish && steps < 2 && client.getContentAsString().contains("ekejte!")) {
                        logger.info("Request wasnt final");
                        GetMethod method2 = parseMethod(client.getContentAsString());
                        downloader.sleep(10 * getTimeToWait(client.getContentAsString()));
                        if (downloader.isTerminated())
                            throw new InterruptedException();
                        httpFile.setState(DownloadState.GETTING);
                        finish = trydownload(method2);
                        steps++;
                    }
                    if (!finish) {
                        checkProblems();
                        throw new IOException("File input stream is empty.");
                    }
                }
            } else {
                checkProblems();
                logger.info(client.getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private String checkURL(String fileURL) {
        return fileURL.replaceFirst("iskladka.sk", "iskladka.cz");
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
        String target = "";
        matcher = PlugUtils.matcher("document.location.replace\\(\"([^?]*)\\?", content);
        if (matcher.find()) {
            target = matcher.group(1);
            logger.info("Found target URL: " + target);
        } else {
            logger.info("Not found target URL");
            logger.info(client.getContentAsString());
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }

        String finallink = target + "?file=" + link + "&ticket=" + ticket;
        final GetMethod method = client.getGetMethod(finallink);
        method.setFollowRedirects(true);
        return method;
    }

    private boolean trydownload(GetMethod method) throws Exception {
        try {
            final InputStream inputStream2 = client.makeFinalRequestForFile(method, httpFile);
            if (inputStream2 != null) {
                downloader.saveToFile(inputStream2);
                return true;
            } else {
                return false;
            }
        } finally {
            method.abort();
            method.releaseConnection();
        }
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
        matcher = PlugUtils.matcher("Tento soubor ji. na .* nem.me", client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Tento soubor již není k dispozici.</b><br>"));
        }
    }

}