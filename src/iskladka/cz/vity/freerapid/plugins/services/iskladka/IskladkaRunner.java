package cz.vity.freerapid.plugins.services.iskladka;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.HttpFile;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class IskladkaRunner {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.iskladka.IskladkaRunner.class.getName());
    private HttpDownloadClient client;

    public void run(HttpFileDownloader downloader) throws Exception {

        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        String fileURL = httpFile.getFileUrl().toString();
        fileURL = CheckURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {


            Matcher matcher = Pattern.compile("var downloadCounterLink = '([^']*)';", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (!matcher.find()) {
                checkProblems();
                throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
            }
            String link = matcher.group(1);
            logger.info(" downloadCounterLink: " + link);


            matcher = Pattern.compile("\\?file=[0-9]+_(.*)$", Pattern.MULTILINE).matcher(fileURL);
            if (matcher.find()) {
                final String fn = URLDecoder.decode(matcher.group(1), "UTF-8");
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            }

            matcher = Pattern.compile("ticket=([^']*)", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                String ticket = matcher.group(1);
                logger.info("Ticket: " + ticket);
                int wait = 5;
                matcher = Pattern.compile("downloadCounter = ([0-9]+)", Pattern.MULTILINE).matcher(client.getContentAsString());
                if (matcher.find()) {
                    String s = matcher.group(1);
                    wait = new Integer(s);
                    logger.info("Wait: " + wait);
                }
                downloader.sleep(wait);

                if (downloader.isTerminated())
                    throw new InterruptedException();
                httpFile.setState(DownloadState.GETTING);
                String finallink = "http://www.iskladka.cz/downloadBalancer.php?file=" + link + "&ticket=" + ticket;
                final GetMethod method = client.getGetMethod(finallink);
                method.setFollowRedirects(true);
                try {
                    final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile);
                    if (inputStream != null) {
                        downloader.saveToFile(inputStream);
                    } else {
                        if (client.getContentAsString().contains("Èekejte!")) {

                            matcher = Pattern.compile("var downloadCounterLink = '([^']*)';", Pattern.MULTILINE).matcher(client.getContentAsString());
                            if (!matcher.find()) {
                                checkProblems();
                                throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
                            }
                            String link2 = matcher.group(1);
                            logger.info(" downloadCounterLink: " + link);

                            matcher = Pattern.compile("downloadTicket = '([^']*)'", Pattern.MULTILINE).matcher(client.getContentAsString());
                            if (matcher.find()) {
                                String ticket2 = matcher.group(1);
                                logger.info("Ticket: " + ticket);

                                wait = 5;
                                matcher = Pattern.compile("downloadCounter = ([0-9]+)", Pattern.MULTILINE).matcher(client.getContentAsString());
                                if (matcher.find()) {
                                    String s = matcher.group(1);
                                    wait = new Integer(s);
                                    logger.info("Wait: " + 10 * wait);


                                }
                                downloader.sleep(wait * 10);

                                if (downloader.isTerminated())
                                    throw new InterruptedException();
                                httpFile.setState(DownloadState.GETTING);
                                String finallink2 = "http://www.iskladka.cz/downloadSimple.php?file=" + link2 + "&ticket=" + ticket2;
                                final GetMethod method2 = client.getGetMethod(finallink2);
                                method2.setFollowRedirects(true);
                                try {
                                    final InputStream inputStream2 = client.makeFinalRequestForFile(method2, httpFile);
                                    if (inputStream2 != null) {
                                        downloader.saveToFile(inputStream2);
                                    } else {

                                        checkProblems();
                                        logger.warning(client.getContentAsString());
                                        throw new IOException("File input stream is empty.");
                                    }


                                } finally {
                                    method2.abort();
                                    method2.releaseConnection();
                                }
                            }
                        } else {
                            checkProblems();
                            throw new IOException("File input stream is empty.");
                        }
                    }
                } finally {
                    method.abort();
                    method.releaseConnection();
                }
            } else {
                checkProblems();
                logger.info(client.getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private String CheckURL(String fileURL) {
        return fileURL.replaceFirst("iskladka.sk", "iskladka.cz");
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = Pattern.compile("Your IP is already downloading", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new ServiceConnectionProblemException(String.format("<b>Your IP is already downloading a file from our system.</b><br>You cannot download more than one file in parallel."));
        }
        matcher = Pattern.compile("Please try in\\s*([0-9]+) minute", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new YouHaveToWaitException("You used up your limit for file downloading!", Integer.parseInt(matcher.group(1)) * 60 + 20);
        }

        matcher = Pattern.compile("slots[^<]*busy", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new ServiceConnectionProblemException(String.format("<b>All downloading slots for your country are busy</b><br>"));

        }
        matcher = Pattern.compile("Tento soubor již na .* nemáme", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Tento soubor již není k dispozici.</b><br>"));

        }

    }


}