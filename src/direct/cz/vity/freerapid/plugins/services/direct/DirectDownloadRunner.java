package cz.vity.freerapid.plugins.services.direct;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.interfaces.FileStreamRecognizer;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.io.IOException;

/**
 * @author ntoskrnl
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

    private void checkName() {
        httpFile.setFileName(findName(fileURL));
    }

    private static String findName(final String url) {
        final String[] strings = url.split("/");
        for (int i = strings.length - 1; i >= 0; i--) {
            final String s = strings[i].trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        String s = url.replaceAll(":", "_").trim();
        if (s.startsWith("?"))
            s = s.substring(1);
        if (s.isEmpty()) {
            s = "unknown";
        }
        return s;
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
