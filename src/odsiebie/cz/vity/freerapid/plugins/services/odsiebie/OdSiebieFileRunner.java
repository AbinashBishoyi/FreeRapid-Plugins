package cz.vity.freerapid.plugins.services.odsiebie;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Class which contains main code
 *
 * @author Eterad
 */
class OdSiebieFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OdSiebieFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "Nazwa pliku:</dt>\n" + "<dd>", "</dd>");
        PlugUtils.checkFileSize(httpFile, content, "Rozmiar pliku:</dt>\n" + "<dd>", "</dd>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        if (login()){ //for rare captcha remove
            logger.info("Starting download in TASK " + fileURL);
            String fileID = PlugUtils.getStringBetween(fileURL, "odsiebie.com/pokaz/", ".html") + ".html";
            GetMethod method = getGetMethod(fileURL);
            if (makeRedirectedRequest(method)){
                String s = "http://odsiebie.com/pobierz/" + fileID;
                logger.info("File URL - " + s);
                client.setReferer(fileURL);
                method = getGetMethod(s);
                if (makeRedirectedRequest(method)) {
                    String old = s;
                    s = "http://odsiebie.com/download/" + fileID;
                    logger.info("Download URL - " + s);
                    client.setReferer(old);
                    method = getGetMethod(s);
                    if (!tryDownloadAndSaveFile(method)){
                        checkProblems();
                        logger.warning(getContentAsString());;
                        throw new PluginImplementationException();
                    }
                } else {
                    throw new ServiceConnectionProblemException("Couldn't connect to service.");
                }
            }   else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            throw new ServiceConnectionProblemException("Couldn't login to service.");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Wybierz")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("kod")){
            throw new ServiceConnectionProblemException("Captcha code required for this file. Download it mannually");
        }
    }

    private boolean login() throws Exception {
        String URL = "http://odsiebie.com";
        GetMethod gMethod = getGetMethod(URL);
        logger.info("1");
        if(makeRedirectedRequest(gMethod)){
            logger.info("2");
            PostMethod pMethod = getPostMethod("http://odsiebie.com/?login");
            pMethod.addParameter("luser", "frd@mailinator.com");
            pMethod.addParameter("lpass", "frdodsiebieplug");
            if(makeRedirectedRequest(pMethod)){
                logger.info("3");
                if(getContentAsString().contains("Zalogowany jako")){
                    logger.info("4");
                    return true;
                } else{
                    logger.info("5");
                    return false;
                }
            }

        }
        logger.info("6");
        return false;
    }

}
