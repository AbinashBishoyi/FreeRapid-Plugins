package cz.vity.freerapid.plugins.services.multishare_mms;

import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import java.util.Random;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.methods.PostMethod;

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
    private static int counter=0;

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
    }


    @Override
    public void run() throws Exception {
        super.run();
        counter++;
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
            Random rnd=new Random();
            final String baseUrl="http://dl"+(rnd.nextInt(9999)+1)+".mms.multishare.cz/";
            final HttpMethod finalMethod=getMethodBuilder(getContentAsString()).setBaseURL(baseUrl).setActionFromFormByName("mms-form", true).setParameter("link", fileURL).toGetMethod();
            if (!tryDownloadAndSaveFile(finalMethod)) {
                checkProblems();
                logger.info(getContentAsString());
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void checkProblems() throws NotRecoverableDownloadException {
        if(getMatcherAgainstContent("Nem.te dostate.n. kredit na sta.en. tohoto souboru").find()){
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