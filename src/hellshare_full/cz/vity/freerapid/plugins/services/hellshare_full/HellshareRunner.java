package cz.vity.freerapid.plugins.services.hellshare_full;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
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
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException("1");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());

            client.setReferer(fileURL);

            Matcher matcher = getMatcherAgainstContent("<p>U.ivatel nep.ihl..en.</p>");
            if (matcher.find()) {
                Login(getContentAsString());
            }

            matcher = getMatcherAgainstContent("Bohu.el nem.te dostatek kredit. na sta.en. souboru");
            if (matcher.find()) {
                throw new NotRecoverableDownloadException("No credit for download!");
            }

            matcher = getMatcherAgainstContent(", [0-9]+[)];\" href=\"([^\"]+)\" target=\"full-download-iframe\">Download</a> .</span></p>");
            if(matcher.find())
            {
                String downURL = matcher.group(1);
                final GetMethod getmethod = getGetMethod(downURL);
                httpFile.setState(DownloadState.GETTING);
                if (!tryDownloadAndSaveFile(getmethod)) {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException("2");
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException("3");
            }

        } else
            throw new PluginImplementationException("4");
    }

    private void checkNameAndSize(String content) throws Exception {
        if (getContentAsString().contains("FreeDownProgress")) {
            Matcher matcher = PlugUtils.matcher("<tr><th scope=\"row\" class=\"download-properties-label\">N.zev:</th><td><h2>([^<]+)</h2></td></tr>", content);
            if (matcher.find()) {
                String fn = matcher.group(matcher.groupCount());
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            }
            matcher = PlugUtils.matcher("<tr><th scope=\"row\" class=\"download-properties-label\">Velikost:</th><td>([^<]+ .B)</td></tr>", content);
            if (matcher.find()) {
                Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + a);
                httpFile.setFileSize(a);
            }
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else {
            checkProblems();
            logger.info(getContentAsString());
            throw new PluginImplementationException();
        }
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

            Matcher matcher = PlugUtils.matcher("<p><span>. <a href=\"([^\"]+)\">P.ihl..en.</a> .</span></p>", content);
            if (!matcher.find()) {
                throw new PluginImplementationException("5");
            }
            String loginURL = matcher.group(1);
            GetMethod loginMethod = getGetMethod(loginURL);
            if(makeRequest(loginMethod)) {
                matcher = PlugUtils.matcher("<form name=\"([^\"]+)\" method=\"post\" action=\"([^\"]+)\">", getContentAsString());
                if (!matcher.find()) {
                    throw new PluginImplementationException("6");
                }
                String formName = matcher.group(1);
                String postURL = matcher.group(2);

                PostMethod postmethod = getPostMethod(postURL);

                postmethod.addParameter(formName + "_lg", pa.getUsername());
                postmethod.addParameter(formName + "_psw", pa.getPassword());
                postmethod.addParameter(formName + "_sbm", "Prihlasit");
                postmethod.addParameter("DownloadRedirect", "");

                if (makeRedirectedRequest(postmethod)) {
                    matcher = getMatcherAgainstContent("<h1>P.ihl..en.</h1>");
                    if(matcher.find()) {
                        badConfig=true;
                        throw new NotRecoverableDownloadException("Bad HellShare full account login information!");
                    }
                    GetMethod getMethod = getGetMethod(fileURL);
                    if (!makeRedirectedRequest(getMethod))
                        throw new PluginImplementationException("7");
                }
            } else {
                throw new PluginImplementationException("Bad login URL");
            }
        }
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException, NotRecoverableDownloadException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("Soubor nenalezen");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Soubor nenalezen</b><br>"));
        }
        matcher = getMatcherAgainstContent("Na serveru jsou .* free download");
        if (matcher.find()) {
            throw new YouHaveToWaitException("Na serveru jsou vyu�ity v�echny free download sloty", 30);
        }
        if(badConfig || getContentAsString().equals("")) {
            throw new NotRecoverableDownloadException("Bad HellShare full account login information!");
        }
    }
}