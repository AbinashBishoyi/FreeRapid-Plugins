package cz.vity.freerapid.plugins.services.sharerapid;

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
class ShareRapidRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareRapidRunner.class.getName());
    private boolean badConfig = false;

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
            checkNameAndSize(getContentAsString());

            client.setReferer(fileURL);

            String serverURL = "http://" + getMethod.getURI().getHost();

            Matcher matcher = PlugUtils.matcher("<strong>Stahov.n. je p..stupn. pouze p.ihl..en.m u.ivatel.m</strong>", getContentAsString());
            if (matcher.find()) {
                Login(serverURL);
            }

            matcher = PlugUtils.matcher("<span style=\"padding: 12px 0px 0px 10px; display: block\"><a href=\"([^\"]+)\" title=\"[^\"]+\">[^<]+</a>", getContentAsString());
            if(matcher.find()) {
                String downURL = serverURL + matcher.group(1);

                final GetMethod method = getGetMethod(downURL);

                httpFile.setState(DownloadState.GETTING);
                if (!tryDownloadAndSaveFile(method)) {
                    if(getContentAsString().equals(""))
                        throw new NotRecoverableDownloadException("No credit for download this file!");
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
        if (getContentAsString().contains("<td class=\"i\">Velikost:</td>")) {
            Matcher matcher = PlugUtils.matcher("<title>(.*) - Share-Rapid</title>", content);
            if (matcher.find()) {
                String fn = matcher.group(1);
                httpFile.setFileName(fn);
            }
            matcher = PlugUtils.matcher("<td class=\"i\">Velikost:</td>[^<]+<td class=\"h\"><strong>[^0-9]+([0-9.]+ .B)</strong></td>", content);
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

    private void Login(String serverURL) throws Exception {
        synchronized (ShareRapidRunner.class) {
            ShareRapidServiceImpl service = (ShareRapidServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No ShareRapid account login information!");
                }
                badConfig = false;
            }

            Matcher matcher;
            String postURL = serverURL + "/prihlaseni/";

            GetMethod getmethod = getGetMethod(postURL);
            if (!makeRequest(getmethod))
                throw new PluginImplementationException();

            PostMethod postmethod = getPostMethod(postURL);

            PlugUtils.addParameters(postmethod, getContentAsString(), new String[]{"hash","sbmt"});
            postmethod.addParameter("login", pa.getUsername());
            postmethod.addParameter("pass1", pa.getPassword());

            if (makeRedirectedRequest(postmethod)) {
                matcher = getMatcherAgainstContent("<title>P.ehled ..tu - Share-Rapid</title>");
                if(!matcher.find()) {
                    badConfig=true;
                    throw new NotRecoverableDownloadException("Bad ShareRapid account login information!");
                }
                GetMethod getMethod = getGetMethod(fileURL);
                if (!makeRedirectedRequest(getMethod)) {
                    throw new PluginImplementationException();
                }
            }
        }
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException, NotRecoverableDownloadException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("<h1>Po.adovan. str.nka nebyla nalezena</h1>");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Soubor nenalezen</b><br>");
        }
        matcher = getMatcherAgainstContent("<strong>Ji. V.m do.el kredit a vy.erpal jste free limit</strong>");
        if (matcher.find()) {
            throw new NotRecoverableDownloadException("No credit for download!");
        }
        if(badConfig) {
            throw new NotRecoverableDownloadException("Bad ShareRapid account login information!");
        }
    }

}