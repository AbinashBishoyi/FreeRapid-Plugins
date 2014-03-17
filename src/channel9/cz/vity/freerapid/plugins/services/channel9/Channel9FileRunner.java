package cz.vity.freerapid.plugins.services.channel9;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Abinash Bishoyi
 */
class Channel9FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Channel9FileRunner.class.getName());
    private Channel9SettingsConfig config;

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        setConfig();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        //if (fileURL.contains("media.ch9.ms")) {
        //    checkNameAndSize(fileURL.toString());
        //} else {
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        //}
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        switch (config.getQualitySetting()) {
            case 0:
                PlugUtils.checkName(httpFile, content, "/", "\">MP3");
                //PlugUtils.checkFileSize(httpFile, content, "FileSizeLEFT", "FileSizeRIGHT");
                break;
            case 1:
                PlugUtils.checkName(httpFile, content, "/", "\">MP4");
                //PlugUtils.checkFileSize(httpFile, content, "FileSizeLEFT", "FileSizeRIGHT");
                break;
            case 2:
                PlugUtils.checkName(httpFile, content, "/", "\">Mid Quality WMV");
                //PlugUtils.checkFileSize(httpFile, content, "FileSizeLEFT", "FileSizeRIGHT");
                break;
            case 3:
                PlugUtils.checkName(httpFile, content, "/", "\">High Quality MP4");
                //PlugUtils.checkFileSize(httpFile, content, "FileSizeLEFT", "FileSizeRIGHT");
                break;
            case 4:
                PlugUtils.checkName(httpFile, content, "/", "\">Mid Quality MP4");
                //PlugUtils.checkFileSize(httpFile, content, "FileSizeLEFT", "FileSizeRIGHT");
                break;
            case 5:
                PlugUtils.checkName(httpFile, content, "/", "\">High Quality WMV");
                //PlugUtils.checkFileSize(httpFile, content, "FileSizeLEFT", "FileSizeRIGHT");
                break;
            default:
                httpFile.setFileName(content);
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setConfig();

        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL); //create GET request
        //if(fileURL.contains("media.ch9.ms"))  {
        //here is the download link extraction
        //    if (!tryDownloadAndSaveFile(method)) {
        //        checkProblems();//if downloading failed
        //        throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        //    }
        //} else {
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            //HttpMethod httpMethod = getMethodBuilder().toHttpMethod();
            switch (config.getQualitySetting()) {
                case 0:
                    method = getGetMethod(PlugUtils.getStringBetween(contentAsString, "href=\"", "\">MP3"));
                    break;
                case 1:
                    method = getGetMethod(PlugUtils.getStringBetween(contentAsString, "href=\"", "\">MP4"));
                    break;
                case 2:
                    method = getGetMethod(PlugUtils.getStringBetween(contentAsString, "href=\"", "\">Mid Quality WMV"));
                    break;
                case 3:
                    method = getGetMethod(PlugUtils.getStringBetween(contentAsString, "href=\"", "\">High Quality MP4"));
                    break;
                case 4:
                    method = getGetMethod(PlugUtils.getStringBetween(contentAsString, "href=\"", "\">Mid Quality MP4"));
                    break;
                case 5:
                    method = getGetMethod(PlugUtils.getStringBetween(contentAsString, "href=\"", "\">High Quality WMV"));
                    break;
            }

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        //}
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void setConfig() throws Exception {
        final Channel9ServiceImpl service = (Channel9ServiceImpl) getPluginService();
        config = service.getConfig();
    }

}