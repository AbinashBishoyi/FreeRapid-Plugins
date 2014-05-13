package cz.vity.freerapid.plugins.services.titulky;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class TitulkyFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TitulkyFileRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
/*        //admin%2Bbob03@
        final HttpMethod loginMethod = getMethodBuilder().setAction("http://www.titulky.com/index.php").setParameter("Login", "Bob03").setParameter("Detail2", "").setParameter("Password", "0123456789").toPostMethod();
        if (!makeRedirectedRequest(loginMethod))  //we make the main request
            throw new PluginImplementationException();
        //index.php?welcome=
        final HttpMethod welcomeMethod = getMethodBuilder().setAction("http://www.titulky.com/index.php?welcome=").toGetMethod();
        if (!makeRedirectedRequest(welcomeMethod))  //we make the main request
            throw new PluginImplementationException();
*/
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            //final String number = PlugUtils.getStringBetween(getContentAsString(), "//OpenDownload('", "','");
            final String href = PlugUtils.getStringBetween(getContentAsString(), "href=\"", "\" id=\"opendown\"");
            client.getHTTPClient().getParams().setParameter("considerAsStream", "archive/zip");
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(href).toHttpMethod();
            if (makeRedirectedRequest(httpMethod)) {
                while (getContentAsString().contains("roboty, opsat")) {
                    String captcha = "";
                    while ("".equals(captcha)) {
                        captcha = getCaptchaSupport().getCaptcha("http://www.titulky.com/captcha/captcha.php");
                        if (captcha == null) {
                            throw new CaptchaEntryInputMismatchException("Captcha entry required");
                        }
                    }
                    final HttpMethod withCaptchaMethod = getMethodBuilder().setActionFromFormByName("downform", true).setBaseURL("http://www.titulky.com").setParameter("downkod", captcha).toPostMethod();
                    if (!makeRedirectedRequest(withCaptchaMethod)) {
                        throw new PluginImplementationException();//some unknown problem
                    }
                }
                parseDownloadPage();
            } else {
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void parseDownloadPage() throws Exception {
        downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "CountDown(", ")"));
        final String id = PlugUtils.getStringBetween(getContentAsString(), "href=\"/idown.php?id=", "\"");
        final HttpMethod httpMethod = getMethodBuilder().setAction("http://www.titulky.com/idown.php?id=" + id).toGetMethod();
        //here is the download link extraction
        client.getHTTPClient().getParams().setBooleanParameter("noContentLengthAvailable", true);
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        } else {
            extractZipFile();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void extractZipFile() throws IOException {
        final HttpFile downloadFile = downloadTask.getDownloadFile();
        final File file = new File(downloadFile.getSaveToDirectory(), downloadFile.getFileName());
        if (!file.exists())
            return;
        ZipInputStream zinstream = new ZipInputStream(new FileInputStream(file));
        ZipEntry zentry = zinstream.getNextEntry();
        logger.info("Name of current Zip Entry : " + zentry + "\n");
        byte[] buf = new byte[2048];
        String firstName = null;
        long firstSize = -1;
        while (zentry != null) {
            String entryName = zentry.getName();
            if (firstName == null) {
                firstName = zentry.getName();
                firstSize = zentry.getSize();
            }
            logger.info("Name of  Zip Entry : " + entryName);
            FileOutputStream outstream = new FileOutputStream(new File(downloadFile.getSaveToDirectory(), entryName));
            int n;
            while ((n = zinstream.read(buf, 0, 2048)) > -1) {
                outstream.write(buf, 0, n);
            }
            logger.info("Successfully Extracted File Name : " + entryName);
            outstream.close();
            zinstream.closeEntry();
            zentry = zinstream.getNextEntry();
        }
        zinstream.close();
        if (firstName != null) {
            file.delete();
            downloadFile.setFileName(firstName);
            downloadFile.setDownloaded(firstSize);
            downloadFile.setFileSize(firstSize);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        //unknown errors

    }

}