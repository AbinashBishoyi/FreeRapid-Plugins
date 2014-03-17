package cz.vity.freerapid.plugins.services.imagefap;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class ImageFapFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ImageFapFileRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();

        final Matcher galleryMatcher = PlugUtils.matcher("http://(?:www\\.)?imagefap\\.com/pictures/.*?(\\?view=2)?", fileURL);
        final Matcher singleMatcher = PlugUtils.matcher("http://(?:www\\.)?imagefap\\.com/(image\\.php|photo).*", fileURL);
        logger.info("Starting download in TASK " + fileURL);

        //gallery
        if (galleryMatcher.find()) {
            String pageURL;
            if (galleryMatcher.group(1) == null) {
                pageURL = fileURL + "?view=2"; //switch to whole page
            } else {
                pageURL = fileURL; // already whole page
            }
            //logger.info("current URL : " + pageURL);
            final GetMethod method = getGetMethod(pageURL); //create GET request
            if (makeRedirectedRequest(method)) { //we make the main request
                final String contentAsString = getContentAsString();//check for response
                checkProblems();//check problems

                final Matcher galleryMatcher2 = PlugUtils.matcher("href=\"/image\\.php\\?(.*?)\">", contentAsString);
                final List<URI> urlList = new LinkedList<URI>();
                //add links to queue
                while (galleryMatcher2.find()) {
                    String siteURL = "http://imagefap.com/image.php?" + PlugUtils.unescapeHtml(galleryMatcher2.group(1));
                    urlList.add(new URI(siteURL));
                }
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, urlList);
            }
        } else if (singleMatcher.find()) { //single file
            final GetMethod method = getGetMethod(fileURL); //create GET request
            if (makeRedirectedRequest(method)) { //we make the main request
                final String contentAsString = getContentAsString();//check for response
                checkProblems();//check problems

                final Matcher filenameMatcher = PlugUtils.matcher("<img id=\"mainPhoto\".*?src=\".*?/([^/]+\\.\\w+)\">", contentAsString);
                filenameMatcher.find();
                httpFile.setFileName(filenameMatcher.group(1));

                final HttpMethod httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromImgSrcWhereTagContains("mainPhoto")
                        .toHttpMethod();

                //here is the download link extraction
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
        } else

        {
            throw new ServiceConnectionProblemException();
        }
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        } else if (contentAsString.contains("The Gallery you requested doesn't exist")) {
            throw new URLNotAvailableAnymoreException("Gallery doesn't exist"); //let to know user in FRD
        }
    }

}