package cz.vity.freerapid.plugins.services.myurl;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Arthur Gunawan
 */
class MyurlFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MyurlFileRunner.class.getName());


   // @Override
//    public void runCheck() throws Exception { //this method validates file
//        super.runCheck();
//        final GetMethod getMethod = getGetMethod(fileURL);//make first request
//        if (makeRedirectedRequest(getMethod)) {
//            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
//        } else
//            throw new PluginImplementationException();
//    }

//    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
//        PlugUtils.checkName(httpFile, content, "FileNameLEFT", "FileNameRIGHT");//TODO
//        PlugUtils.checkFileSize(httpFile, content, "FillSizeLEFT", "FileNameRIGHT");//TODO
//        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
//    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            String myURL = PlugUtils.getStringBetween(getContentAsString(),"<iframe scrolling=\"yes\" src=\"","\" id=\"contentFRM\"");


            //final Matcher matcher = getMatcherAgainstContent("<TITLE>(.+?)</TITLE>");
            //if (matcher.find()) {
                //final String s = matcher.group(1);
                try {
                    this.httpFile.setNewURL(new URL(myURL));
                } catch (MalformedURLException e) {
                    throw new URLNotAvailableAnymoreException("Invalid URL");
                }
                this.httpFile.setPluginID("");
                this.httpFile.setState(DownloadState.QUEUED);
//            } else {
//                checkProblems();
//                throw new PluginImplementationException();
//            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}
