package cz.vity.freerapid.plugins.services.hellshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class HellshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HellshareRunner.class.getName());

    public void runCheck(HttpFileDownloader downloader) throws Exception {
        super.runCheck(downloader);
        final GetMethod getMethod = client.getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(client.getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            checkNameAndSize(client.getContentAsString());
            Matcher matcher = PlugUtils.matcher("([0-9.]+)%", client.getContentAsString());
            if (matcher.find()) {
                if (matcher.group(1).equals("100"))
                    throw new YouHaveToWaitException("Na serveru jsou využity všechny free download sloty", 30);
            }
            client.setReferer(fileURL);

            final PostMethod postmethod = client.getPostMethod(fileURL);
            postmethod.addParameter("free_download_iframe", "FREE DOWNLOAD");
            if (makeRequest(postmethod)) {
                PostMethod method = stepCaptcha();
                httpFile.setState(DownloadState.GETTING);
                if (!tryDownload(method)) {
                    boolean finish = false;
                    while (!finish) {
                        method = stepCaptcha();
                        finish = tryDownload(method);
                    }
                }
            } else {
                checkProblems();
                logger.info(client.getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }

        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkNameAndSize(String content) throws Exception {
        if (client.getContentAsString().contains("free_download_iframe")) {
            Matcher matcher = PlugUtils.matcher("<div class=\"download-filename\">([^<]*)</div>", content);
            if (matcher.find()) {
                String fn = matcher.group(matcher.groupCount());
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            }
            matcher = PlugUtils.matcher("<td>([0-9.]+ .B)</td>", content);
            if (matcher.find()) {
                Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + a);
                httpFile.setFileSize(a);
            }
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            checkProblems();
            logger.info(client.getContentAsString());
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }
    }

    private PostMethod stepCaptcha() throws Exception {
        if ("".equals(client.getContentAsString())) {
            throw new YouHaveToWaitException("Neurèité omezení", 120);
        }
        Matcher matcher;
        matcher = PlugUtils.matcher("<img id=\"captcha-img\" src=\"([^\"]*)\"", client.getContentAsString());
        if (!matcher.find()) {
            checkProblems();
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }
        String img = PlugUtils.replaceEntities(matcher.group(1));
        boolean emptyCaptcha;
        String captcha;
        do {
            logger.info("Captcha image " + img);
            captcha = getCaptchaSupport().getCaptcha(img);
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            if (captcha.equals("")) {
                emptyCaptcha = true;
                img = img + "1";
            } else emptyCaptcha = false;
        } while (emptyCaptcha);
        matcher = PlugUtils.matcher("form action=\"([^\"]*)\"", client.getContentAsString());
        if (!matcher.find()) {
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }

        String finalURL = matcher.group(1);
        String free_download_uri = PlugUtils.getParameter("free_download_uri", client.getContentAsString());

        final PostMethod method = client.getPostMethod(finalURL);

        method.addParameter("free_download_uri", free_download_uri);
        method.addParameter("captcha", captcha);
        method.addParameter("free_download_button", "St%E1hnout");
        return method;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = PlugUtils.matcher("Soubor nenalezen", client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Soubor nenalezen</b><br>"));
        }
        matcher = PlugUtils.matcher("Na serveru jsou .* free download", client.getContentAsString());
        if (matcher.find()) {
            throw new YouHaveToWaitException("Na serveru jsou využity všechny free download sloty", 30);
        }


    }
}