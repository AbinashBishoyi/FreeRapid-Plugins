package cz.vity.freerapid.plugins.services.bitshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Class which contains main code
 *
 * @author Stan
 */
class BitShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BitShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1>Downloading ", " - ");
        PlugUtils.checkFileSize(httpFile, content, " - ", "</h1>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final String contentAsString;

        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        final String ajaxdl = PlugUtils.getStringBetween(contentAsString, "var ajaxdl = \"", "\";"); // download ID
        final String action = PlugUtils.getStringBetween(contentAsString, "http://bitshare.com", "request.html")
                + "request.html";

        final HttpMethod postMethodWithID = getMethodBuilder() // click to Download button
                .setAction(action).setParameter("request", "generateID").setParameter("ajaxid", ajaxdl)
                .setReferer(fileURL).toPostMethod();
        postMethodWithID.addRequestHeader("X-Requested-With", "XMLHttpRequest"); // send as AJAX

        String[] typeTimeCaptcha;
        if (client.getHTTPClient().executeMethod(postMethodWithID) == 200) {
            typeTimeCaptcha = processJsonAsString(postMethodWithID).split(":");  // JSON contains data in format "fileType:timeInSecond:captchaRequired"
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        downloadTask.sleep(Integer.parseInt(typeTimeCaptcha[1]));

        if (Integer.parseInt(typeTimeCaptcha[2]) == 1) {
            String captchaKey = PlugUtils.getStringBetween(contentAsString, "api.recaptcha.net/noscript?k=", "\"");
            HttpMethod captchaMethod;
            while (true) {
                captchaMethod = processCaptcha(captchaKey).modifyResponseMethod(getMethodBuilder(contentAsString)
                        .setAction(action).setParameter("request", "validateCaptcha").setParameter("ajaxid", ajaxdl)
                        .setReferer(fileURL)).toPostMethod();
                postMethodWithID.addRequestHeader("X-Requested-With", "XMLHttpRequest"); // send as AJAX

                if (client.getHTTPClient().executeMethod(captchaMethod) == 200) {
                    if (processJsonAsString(captchaMethod).equals("SUCCESS")) {
                        break;
                    }
                } else {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            }
        }

        final HttpMethod postMethodForUrl = getMethodBuilder()
              .setAction(action).setParameter("request", "getDownloadURL").setParameter("ajaxid", ajaxdl)
              .setReferer(fileURL).toPostMethod();
        postMethodWithID.addRequestHeader("X-Requested-With", "XMLHttpRequest"); // send as AJAX

        if (client.getHTTPClient().executeMethod(postMethodForUrl) == 200) {
            final HttpMethod getMethodForDownload = getMethodBuilder()
                  .setAction(processJsonAsString(postMethodForUrl).substring("SUCCESS#".length()))
                  .setReferer(fileURL).toGetMethod();

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(getMethodForDownload)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//se unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String processJsonAsString(HttpMethod method) throws PluginImplementationException {

        final StringBuilder content = new StringBuilder();
        InputStream is = null;
        try {
            is = new GZIPInputStream(method.getResponseBodyAsStream());
            int i;
            while ((i = is.read()) != -1) {
                content.append((char) i);
            }
        } catch (IOException e) {
            throw new PluginImplementationException("Error in JSON processing!");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "InputStream cannot closed.", e);
                }
            }
        }

        return content.toString();
    }

    private ReCaptcha processCaptcha(String key) throws Exception {

        final ReCaptcha reCaptcha = new ReCaptcha(key, client);
        final String captcha = getCaptchaSupport().getCaptcha(reCaptcha.getImageURL());
        if (captcha != null) {
            reCaptcha.setRecognized(captcha);
        }

        return reCaptcha;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not available")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}