package cz.vity.freerapid.plugins.services.zbigz;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u3
 */
class ZBigZFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ZBigZFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        login();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
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
        setClientParameter(DownloadClientConsts.IGNORE_ACCEPT_RANGES, true);
        httpFile.setResumeSupported(true);
        method = getMethodBuilder().setReferer(fileURL).setAction(action).toGetMethod();
        return super.tryDownloadAndSaveFile(method);
    }

    private void processHttpMethod(HttpMethod method) throws IOException {
        if (client.getHTTPClient().getHostConfiguration().getProtocol() != null) {
            client.getHTTPClient().getHostConfiguration().setHost(method.getURI().getHost(), 80, client.getHTTPClient().getHostConfiguration().getProtocol());
        }
        client.getHTTPClient().executeMethod(method);
    }

    private void login() throws Exception {
        synchronized (ZBigZFileRunner.class) {
            ZBigZServiceImpl service = (ZBigZServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No ZBigZ account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction("http://m.zbigz.com/login.php")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("Bad login or password")) {
                throw new BadLoginException("Invalid account login information");
            }
        }
    }

}
