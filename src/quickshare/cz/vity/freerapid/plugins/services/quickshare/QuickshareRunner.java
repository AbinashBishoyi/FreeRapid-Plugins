package cz.vity.freerapid.plugins.services.quickshare;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class QuickshareRunner {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.quickshare.QuickshareRunner.class.getName());
    private HttpDownloadClient client;

    public void run(HttpFileDownloader downloader) throws Exception {
        HttpFile httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        String fileURL = httpFile.getFileUrl().toString();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            if (client.getContentAsString().contains("var server")) {
                Matcher matcher = PlugUtils.matcher("zev: <strong>([^<]*)</strong>", client.getContentAsString());
                if (matcher.find()) {
                    String fn = matcher.group(matcher.groupCount());
                    logger.info("File name " + fn);
                    httpFile.setFileName(fn);
                }
                matcher = PlugUtils.matcher("([0-9.]+)</strong>( .B)", client.getContentAsString());
                if (matcher.find()) {
                    Long a = PlugUtils.getFileSizeFromString(matcher.group(1) + matcher.group(2));
                    logger.info("File size " + a);
                    httpFile.setFileSize(a);
                }
                downloader.sleep(5);
                String server = getVar("server", client.getContentAsString());
                String id1 = getVar("ID1", client.getContentAsString());
                String id2 = getVar("ID2", client.getContentAsString());
                String id3 = getVar("ID3", client.getContentAsString());
                String id4 = getVar("ID4", client.getContentAsString());

                client.setReferer(fileURL);
                final String fn = server + "/download.php";
                logger.info("Found file URL " + fn);

                final PostMethod method = client.getPostMethod(fn);
                method.addParameter("ID1", id1);
                method.addParameter("ID2", id2);
                method.addParameter("ID3", id3);
                method.addParameter("ID4", id4);

                httpFile.setState(DownloadState.GETTING);
                try {
                    final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile);
                    if (inputStream != null) {
                        downloader.saveToFile(inputStream);

                    } else {
                        checkProblems();
                        logger.info(client.getContentAsString());
                        throw new ServiceConnectionProblemException("Soubor v tuto chvíli není k dispozici! Možná se z této IP již stahuje");
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

    private String getVar(String s, String contentAsString) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("var " + s + " = '([^']*)'", contentAsString);
        if (matcher.find()) {
            return matcher.group(1);
        } else
            throw new PluginImplementationException("Parameter " + s + " was not found");
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = PlugUtils.matcher("(c|C)hyba", client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Chyba! Soubor zøejmì neexistuje</b><br>"));
        }


    }
}