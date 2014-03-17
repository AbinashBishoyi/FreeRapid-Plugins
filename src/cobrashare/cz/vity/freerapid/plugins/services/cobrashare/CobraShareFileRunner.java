package cz.vity.freerapid.plugins.services.cobrashare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Abinash Bishoyi
 */
class CobraShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CobraShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {
            stepRetarget();
            checkNameAndSize(getContentAsString());

        } else
            throw new ServiceConnectionProblemException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException, IOException {
        checkProblems();

        PlugUtils.checkName(httpFile, getContentAsString(), "<td class=\"popis\">N�zov s�boru :&nbsp;</td>\n<td class=\"data\">", "</td>");

        Matcher matcher = PlugUtils.matcher("Ve�kos� :&nbsp;<\\/td>\\s*<td class=\"data\">(.+?)<", content);
        if (matcher.find()) {
            final String stringSize = matcher.group(1);
            logger.info("String size:" + stringSize);
            final String removedEntities = PlugUtils.unescapeHtml(stringSize).replaceAll("(\\s|\u00A0)*", "");
            logger.info("Entities:" + removedEntities);
            final long size = PlugUtils.getFileSizeFromString(removedEntities);
            httpFile.setFileSize(size);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            checkProblems();
            logger.warning("File size was not found\n:");
            throw new PluginImplementationException();
        }

    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {
            stepRetarget();
            checkNameAndSize(getContentAsString());

        } else
            throw new ServiceConnectionProblemException();
        while (true) {

            checkProblems();
            HttpMethod finalMethod = stepCaptcha();
            if (!tryDownloadAndSaveFile(finalMethod)) {
                if (getContentAsString().contains("retarget()")) {
                    stepRetarget();
                    continue;
                }
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty.");
            } else break;
        }
    }

    private HttpMethod stepCaptcha() throws Exception {

        CaptchaSupport captchaSupport = getCaptchaSupport();
        String s = getMethodBuilder().setActionFromImgSrcWhereTagContains("ImageGen").getAction();
        logger.info("Captcha URL " + s);
        String captcha = captchaSupport.getCaptcha(s);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            final HttpMethod postMethod = getMethodBuilder().setActionFromFormByIndex(1, true).
                    setParameter("over", captcha).toPostMethod();
            return postMethod;

        }


    }

    private void stepRetarget() throws Exception {

        HttpMethod getMethod = getMethodBuilder().setActionFromTextBetween("url=", "\"").setReferer(fileURL).toHttpMethod();
        if (!makeRedirectedRequest(getMethod)) {
            throw new ServiceConnectionProblemException();
        }

    }

    private void checkProblems() throws ErrorDuringDownloadingException, IOException {
        String contentAsString = getContentAsString();
        if (contentAsString.contains("open(\"http://www.cobrashare.sk/cantDownload.php")) {
            final GetMethod getMethod = getGetMethod("http://www.cobrashare.sk/cantDownload.php");
            if (!makeRedirectedRequest(getMethod)) {
                throw new PluginImplementationException();
            } else contentAsString = getContentAsString();
        }

        if (contentAsString.contains("sa na serveri nenach")) {
            throw new URLNotAvailableAnymoreException("Požadovaný súbor sa na serveri nenachádza");
        }

        if (contentAsString.contains("prebieha prenos")) {
            throw new ServiceConnectionProblemException("Práve prebieha prenos (download) z vašej IP adresy");
        }

//        if (PlugUtils.find("te stiahnu. zadarmo", contentAsString)) {
//            throw new ServiceConnectionProblemException("Záťaž 100% - nemôžte stiahnuť zadarmo");
//        }

//        if (contentAsString.contains("Neplatny download")) {
//            throw new YouHaveToWaitException("Neplatny download", 2);
//        }
    }

}
