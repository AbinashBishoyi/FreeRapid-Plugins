package cz.vity.freerapid.plugins.services.sharerapid;

import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
 * @author Jan Smejkal (edit from CZshare profi to RapidShare)
 * @ edit František Musil (lister@gamesplit.cz, repair multidownload)
 */
class ShareRapidRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareRapidRunner.class.getName());
    private boolean badConfig = false;
    //time to next check (seconds)
    private final static Integer timeToCheck = 120;
    private final static Integer maxReconnect = 30;

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

            Matcher matcher = PlugUtils.matcher("(Stahování je povoleno pouze pro přihlášené uživatele|<strong>Stahov.n. je p..stupn. pouze p.ihl..en.m u.ivatel.m</strong>)", getContentAsString());
            if (matcher.find()) {
                Login(serverURL);
            }

            matcher = PlugUtils.matcher("<span style=\"padding: 12px 0px 0px 10px; display: block\"><a href=\"([^\"]+)\" title=\"[^\"]+\">[^<]+</a>", getContentAsString());
            if (matcher.find()) {
                String downURL = matcher.group(1);
                if (!downURL.contains("http://"))
                    downURL = serverURL + downURL;
                for (int i = 0; i <= maxReconnect; i++) {
                    final GetMethod method = getGetMethod(downURL);

                    httpFile.setState(DownloadState.GETTING);
                    if (tryDownloadAndSaveFile(method))
                        return;
                    if (!getContentAsString().equals(""))
                        checkProblems();
                    downloadTask.sleep(timeToCheck);

                    /*
                    if (!tryDownloadAndSaveFile(method)) {
                        if(getContentAsString().equals(""))
                            throw new NotRecoverableDownloadException("No credit for download this file!");
                        checkProblems();
                        logger.info(getContentAsString());
                        throw new PluginImplementationException();
                    }
                    */
                }
                if (getContentAsString().equals(""))
                    throw new NotRecoverableDownloadException("No credit for download this file or too many downloads!");
                checkProblems();
                throw new PluginImplementationException();
            } else {
                checkProblems();
                throw new PluginImplementationException();
            }
        } else
            throw new ServiceConnectionProblemException();

    }

    private void checkNameAndSize(String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("<span style=\"padding: 12px 0px 0px 10px; display: block\">(.+?)<", content);
        if (!matcher.find())
            throw new PluginImplementationException("Filename not found");
        httpFile.setFileName(matcher.group(1).trim());

        matcher = PlugUtils.matcher("<td class=\"i\">Velikost:</td>\\s*?<td class=\"h\"><strong>\\s*?([0-9].+?B)</strong></td>", content);
        if (matcher.find()) {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).trim().replace("iB", "B")));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void Login(String serverURL) throws Exception {
        synchronized (ShareRapidRunner.class) {
            ShareRapidServiceImpl service = (ShareRapidServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No ShareRapid account login information!, Stahování je přístupné pouze přihlášeným uživatelům");
                }
                badConfig = false;
            }

            Matcher matcher;
            String postURL = serverURL + "/prihlaseni/";

            GetMethod getmethod = getGetMethod(postURL);
            if (!makeRequest(getmethod))
                throw new PluginImplementationException();

            PostMethod postmethod = getPostMethod(postURL);

            PlugUtils.addParameters(postmethod, getContentAsString(), new String[]{"hash", "sbmt"});
            postmethod.addParameter("login", pa.getUsername());
            postmethod.addParameter("pass1", pa.getPassword());

            if (makeRedirectedRequest(postmethod)) {
                matcher = getMatcherAgainstContent("<title>Přihlášení - Share-Rapid</title>");
                if (matcher.find()) {
                    badConfig = true;
                    throw new NotRecoverableDownloadException("Bad ShareRapid account login information!");
                }
                GetMethod getMethod = getGetMethod(fileURL);
                if (!makeRedirectedRequest(getMethod)) {
                    throw new PluginImplementationException();
                }
            }
        }
    }

    private void checkProblems() throws Exception {
        final String content = getContentAsString();
        if (content.contains("Soubor byl smazán"))
            throw new URLNotAvailableAnymoreException("Soubor byl smazán");
        //if (content.contains("Stahování je přístupné pouze přihlášeným uživatelům"))
        //    throw new ErrorDuringDownloadingException("Stahování je přístupné pouze přihlášeným uživatelům");
        if (content.contains("Stahování zdarma je možné jen přes náš"))
            throw new NotRecoverableDownloadException("Stahování zdarma je možné jen přes náš download manager");
        if (content.contains("Soubor nelze stáhnout, aktuálně nemáte aktivní žádné předplacené služby."))
            throw new NotRecoverableDownloadException("Soubor nelze stáhnout, aktuálně nemáte aktivní žádné předplacené služby.");

        Matcher matcher;
        matcher = getMatcherAgainstContent("<h1>Po.adovan. str.nka nebyla nalezena</h1>");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Soubor nenalezen</b><br>");
        }
        matcher = getMatcherAgainstContent("<strong>Ji. V.m do.el kredit a vy.erpal jste free limit</strong>");
        if (matcher.find()) {
            throw new NotRecoverableDownloadException("No credit for download!");
        }
        if (badConfig) {
            throw new NotRecoverableDownloadException("Bad ShareRapid account login information!");
        }
    }

}