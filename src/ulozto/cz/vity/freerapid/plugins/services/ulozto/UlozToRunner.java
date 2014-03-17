package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class UlozToRunner {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.ulozto.UlozToRunner.class.getName());
    private HttpDownloadClient client;

    public void run(HttpFileDownloader downloader) throws Exception {
        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        String fileURL = httpFile.getFileUrl().toString();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            if (client.getContentAsString().contains("uloz.to")) {
                Matcher matcher = PlugUtils.matcher("\\|\\s*([^|]+) \\| </title>", client.getContentAsString());
                // odebiram jmeno
                String fn;
                if (matcher.find()) {
                    fn = matcher.group(1);
                } else fn = sicherName(fileURL);
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
               // konec odebirani jmena

                matcher = PlugUtils.matcher("<form action=\"([^\"]*)\" method=\"get\">", client.getContentAsString());
                if (!matcher.find()) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
                }
                final GetMethod method = client.getGetMethod(matcher.group(1));
                method.setFollowRedirects(true);
                client.getHTTPClient().getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
                httpFile.setState(DownloadState.GETTING);
                try {
                    final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile);
                    if (inputStream != null) {
                        downloader.saveToFile(inputStream);

                    } else {
                        checkProblems();
                        logger.info(client.getContentAsString());
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

    private String sicherName(String s) throws UnsupportedEncodingException {
        Matcher matcher = PlugUtils.matcher("(.*/)([^/]*)$", s);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "file01";
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = PlugUtils.matcher("soubor nebyl nalezen", client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Požadovaný soubor nebyl nalezen.</b><br>"));
        }
        matcher = PlugUtils.matcher("stahovat pouze jeden soubor", client.getContentAsString());
        if (matcher.find()) {
            throw new ServiceConnectionProblemException(String.format("<b>Mùžete stahovat pouze jeden soubor naráz</b><br>"));

        }


    }

}
