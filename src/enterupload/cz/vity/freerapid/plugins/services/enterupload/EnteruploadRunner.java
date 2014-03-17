package cz.vity.freerapid.plugins.services.enterupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */

class EnteruploadRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EnteruploadRunner.class.getName());
    private int captchaCounter;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        //http://www.enterupload.com/meybvc1ty6am/Website.Layout.Maker.Ultra.Edition.v2.4.rar.html
        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRequest(getMethod)) {
            checkNameandSize(getContentAsString());
        } else
            throw new ServiceConnectionProblemException();
    }

    private void checkNameandSize(String contentAsString) throws Exception {
//<META NAME="description" CONTENT="Download Website.Layout.Maker.Ultra.Edition.v2.4.rar">
        if (contentAsString.contains("File not found") || getContentAsString().contains("No such file")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));
        }

        if (!contentAsString.contains("Download File")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        //Matcher matcher = PlugUtils.matcher("Download ([^,]+), upload", contentAsString);
        PlugUtils.checkName(httpFile, getContentAsString(), "Download File", "</h2>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "> (", ")</font>");

    }

    @Override
    public void run() throws Exception {
        super.run();
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        logger.info("Starting download in TASK " + fileURL);

        if (makeRedirectedRequest(getMethod)) {
            String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);
            stepEnterPage();

            while (true) {           //(http://www\.enterupload\.com/captchas/[^\"]+)
                if (!getContentAsString().contains("captchas")) {
                    checkProblems();
                    throw new PluginImplementationException("Captcha not found");
                }

                HttpMethod getlinkMethod = stepCaptcha(getContentAsString());
                if (makeRequest(getlinkMethod)) {
                    final String value = PlugUtils.getStringBetween(getContentAsString(), "7px;\">", "</a>");
                    final HttpMethod finalMethod = getMethodBuilder(value + "</a>").setActionFromAHrefWhereATagContains("enterupload").toHttpMethod();
                    //downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "countdown\">", "</span"));
                    if (!tryDownloadAndSaveFile(finalMethod)) {
                        checkProblems();
                        if (getContentAsString().contains("Wrong captcha")) continue;
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty.");

                    } else break;
                } else
                    throw new ServiceConnectionProblemException();
            }


        } else throw new ServiceConnectionProblemException();
    }

    private HttpMethod stepCaptcha(String contentAsString) throws Exception {


        String s = getMethodBuilder(contentAsString).
                setActionFromImgSrcWhereTagContains("captchas").getAction();
        client.setReferer(fileURL);
        final String captcha;
        if (captchaCounter < 4) {
            ++captchaCounter;
            final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(s);
            captcha = new CaptchaRecognizer().recognize(captchaImage);
        } else {
            captcha = getCaptchaSupport().getCaptcha(s);
        }

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();

        }

        downloadTask.sleep(60);//extract sleep time from the website :-)

        final HttpMethod method = getMethodBuilder(contentAsString).
                setActionFromFormByName("F1", true).
                setParameter("code", captcha).setReferer(fileURL).
                setAction(fileURL).toPostMethod();
        setClientParameter("noContentTypeInHeader", true);

        return method;

        //} else throw new InvalidURLOrServiceProblemException("Cant find action - " + contentAsString);

    }

    private void stepEnterPage() throws Exception {
        if (getContentAsString().contains("Free Download")) {
            HttpMethod postMethod = getMethodBuilder().setActionFromFormByIndex(1, true).setAction(fileURL).removeParameter("method_premium").setParameter("method_free", "Free+Download").toPostMethod();
            if (!makeRequest(postMethod)) {
                throw new ServiceConnectionProblemException();
            }
        }

    }

    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException, YouHaveToWaitException, PluginImplementationException {
        if (getContentAsString().contains("File not found") || getContentAsString().contains("File Not Found") || getContentAsString().contains("No such file")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));

        }
        if (getContentAsString().contains("Skipped countdown")) {
            throw new PluginImplementationException("Skipped countdown");
        }
        if (getContentAsString().contains("reached the download-limit")) {
            int timeToWait = 0;
            Matcher minute = PlugUtils.matcher("([0-9]+)//s*minute", getContentAsString());
            Matcher second = PlugUtils.matcher("([0-9]+)//s*second", getContentAsString());
            if (minute.find()) {
                timeToWait = Integer.parseInt(minute.group(1));
            }
            if (second.find()) {
                timeToWait += Integer.parseInt(second.group(1));
            }
            if (timeToWait == 0) timeToWait = 5 * 60;
            throw new YouHaveToWaitException("You have reached the download-limit for free-users.", timeToWait + 1);
        }


    }


}