package cz.vity.freerapid.plugins.services.hellshare_full;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class HellshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HellshareRunner.class.getName());
    private boolean badConfig = false;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {

            checkNameAndSize(getContentAsString());

            if (getContentAsString().contains("frm-loginForm")) {
                Login(getContentAsString());
            }
            Matcher matcher = getMatcherAgainstContent("<a id=\"button-download-full-nocredit\"");
            if (matcher.find()) {
                 throw new NotRecoverableDownloadException("No credit for download!");
            }

            matcher = getMatcherAgainstContent(" href=\"([^\"]+)\" target=\"full-download-iframe\">[^<]+</a>");
            if (matcher.find()) {
                String downURL = matcher.group(1);
                final GetMethod getmethod = getGetMethod(downURL);
                httpFile.setState(DownloadState.GETTING);
                if (!tryDownloadAndSaveFile(getmethod)) {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }

        } else
            throw new ServiceConnectionProblemException();
    }

    private void checkNameAndSize(String content) throws Exception {
        PlugUtils.checkName(httpFile, content, "<strong id=\"FileName_master\">", "</strong>");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "<strong id=\"FileSize_master\">", "</strong>").replace("&nbsp;", " ")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void Login(String content) throws Exception {
        synchronized (HellshareRunner.class) {
            HellshareServiceImpl service = (HellshareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();

            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No HellShare full account login information!");
                }
                badConfig = false;
            }
            PostMethod  postmethod=(PostMethod) getMethodBuilder(getContentAsString()).setActionFromFormByName("frm-loginForm", true)
                    .setParameter("username", pa.getUsername()).setParameter("password", pa.getPassword()).toPostMethod();

            if (makeRedirectedRequest(postmethod)) {
                if (getContentAsString().contains("frm-loginForm")) {
                    badConfig = true;
                    throw new NotRecoverableDownloadException("Bad HellShare full account login information!");
                }
                GetMethod getMethod = getGetMethod(fileURL);
                if (!makeRedirectedRequest(getMethod))
                    throw new PluginImplementationException();
            } else {
                throw new PluginImplementationException("Bad login URL");
            }
        }
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, NotRecoverableDownloadException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("Soubor nenalezen|S.bor nen.jden.|A f.jl nem volt megtal.lhat.");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Soubor nenalezen</b><br>"));
        }
        matcher = getMatcherAgainstContent("Na serveru jsou .* free download|Na serveri s. vyu.it. v.etky free download sloty|A szerveren az .sszes free download slot ki van haszn.lva");
        if (matcher.find()) {
            throw new YouHaveToWaitException("Na serveru jsou využity všechny free download sloty", 30);
        }
        if (badConfig || getContentAsString().equals("")) {
            throw new NotRecoverableDownloadException("Bad HellShare full account login information!");
        }
    }
}