package cz.vity.freerapid.plugins.services.egoshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class EgoshareRunner {
    private final static Logger logger = Logger.getLogger(EgoshareRunner.class.getName());
    private HttpDownloadClient client;
    private HttpFileDownloader downloader;
    private static final String HTTP_EGOSHARE = "http://www.egoshare.com";

    private String initURL;
    private String enterURL;

    public void run(HttpFileDownloader downloader) throws Exception {
        this.downloader = downloader;
        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        final String fileURL = httpFile.getFileUrl().toString();
        initURL = fileURL;
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {

            Matcher matcher = Pattern.compile("\\(([0-9.]* .B)\\)", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + a);
                 httpFile.setFileSize(a);
            }
            matcher = Pattern.compile("File name: </b></td>\\s*<td align=left><b> ([^<]*)<", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                final String fn = matcher.group(1);
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            } else logger.warning("File name was not found" + client.getContentAsString());

            do {
                checkProblems();
                if (!client.getContentAsString().contains("Please enter the number")) {
                    logger.info(client.getContentAsString());
                    throw new PluginImplementationException("No captcha.\nCannot find requested page content");
                }
                stepCaptcha(client.getContentAsString());

            } while (client.getContentAsString().contains("Please enter the number"));

            matcher = Pattern.compile("decode\\(\"([^\"]*)\"", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                String s = decode(matcher.group(1));
                logger.info("Found File URL - " + s);
                if (downloader.isTerminated())
                    throw new InterruptedException();
                httpFile.setState(DownloadState.GETTING);
                final GetMethod method = client.getGetMethod(s);
                client.getHTTPClient().getParams().setHttpElementCharset("ISO-8859-1");
                try {
                    final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile);
                    if (inputStream != null) {
                        downloader.saveToFile(inputStream);
                    } else {
                        checkProblems();
                        throw new IOException("File input stream is empty.");
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

    private String replaceEntities(String s) {
        s = s.replaceAll("\\&amp;", "&");
        return s;
    }

    private String decode(String input) {

        final StringBuilder output = new StringBuilder();
        int chr1;
        int chr2;
        int chr3;
        int enc1;
        int enc2;
        int enc3;
        int enc4;
        int i = 0;
        final String _keyStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

        input = input.replaceAll("[^A-Za-z0-9+/=]", "");

        while (i < input.length()) {

            enc1 = _keyStr.indexOf(input.charAt(i++));
            enc2 = _keyStr.indexOf(input.charAt(i++));
            enc3 = _keyStr.indexOf(input.charAt(i++));
            enc4 = _keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output.append((char) chr1);

            if (enc3 != 64) {
                output.append((char) chr2);
            }
            if (enc4 != 64) {
                output.append((char) chr3);
            }
        }


        try {
            return (new String(output.toString().getBytes(), "UTF-8"));

        } catch (UnsupportedEncodingException ex) {
            logger.warning("Unsupported encoding" + ex);
        }
        return "";

    }


    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("Please enter the number")) {

            Matcher matcher = Pattern.compile("captcha", Pattern.MULTILINE).matcher(contentAsString);
            if (matcher.find()) {
                String s = "http://www.egoshare.com/captcha.php";
                String captcha = downloader.getCaptcha(s);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    String ndpage = getParameter("2ndpage", contentAsString);
                    matcher = Pattern.compile("name=myform action\\=\"([^\"]*)\"", Pattern.MULTILINE).matcher(contentAsString);
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Captcha form action was not found");
                    }
                    s = matcher.group(1);
                    client.setReferer(initURL);
                    final PostMethod postMethod = client.getPostMethod(s);

                    postMethod.addParameter("captchacode", captcha);
                    postMethod.addParameter("2ndpage", ndpage);
                    client.getHTTPClient().getParams().setParameter("http.protocol.single-cookie-header", true);
                     if (client.makeRequest(postMethod) == HttpStatus.SC_OK) {
                          return true;
                    }
                }
            } else {
                logger.warning(contentAsString);
                throw new PluginImplementationException("Captcha picture was not found");
            }

        }
        return false;
    }

    private String getParameter(String s, String contentAsString) throws PluginImplementationException {
        Matcher matcher = Pattern.compile("name=\"" + s + "\"[^v>]*value=\"([^\"]*)\"", Pattern.MULTILINE).matcher(contentAsString);
        if (matcher.find()) {
            return matcher.group(1);
        } else
            throw new PluginImplementationException("Parameter " + s + " was not found");
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = Pattern.compile("You have got max allowed download sessions from the same IP", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {

            throw new YouHaveToWaitException("You have got max allowed download sessions from the same IP!", 5 * 60);
        }
        matcher = Pattern.compile("Your requested file could not be found", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("Your requested file could not be found");
        }

    }

}