package cz.vity.freerapid.plugins.services.czshare_profi;

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
 * @author Ladislav Vitasek, Ludek Zika, Jan Smejkal (edit from Hellshare to CZshare)
 */
class CzshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CzshareRunner.class.getName());
    private boolean badConfig = false;
    private final static int WAIT_TIME = 30;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
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

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            String content = getContentAsString();
            checkNameAndSize(content);

            client.setReferer(fileURL);

            Matcher matcher = PlugUtils.matcher("<div class=\"profy-download\">[ ]*\n[ ]*<form action=\"([^\"]*)\" method=\"post\">", content);
            if (!matcher.find()) {
                throw new PluginImplementationException();
            }
            String postURL = matcher.group(1);

            final PostMethod postmethod = getPostMethod(postURL);

            PlugUtils.addParameters(postmethod, getContentAsString(), new String[]{"id", "file"});
            String id = postmethod.getParameter("id").getValue();

            if (makeRedirectedRequest(postmethod)) {
                matcher = getMatcherAgainstContent("<span class=\"nadpis\">P.ihl..en.</span>");
                if(matcher.find())
                    Login(getContentAsString());

                content = getContentAsString();
                matcher = PlugUtils.matcher("<a href=\"(.*czshare.com/" + id + "/[^\"]*)\" title=\"" + httpFile.getFileName() + "\">" + httpFile.getFileName() + "</a>", content);
                if(matcher.find())
                {
                    String downURL = matcher.group(1);

                    GetMethod method = getGetMethod(downURL);
                    httpFile.setState(DownloadState.GETTING);
                    if (!tryDownloadAndSaveFile(method)) {
                        if(getContentAsString().equals(""))
                            throw new NotRecoverableDownloadException("No credit for download this file!");
                        checkProblems();
                        logger.info(getContentAsString());
                        throw new PluginImplementationException();
                    } else {
                        matcher = PlugUtils.matcher("<form action=\'([^\']+)\' method=\'POST\'>", content);
                        if(matcher.find()) {
                            String delURL = "http://czshare.com/profi/" + matcher.group(1);
                            matcher = PlugUtils.matcher("<a href=\"" + downURL + "\" title=\"" + httpFile.getFileName() + "\">" + httpFile.getFileName() + "</a></td><td><input type=\'checkbox\' name=\'[^\']+\' value=\'([^\']+)\'></td>", content);
                            if(matcher.find()) {
                                String delFile = matcher.group(1);
                                PostMethod postMethod = getPostMethod(delURL);
                                postMethod.addParameter("smaz[0]", delFile);
                                makeRequest(postMethod);
                            }
                        }
                    }
                } else {
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

    private void Login(String content) throws Exception {
        synchronized (CzshareRunner.class) {
            CzshareServiceImpl service = (CzshareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No CZshare profi account login information!");
                }
                badConfig = false;
            }

            Matcher matcher = PlugUtils.matcher("<form action=\"([^\"]*)\" method=\"post\">[ \n]+<input type=\"hidden\" name=\"step\" value=\"1\" />", content);
            if (!matcher.find()) {
                throw new PluginImplementationException();
            }
            String postURL = "http://czshare.com" + matcher.group(1);

            final PostMethod postmethod = getPostMethod(postURL);

            PlugUtils.addParameters(postmethod, getContentAsString(), new String[]{"id", "file", "step", "prihlasit"});
            postmethod.addParameter("jmeno", pa.getUsername());
            postmethod.addParameter("heslo", pa.getPassword());

            if (makeRedirectedRequest(postmethod)) {
                matcher = getMatcherAgainstContent("<span class=\"nadpis\">P.ihl..en.</span>");
                if(matcher.find()) {
                    badConfig=true;
                    throw new NotRecoverableDownloadException("Bad CZshare profi account login information!");
                }
            }
        }
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException, NotRecoverableDownloadException {
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
        if(badConfig) {
            throw new NotRecoverableDownloadException("Bad CZshare profi account login information!");
        }
    }

}