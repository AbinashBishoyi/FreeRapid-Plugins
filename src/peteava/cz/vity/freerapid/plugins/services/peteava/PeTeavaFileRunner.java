package cz.vity.freerapid.plugins.services.peteava;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class PeTeavaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PeTeavaFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<meta name=\"title\" content=\"", "\" />");
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean login() throws Exception {
        synchronized (PeTeavaFileRunner.class) {
            PeTeavaServiceImpl service = (PeTeavaServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();

            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer("http://www.peteava.ro/autentificare")
                    .setAction("http://www.peteava.ro/autentificare/login")
                    .setParameter("username", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (getContentAsString().contains("Numele de utilizator sau parola sunt gresite"))
                throw new BadLoginException("Invalid PeTeava registered account login information!");

            return true;
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        login();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkFileProblems();
            checkDownloadProblems();
            checkNameAndSize(contentAsString);
            final String fid;
            if (getContentAsString().contains("hd_file=&")) { //SD
                fid = PlugUtils.getStringBetween(getContentAsString(), "flashvars=\"streamer=http://content.peteava.ro/stream.php&file=", "&image=");
            } else { //HD, default
                fid = PlugUtils.getStringBetween(getContentAsString(),"hd_file=","&hd_image");
            }
            logger.info("fid : " + fid);
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setBaseURL("http://content.peteava.ro")
                    .setAction("/video/" + fid + "?start=0&units=s&token=PETEAVARO&authcode=")
                    .toGetMethod();
            setClientParameter("dontUseHeaderFilename", true);
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkDownloadProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkFileProblems();
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Ne pare rau dar nu am gasit materialul")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Trebuie sa fiti logat si sa aveti peste 18 ani pentru a vizualiza acest material")) {
            throw new PluginImplementationException("You must logged in and over 18 years to view this material");
        }
    }

}