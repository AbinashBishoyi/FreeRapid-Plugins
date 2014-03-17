package cz.vity.freerapid.plugins.services.multishare_mms;

import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class MultiShareMMSFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(MultiShareMMSFileRunner.class.getName());
    private static final String SERVER_URL = "http://www.multishare.cz/";
    private boolean badConfig = false;
    private static String PHPSESSID = "";
    private static String versionUrl="http://www.multishare.cz/html/mms_support.php?version";

    private static String version="1.1.13";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        checkNameAndSize();
    }

    private void checkNameAndSize() throws Exception {
        PostMethod pm = new PostMethod("http://www.multishare.cz/html/mms_ajax.php");
        pm.addParameter("link", fileURL);
        if (makeRequest(pm)) {
            String content=getContentAsString();
            if (content.equals("neexistuje")) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            if (content.equals("neznam")) {
                {
                    throw new URLNotAvailableAnymoreException("File URL not supported");
                }
            }
            PlugUtils.checkName(httpFile, content, "Soubor: <strong>", "</strong>");
            PlugUtils.checkFileSize(httpFile, content, "Velikost: <strong>", "</strong>");
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }
    }

    public void openBrowser(URL url) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            return;
        try {
            Desktop.getDesktop().browse(url.toURI());
        } catch (IOException e) {

        } catch (Exception ignored) {
            //ignore
        }
    }

    public void versionCheck() throws IOException, PluginImplementationException
    {
        GetMethod get=new GetMethod(versionUrl);
        if(makeRedirectedRequest(get))
        {
            String actualVersion=getContentAsString().trim();
            logger.info("Actual version:"+actualVersion);
            if(!actualVersion.equals(version))
            {
                if(JOptionPane.showOptionDialog(null, "Na webu byla nalezena nov\u011Bj\u0161\u00ED verze pluginu MultiShare MMS - verze "+actualVersion+"\nChcete ji stahnout\u003F Aktualizaci mus\u00EDte prov\u00E9st ru\u010Dn\u011B sta\u017Een\u00EDm z internetu.", "Nov\u011Bj\u0161\u00ED verze Multishare MMS", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null)==JOptionPane.YES_OPTION){
                    logger.info("Redirecting browser to plugin page");
                    openBrowser(new URL("http://www.multishare.cz/frd/plugin/"));
                    throw new PluginImplementationException("Je ke sta\u017Een\u00ED nov\u011Bj\u0161\u00ED verze - "+actualVersion);
                }
            }                        
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkNameAndSize();
        versionCheck();
        logger.info("Starting download in TASK " + fileURL);
        if (!PHPSESSID.equals("")) {
            client.getHTTPClient().getState().addCookie(new Cookie("www.multishare.cz", "PHPSESSID", PHPSESSID, "/", 86400, false));
        }

        final GetMethod method = getGetMethod(SERVER_URL);
        Matcher matcher;
        if (makeRedirectedRequest(method)) {

            if (getContentAsString().contains("<form id=\"form_prihlaseni\"")) {
                login();
                makeRedirectedRequest(method);
            }

            client.getHTTPClient().getParams().setParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true); //Multishare kurvi nazvy souboru v hlavicce podtrzitky

            Random rnd = new Random();
            final String baseUrl = "http://dl" + (rnd.nextInt(9999) + 1) + ".mms.multishare.cz/";
            final HttpMethod finalMethod = getMethodBuilder(getContentAsString()).setBaseURL(baseUrl).setActionFromFormByName("mms-form", true).setParameter("link", URLEncoder.encode(fileURL, "utf8")).toGetMethod();
            if (!tryDownloadAndSaveFile(finalMethod)) {
                checkProblems();
                logger.info(getContentAsString());
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void checkProblems() throws NotRecoverableDownloadException {
        if (getMatcherAgainstContent("Nem.te dostate.n. kredit na sta.en. tohoto souboru").find()) {
            throw new NotRecoverableDownloadException("No credit for download this file!");
        }
    }

    private void login() throws Exception {
        synchronized (MultiShareMMSFileRunner.class) {
            MultiShareMMSServiceImpl service = (MultiShareMMSServiceImpl) getPluginService();
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
}
