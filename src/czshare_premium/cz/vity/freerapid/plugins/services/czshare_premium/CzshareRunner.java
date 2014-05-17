package cz.vity.freerapid.plugins.services.czshare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Jan Smejkal (edit from CZshare and RapidShare premium to CZshare profi)
 * @author ntoskrnl
 */
class CzshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CzshareRunner.class.getName());
    private final static int WAIT_TIME = 30;
    private final static String BASE_URL = "http://sdilej.cz";

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
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(filenameMatcher.group(1));

        final Matcher filesizeMatcher = getMatcherAgainstContent("Velikost:\\s*(.+?)\\s*</div>");
        if (!filesizeMatcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(filesizeMatcher.group(1).replace("i", "")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        normalizeFileURL();
        logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod method = getMethodBuilder().setAction(fileURL).setReferer("").setBaseURL(BASE_URL).toGetMethod();
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            method = getMethodBuilder().setActionFromFormWhereActionContains("profi_down", true)
                    .setBaseURL(BASE_URL)
                    .toPostMethod();
            final int status = client.makeRequest(method, false);
            if (status / 100 == 3) {
                final String dlLink = method.getResponseHeader("Location").getValue();
                method = getMethodBuilder().setAction(dlLink.replaceFirst("https:", "http:")).toGetMethod();
            }
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void login() throws Exception {
        synchronized (CzshareRunner.class) {
            CzshareServiceImpl service = (CzshareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No CZShare/Sdilej premium account login information");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction("http://sdilej.cz/index.php")
                    .setParameter("login-name", pa.getUsername())
                    .setParameter("login-password", pa.getPassword())
                    .setParameter("trvale", "on")
                    .setParameter("Prihlasit", "Přihlásit SSL")
                    .setBaseURL(BASE_URL)
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            if (getContentAsString().contains("Zadané jméno se neshoduje s heslem")) {
                throw new BadLoginException("Invalid CZShare/Sdilej premium account login information");
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("Soubor nenalezen");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Soubor nenalezen</b><br>");
        }
        matcher = getMatcherAgainstContent("Soubor expiroval");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Soubor expiroval</b><br>");
        }
        matcher = getMatcherAgainstContent("Soubor byl smaz.n jeho odesilatelem</strong>");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Soubor byl smaz�n jeho odesilatelem</b><br>");
        }
        matcher = getMatcherAgainstContent("Tento soubor byl na upozorn.n. identifikov.n jako warez\\.</strong>");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Tento soubor byl na upozorn�n� identifikov�n jako warez</b><br>");
        }
        matcher = getMatcherAgainstContent("Bohu.el je vy.erp.na maxim.ln. kapacita FREE download.");
        if (matcher.find()) {
            throw new YouHaveToWaitException("Bohužel je vyčerpána maximální kapacita FREE downloadů", WAIT_TIME);
        }
        if (getContentAsString().equals("")) {
            throw new NotRecoverableDownloadException("Bad CZShare/Sdilej profi account login information!");
        }
    }

    private void normalizeFileURL() {
        fileURL = fileURL.replaceFirst("czshare\\.cz", "czshare.com");
        fileURL = fileURL.replaceFirst("czshare\\.com", "sdilej.cz");
        fileURL = fileURL.replaceFirst("https:", "http:");
    }

}