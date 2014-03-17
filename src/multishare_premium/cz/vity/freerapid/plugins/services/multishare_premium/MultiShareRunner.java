package cz.vity.freerapid.plugins.services.multishare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author JPEXS
 */
class MultiShareRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(MultiShareRunner.class.getName());
    private static final String SERVER_URL = "http://www.multishare.cz/";
    private boolean badConfig = false;
    private static String PHPSESSID = "";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (makeRedirectedRequest(getGetMethod(fileURL))) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            makeRedirectedRequest(getGetMethod(fileURL));
            checkProblems();
            throw new PluginImplementationException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        if (!PHPSESSID.equals("")) {
            client.getHTTPClient().getState().addCookie(new Cookie("www.multishare.cz", "PHPSESSID", PHPSESSID, "/", 86400, false));
        }

        final GetMethod method = getGetMethod(fileURL);
        Matcher matcher;
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());            
            if (getContentAsString().contains("<form id=\"form_prihlaseni\"")) {
                login();
                makeRedirectedRequest(method);
            }
            matcher = getMatcherAgainstContent("<a href=\"(/html/download_premium.php[^\"]+)\"");
            if (matcher.find()) {
                final HttpMethod method2 = getMethodBuilder().setReferer(fileURL).setAction(SERVER_URL + matcher.group(1)).toHttpMethod();

                if (!tryDownloadAndSaveFile(method2)) {
                    if (getContentAsString().equals("")) {
                        throw new NotRecoverableDownloadException("No credit for download this file!");
                    }
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }
            } else {
                matcher = getMatcherAgainstContent("p..li. m.lo kreditu");
                if (matcher.find()) {
                    throw new NotRecoverableDownloadException("No credit for download this file!");
                } else {
                    throw new PluginImplementationException("Cannot find premium download link");
                }
            }
        } else {
            throw new PluginImplementationException();
        }

    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "zev: <strong>", "</strong>");
        String sizeStr = PlugUtils.getStringBetween(content, "Velikost: <strong>", "</strong>");
        sizeStr = sizeStr.replace("&nbsp;", " ");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(sizeStr));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void login() throws Exception {
        synchronized (MultiShareRunner.class) {
            MultiShareServiceImpl service = (MultiShareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No MultiShare premium account login information!");
                }
                badConfig = false;
            }

            final PostMethod postmethod = getPostMethod("http://www.multishare.cz/html/prihlaseni_process.php");
            postmethod.addParameter("jmeno", pa.getUsername());
            postmethod.addParameter("heslo", pa.getPassword());
            postmethod.addParameter("trvale", "ano");
            char prihlasitBytes[] = new char[]{'P', (char) 0xc5, (char) 0x99, 'i', 'h', 'l', (char) 0xc3, (char) 0xa1, 's', 'i', 't'};
            postmethod.addParameter("akce", new String(prihlasitBytes));
            logger.info("Logging in...");
            postmethod.setQueryString(PHPSESSID);
            if (makeRedirectedRequest(postmethod)) {
                if ("".equals(getContentAsString())) {
                    throw new PluginImplementationException("Null response from login page");
                }
                Matcher matcher = getMatcherAgainstContent("Chybn?. jm.no nebo heslo");
                if (matcher.find()) {
                    badConfig = true;
                    throw new NotRecoverableDownloadException("Bad MultiShare premium account login information!");
                } else {
                    Cookie[] cookies = client.getHTTPClient().getState().getCookies();
                    for (Cookie c : cookies) {
                        if ("PHPSESSID".equals(c.getName())) {
                            PHPSESSID = c.getValue();
                        }
                    }
                    logger.info("Logged in");
                }
            }
        }
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, NotRecoverableDownloadException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains(" soubor neexistuje.")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }

        if (badConfig || getContentAsString().equals("")) {
            throw new NotRecoverableDownloadException("Bad MultiShare premium account login information!");
        }
    }
}
