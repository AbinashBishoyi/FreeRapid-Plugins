package cz.vity.freerapid.plugins.services.flickrcollections;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Arthur
 */
class flickrCollectionsFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(flickrCollectionsFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
//        PlugUtils.checkName(httpFile, content, "FileNameLEFT", "FileNameRIGHT");//TODO
//        PlugUtils.checkFileSize(httpFile, content, "FillSizeLEFT", "FileSizeRIGHT");//TODO
//        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        String baseURL = fileURL;
        //http://www.flickr.com/photos/calosa/
        //http://www.flickr.com/photos/calosa/page2/


        if (!makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
        String contentAsString = getContentAsString();//check for response
        parseWebsite();
        //get maximum page
        int i = 2;

        while (contentAsString.contains("page" + i)) {
            method = getGetMethod(baseURL + "page" + i + "/");
            if (!makeRedirectedRequest(method)) { //we make the main request
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }

            parseWebsite();
            contentAsString = getContentAsString();
            logger.info("Page : " + i);
            
            i++;

        }


    }


    private void parseWebsite() {
        final Matcher matcher = getMatcherAgainstContent("photo_container.+href=\"([^\"]+)");
        //http://www.flickr.com/photos/calosa/4173384151/
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find(start)) {
            final String link = "http://www.flickr.com" + matcher.group(1);
            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            start = matcher.end();
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}