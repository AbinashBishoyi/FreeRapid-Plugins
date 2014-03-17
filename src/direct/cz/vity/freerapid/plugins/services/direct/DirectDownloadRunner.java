package cz.vity.freerapid.plugins.services.direct;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.interfaces.FileStreamRecognizer;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * @author ntoskrnl
 * @author tong2shot
 */
class DirectDownloadRunner extends AbstractRunner implements FileStreamRecognizer {

    @Override
    public void run() throws Exception {
        super.run();
        checkName();
        final HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkName() throws ErrorDuringDownloadingException {
        String filename = findName(fileURL);
        logger.info("File name : " + filename);
        httpFile.setFileName(filename);
    }

    private static String findName(final String url) throws PluginImplementationException {
        String filename;
        try {
            String path = new URL(url).getPath();
            filename = URLDecoder.decode(path.substring(path.lastIndexOf("/") + 1), "UTF-8");
        } catch (Exception e) {
            throw new PluginImplementationException("Error getting file name");
        }
        return filename;
    }

    @Override
    public boolean isStream(HttpMethod method, boolean showWarnings) {
        return true;
    }

    @Override
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
        Header locationHeader;
        String action = method.getURI().toString();
        do {
            final HttpMethod method2 = getMethodBuilder().setReferer(fileURL).setAction(action).toGetMethod();
            processHttpMethod(method2);
            if (method2.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            locationHeader = method2.getResponseHeader("Location");
            if (locationHeader != null) {
                action = locationHeader.getValue();
            }
            method2.abort();
            method2.releaseConnection();
        } while (locationHeader != null);
        httpFile.setFileName(findName(action));
        setClientParameter(DownloadClientConsts.FILE_STREAM_RECOGNIZER, this);
        method = getMethodBuilder().setReferer(fileURL).setAction(action).toGetMethod();
        return super.tryDownloadAndSaveFile(method);
    }

    private void processHttpMethod(HttpMethod method) throws IOException {
        if (client.getHTTPClient().getHostConfiguration().getProtocol() != null) {
            client.getHTTPClient().getHostConfiguration().setHost(method.getURI().getHost(), 80, client.getHTTPClient().getHostConfiguration().getProtocol());
        }
        client.getHTTPClient().executeMethod(method);
    }

}
