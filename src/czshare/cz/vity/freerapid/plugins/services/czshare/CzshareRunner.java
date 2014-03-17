package cz.vity.freerapid.plugins.services.czshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, Jan Smejkal (edit from Hellshare to CZshare)
 */
class CzshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CzshareRunner.class.getName());
    private final static Map<String, PostMethod> methodsMap = new HashMap<String, PostMethod>();
    private final static int WAIT_TIME = 30;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            checkCaptcha();
        } else {
            checkProblems();
            makeRedirectedRequest(getMethod);
            checkProblems();
            throw new PluginImplementationException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        if (checkInQueue())
            return;

        final PostMethod postmethod = parseFirstPage();
        if (makeRequest(postmethod)) {
            PostMethod method = stepCaptcha();
            httpFile.setState(DownloadState.GETTING);
            if (!tryDownloadAndSaveFile(method)) {
                boolean finish = false;
                while (!finish) {
                    method = stepCaptcha();
                    finish = tryDownloadAndSaveFile(method);
                }
            }
        } else {
            checkProblems();
            logger.info(getContentAsString());
            throw new PluginImplementationException();
        }
    }

    private void checkCaptcha() throws Exception {
        final PostMethod postmethod = parseFirstPage();
        if (makeRequest(postmethod)) {
            stepCaptcha();
        } else {
            checkProblems();
            logger.info(getContentAsString());
            throw new PluginImplementationException();
        }
    }

    private boolean checkInQueue() throws Exception {
        if (!methodsMap.containsKey(fileURL))
            return false;

        PostMethod method = methodsMap.get(fileURL);
        methodsMap.remove(fileURL);

        httpFile.setState(DownloadState.GETTING);
        return tryDownloadAndSaveFile(method);
    }

    private PostMethod parseFirstPage() throws Exception {
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            String content = getContentAsString();
            checkNameAndSize(content);

            Matcher matcher = getMatcherAgainstContent("Bohu.el je vy.erp.na maxim.ln. kapacita FREE download.");
            if (matcher.find()) {
                throw new YouHaveToWaitException("Na serveru jsou využity všechny free download sloty", WAIT_TIME);
            }
            client.setReferer(fileURL);


            matcher = PlugUtils.matcher("<div class=\"free-download\">[ ]*\n[ ]*<form action=\"([^\"]*)\" method=\"post\">", content);
            if (!matcher.find()) {
                throw new PluginImplementationException();
            }
            String postURL = matcher.group(1);

            final PostMethod method = getPostMethod(postURL);

            PlugUtils.addParameters(method, getContentAsString(), new String[]{"id", "file", "ticket"});

            return method;

        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws Exception {
        if (getContentAsString().contains("zev souboru:")) {
            Matcher matcher = PlugUtils.matcher("<span class=\"text-darkred\"><strong>([^<]*)</strong></span>", content);
            if (matcher.find()) {
                String fn = matcher.group(1);
                httpFile.setFileName(fn);
            }
            matcher = PlugUtils.matcher("<td class=\"text-left\">([0-9.]+ .B)</td>", content);
            if (matcher.find()) {
                long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                httpFile.setFileSize(a);
            }
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            checkProblems();
            logger.info(getContentAsString());
            throw new PluginImplementationException();
        }
    }

    private PostMethod stepCaptcha() throws Exception {
        if ("".equals(getContentAsString())) {
            throw new YouHaveToWaitException("Neurčité omezení", 4 * WAIT_TIME);
        }
        Matcher matcher;
        matcher = getMatcherAgainstContent("<td class=\"kod\" colspan=\"2\"><img src=\"([^\"]*)\" /></td>");
        if (!matcher.find()) {
            checkProblems();
            throw new PluginImplementationException();
        }

        String img = "http://czshare.com/" + PlugUtils.replaceEntities(matcher.group(1));
        boolean emptyCaptcha;
        String captcha;
        do {
            logger.info("Captcha image " + img);
            captcha = getCaptchaSupport().getCaptcha(img);
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            if ("".equals(captcha)) {
                emptyCaptcha = true;
                img = img + "1";
            } else emptyCaptcha = false;
        } while (emptyCaptcha);

        matcher = getMatcherAgainstContent("<form action=\"([^\"]*)\" method=\"get\">");
        if (!matcher.find()) {
            throw new PluginImplementationException();
        }
        String finalURL = matcher.group(1);

        final String content = getContentAsString();
        String finalID = PlugUtils.getParameter("id", content);

        String finalFile = PlugUtils.getParameter("file", content);

        String finalTicket = PlugUtils.getParameter("ticket", content);

        finalURL = "http://czshare.com/" + finalURL + "?id=" + finalID + "&file=" + finalFile + "&ticket=" + finalTicket + "&captchastring=" + captcha;

        final GetMethod method = getGetMethod(finalURL);

        if (makeRequest(method)) {
            return stepDownload();
        } else {
            checkProblems();
            logger.info(getContentAsString());
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }
    }

    private PostMethod stepDownload() throws Exception {

        Matcher matcher = getMatcherAgainstContent("<form name=\"pre_download_form\" action=\"([^\"]*)\" method=\"post\"");
        if (!matcher.find()) {
            throw new PluginImplementationException();
        }
        String finalURL = matcher.group(1);

        PostMethod method = getPostMethod(finalURL);
        PlugUtils.addParameters(method, getContentAsString(), new String[]{"id", "ticket", "submit_btn"});

        methodsMap.put(fileURL, method);

        return method;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("Soubor nenalezen");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Soubor nenalezen</b><br>");
        }
        matcher = getMatcherAgainstContent("Soubor byl smaz.n jeho odesilatelem</strong>");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Soubor byl smazán jeho odesilatelem</b><br>");
        }
        matcher = getMatcherAgainstContent("Bohu.el je vy.erp.na maxim.ln. kapacita FREE download.");
        if (matcher.find()) {
            throw new YouHaveToWaitException("Bohužel je vyčerpána maximální kapacita FREE downloadů", WAIT_TIME);
        }
        matcher = getMatcherAgainstContent("Nesouhlas. kontroln. kod");
        if (matcher.find()) {
            throw new YouHaveToWaitException("Špatný kód", 3);
        }
    }
}