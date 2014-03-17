package cz.vity.freerapid.plugins.services.quickshare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author JPEXS
 * (parts taken from Lukiz QuickShare plugin)
 */
class QuickShareFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(QuickShareFileRunner.class.getName());
    private static String PHPSESSID = null;

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            throw new PluginImplementationException();
        }
    }

    private void checkNameAndSize(String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("zev: <strong>([^<]*)</strong>", content);
        if (matcher.find()) {
            String fn = matcher.group(matcher.groupCount());
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
        }
        matcher = PlugUtils.matcher("([0-9.]+)</strong>( .B)", content);
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1) + matcher.group(2));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }
    }

    private void downloadFileGet(String downloadURL) throws Exception{
        final HttpMethod httpMethod = getGetMethod(downloadURL);
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (PHPSESSID != null) {
            addCookie(new Cookie("www.quickshare.cz", "PHPSESSID", PHPSESSID, "/", 8640, false));
        }
        final GetMethod method = getGetMethod(fileURL); //create GET request        
        int status = client.makeRequest(method, false);
        if (status == HttpStatus.SC_MOVED_TEMPORARILY) {
            logger.info("Direct download mode");
            downloadFileGet(method.getResponseHeader("location").getValue());
            return;
        }

            String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);//extract file name and size from the page            
            if (!content.contains("Kredit: ")) {
                login();
                status = client.makeRequest(method, false);
                if (status == HttpStatus.SC_MOVED_TEMPORARILY) {
                    logger.info("Direct download mode");
                    downloadFileGet(method.getResponseHeader("location").getValue());
                    return;
                }
            }
            content = getContentAsString();
            int UU_kredit = getIntVar("UU_kredit", content);
            int velikost = getIntVar("velikost", content);
            if ((UU_kredit >= velikost) && (UU_kredit != 0)) {
                String server = getVar("server", content);
                String id1 = getVar("ID1", content);
                String id2 = getVar("ID2", content);
                String id4 = getVar("ID4", content);
                String id5 = getVar("ID5", content);
                downloadFileGet(server + "/download_premium.php?ID1=" + id1 + "&ID2=" + id2 + "&ID4=" + id4 + "&ID5=" + id5);
            } else {
                throw new NotRecoverableDownloadException(String.format("<b>Chyba! Nedostatek kreditu ke sta\u017Eení. Kredit:" + UU_kredit + " MB</b><br>"));
            }        
        
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        String content = getContentAsString();
        if (content.contains("location.href='/chyba'")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Chyba! Soubor z\u0159ejm\u011B neexistuje</b><br>"));
        }
    }

    private void login() throws Exception {
        synchronized (QuickShareFileRunner.class) {
            QuickShareServiceImpl service = (QuickShareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No QuickShare Premium account login information!");
                }
            }
            Cookie[] cookies = client.getHTTPClient().getState().getCookies();
            for (Cookie c : cookies) {
                if ("PHPSESSID".equals(c.getName())) {
                    QuickShareFileRunner.PHPSESSID = c.getValue();
                }
            }
            client.getHTTPClient().getParams().setContentCharset("UTF-8"); //Necessary for sending "Prihlasit" string
            HttpMethod method = getMethodBuilder().setAction("/html/prihlaseni_process.php").setBaseURL("http://www.quickshare.cz").setReferer(fileURL).setParameter("jmeno", pa.getUsername()).setParameter("heslo", pa.getPassword()).setParameter("akce", "P\u0159ihl\u00E1sit").toPostMethod();
            client.makeRequest(method, false);
            Header h = method.getResponseHeader("location");
            if ((h != null) && h.getValue().equals("/premium")) {
                logger.info("Logged in");
            }else{
                throw new BadLoginException("Bad login information");
            }
        }
    }

    private String getVar(String s, String contentAsString) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("var " + s + " = '([^']*)'", contentAsString);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new PluginImplementationException("Parameter " + s + " was not found");
        }
    }

    private int getIntVar(String s, String contentAsString) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("var " + s + " = ([0-9]+);", contentAsString);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            throw new PluginImplementationException("Parameter " + s + " was not found");
        }
    }
}
