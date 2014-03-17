package cz.vity.freerapid.plugins.services.fsx_premium;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.HeadMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.httpclient.auth.BasicScheme.authenticate;

/**
 * Class which contains main code
 *
 * @author Hosszu
 */
class FsxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FsxFileRunner.class.getName());
    String authCookie;   // The authorization entry for the HEAD/GET methods

    @Override
    public void runCheck() throws Exception { //this method validates the file name and size
        super.runCheck();
        authCookie = login();
        final HeadMethod headMethod = new HeadMethod(fileURL);
        headMethod.addRequestHeader("Connection", "keep-alive");
        headMethod.removeRequestHeader("Keep-Alive");
        headMethod.addRequestHeader("Keep-Alive", "115");
        headMethod.addRequestHeader("Authorization", authCookie);
        if (makeRedirectedRequest(headMethod)) {
            checkNameAndSize(headMethod);  //ok let's extract file name and size from the method
        } else {
            throw new ServiceConnectionProblemException("The very first GET request failed.");
        }
    }

    // The file name and size will be checked and set into httpFile
    private void checkNameAndSize(HttpMethod headMethod) throws ErrorDuringDownloadingException {
        Header sizeH = headMethod.getResponseHeader("Content-length");
        final String path = headMethod.getPath();
        Pattern pat = Pattern.compile(".*/([^/]+)", Pattern.DOTALL);
        Matcher m = pat.matcher(path);
        String fileName;

        if (m.find()) {
            // Get the file name
            fileName = m.group(1);
        } else {
            throw new ServiceConnectionProblemException("The file name not found.");
        }
        // set the file name and size from the HEAD request
        httpFile.setFileName(fileName);
        httpFile.setFileSize(Long.parseLong(sizeH.getValue()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        logger.info("Checked, file: " + fileName + "   size: " + sizeH.getValue());
    }

    // No browser download is possible with the FSX premium account.
    // You can simply GET the file with Basic Authentication
    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        // Check and set file name and size
        runCheck();

        final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).toGetMethod();
        httpMethod.addRequestHeader("Connection", "keep-alive");
        httpMethod.removeRequestHeader("Keep-Alive");
        httpMethod.addRequestHeader("Keep-Alive", "115");
        httpMethod.addRequestHeader("Authorization", authCookie);

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(httpMethod)) {
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    // Check the existence and validity of the user name and password,
    // and return the HTTP authentication field value
    private String login() throws Exception {
        final String HTTP_SITE = "http://www.fsx.hu";
        synchronized (FsxFileRunner.class) {
            FsxServiceImpl service = (FsxServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No Fsx Premium account login information!");
                }
            }

            // Check the validity of the user name and password by a fake login
            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction(HTTP_SITE + "/index.php?m=home&o=login&logout=1")
                    .setParameter("u", pa.getUsername())
                    .setParameter("p", pa.getPassword())
                    .setParameter("x", "10")     // some position in the button image
                    .setParameter("y", "10")
                    .toPostMethod();

            httpMethod.addRequestHeader("Connection", "keep-alive");
            httpMethod.removeRequestHeader("Keep-Alive");
            httpMethod.addRequestHeader("Keep-Alive", "115");
            httpMethod.removeRequestHeader("Referer");
            httpMethod.addRequestHeader("Referer", HTTP_SITE + "/index.php?m=home&o=login&logout=1");

            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("window.alert(\"Hib")) {
                throw new NotRecoverableDownloadException("Invalid or missing Fsx Premium account login information!");
            }

            UsernamePasswordCredentials cred = new UsernamePasswordCredentials(pa.getUsername(), pa.getPassword());
            return authenticate(cred, "US-ASCII");
        }
    }
}