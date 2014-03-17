package cz.vity.freerapid.plugins.services.czshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, Jan Smejkal (edit from Hellshare to CZshare), tong2shot
 */
class CzshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CzshareRunner.class.getName());
    private final static int WAIT_TIME = 30;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        normalizeFileURL();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        final Matcher filenameMatcher = getMatcherAgainstContent("Celý název:.+?>(.+?)<");
        if (!filenameMatcher.find()) {
            throw new PluginImplementationException("Filename not found");
        }
        httpFile.setFileName(filenameMatcher.group(1));

        final Matcher filesizeMatcher = getMatcherAgainstContent("<div class=\"tab\" id=\"category\">\\s*Velikost:\\s*(.+?)\\s*</div>");
        if (!filesizeMatcher.find()) {
            throw new PluginImplementationException("Filesize not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(filesizeMatcher.group(1).replace("i", "")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        normalizeFileURL();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        if (getContentAsString().contains("Bohu.el je vy.erp.na maxim.ln. kapacita FREE download.")) {
            throw new YouHaveToWaitException("Na serveru jsou vyu�ity v�echny free download sloty", WAIT_TIME);
        }
        checkProblems();
        checkNameAndSize();

        MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromAHrefWhereATagContains("Stáhnout FREE");
        methodBuilder.setAction(PlugUtils.unescapeHtml(methodBuilder.getAction()));
        HttpMethod httpMethod = methodBuilder.toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();

        final String downloadPageURL = (httpMethod.getURI().toString());
        while (getContentAsString().contains("captchastring2")) {
            httpMethod = getMethodBuilder()
                    .setReferer(downloadPageURL)
                    .setActionFromFormWhereTagContains("freedown", true)
                    .setParameter("captchastring2", stepCaptcha(downloadPageURL))
                    .toPostMethod();
            final int httpStatus = client.makeRequest(httpMethod, false);
            if (httpStatus / 100 == 3) { //redirect to download file location
                final Header locationHeader = httpMethod.getResponseHeader("Location");
                if (locationHeader == null)
                    throw new ServiceConnectionProblemException("Could not find download file location");
                httpMethod = getMethodBuilder()
                        .setReferer(downloadPageURL)
                        .setAction(locationHeader.getValue())
                        .toGetMethod();
                break;
            }
            checkProblems();
        }
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        //final int waitTime = PlugUtils.getNumberBetween(getContentAsString(),"countdown_number =",";");
        //downloadTask.sleep(waitTime);
        httpMethod = getMethodBuilder()
                .setReferer(downloadPageURL)
                .setActionFromAHrefWhereATagContains("Stáhnout free omezenou rychlostí")
                .toGetMethod();

        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private String stepCaptcha(final String referer) throws Exception {
        CaptchaSupport captchaSupport = getCaptchaSupport();
        client.setReferer(referer);
        String captchaURL = "http://czshare.com/captcha.php";
        String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        return captcha;
    }


    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Soubor nenalezen")) {
            throw new URLNotAvailableAnymoreException("<b>Soubor nenalezen</b><br>");
        }
        if (contentAsString.contains("Soubor expiroval")) {
            throw new URLNotAvailableAnymoreException("<b>Soubor expiroval</b><br>");
        }
        if (contentAsString.contains("Soubor byl smaz.n jeho odesilatelem</strong>")||contentAsString.contains("Soubor byl smazán jeho odesilatelem")) {
            throw new URLNotAvailableAnymoreException("<b>Soubor byl smazán jeho odesilatelem</b><br>");
        }
        if (contentAsString.contains("Tento soubor byl na upozorn.n. identifikov.n jako warez.</strong>")) {
            throw new URLNotAvailableAnymoreException("<b>Tento soubor byl na upozorn�n� identifikov�n jako warez</b><br>");
        }
        if (contentAsString.contains("Bohu.el je vy.erp.na maxim.ln. kapacita FREE download.")) {
            throw new YouHaveToWaitException("Bohu�el je vy�erp�na maxim�ln� kapacita FREE download�", WAIT_TIME);
        }
        if (contentAsString.contains("Nesouhlas. kontroln. kod")) {
            throw new YouHaveToWaitException("�patn� k�d", 3);
        }
        if (contentAsString.contains("Z Vaší IP adresy momentálně probíhá jiné stahování")) {
            throw new YouHaveToWaitException("Your IP address is currently downloading another file", 10 * 60);
        }
    }

    private void normalizeFileURL() {
        fileURL = fileURL.replaceFirst("czshare\\.cz", "czshare.com");
    }
}