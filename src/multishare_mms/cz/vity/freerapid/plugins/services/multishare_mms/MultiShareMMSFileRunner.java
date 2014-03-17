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
import java.net.URLDecoder;
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
    private static final String API_URL = "https://www.multishare.cz/api/";
    private boolean badConfig = false;
    private static String versionUrl="http://www.multishare.cz/html/mms_support.php?version";

    private static String version="1.2.3";
    
    private static final String HTTP_USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.10) Gecko/2009042523 Ubuntu/9.04 (jaunty) Firefox/3.0.10";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        setClientParameter(DownloadClientConsts.USER_AGENT, HTTP_USER_AGENT);
        
        checkNameAndSize();
    }

    private void checkNameAndSize() throws Exception {
        HttpMethod pm=getMethodBuilder()
                .setAction(API_URL+"?sub=check-file")
                .setParameter("link", fileURL).toPostMethod();
        if (makeRequest(pm)) {
            String content=getContentAsString();
            if(content.startsWith("ERR:")){
                throw new URLNotAvailableAnymoreException(content.substring(4));
            }
            PlugUtils.checkName(httpFile, content, "\"file_name\":\"", "\"");
            
            String fileSize=PlugUtils.getStringBetween(content, "\"file_size\":", ",");
            if(fileSize.startsWith("\"")){
                fileSize = fileSize.substring(1);
            }
            if(fileSize.endsWith("\"")){
                fileSize = fileSize.substring(0,fileSize.length()-1);
            }
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));
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
        setClientParameter(DownloadClientConsts.USER_AGENT, HTTP_USER_AGENT);
        versionCheck();    
        checkNameAndSize();        
        String jmeno = "";
        String heslo = "";
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
            jmeno = pa.getUsername();
            heslo = pa.getPassword();
        }
        HttpMethod pm=getMethodBuilder()
        
                .setAction(API_URL+"?sub=download-link")
                .setParameter("login", jmeno)
                .setParameter("password", heslo)
                .setParameter("link", fileURL).toPostMethod();
        if (makeRequest(pm)) {
            String content=getContentAsString();
            if(content.startsWith("ERR:")){
                throw new NotRecoverableDownloadException(content.substring(4));
            }
            String link = PlugUtils.getStringBetween(content, "\"link\":\"", "\"");
            link = link.replace("\\/", "/");
            link = URLDecoder.decode(link, "UTF-8");
            if (!tryDownloadAndSaveFile(getGetMethod(link))) {
                logger.info(getContentAsString());
                throw new ServiceConnectionProblemException();
            }
        }
    }
}
