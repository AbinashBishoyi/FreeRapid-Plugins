package cz.vity.freerapid.plugins.services.flickr;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * Class which contains main code
 *
 * @author Arthur
 */
class FlickrFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FlickrFileRunner.class.getName());


    private String getID() throws Exception {
        Matcher matcher = PlugUtils.matcher("/photos/.+/([0-9]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Can't get ID");
        }
        return matcher.group(1);


    }

    private String getServer(String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("(http://farm[0-9].static.flickr.com/[0-9]+/[0-9]+_[a-z0-9]+)", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("Can't get ID");
        }
        return matcher.group(1);
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (!content.contains("page_p.title = '';")) {
            String title = PlugUtils.getStringBetween(content, "page_p.title = '", "';") + ".jpg";
            httpFile.setFileName(title);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }


    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        if (!makeRedirectedRequest(method)) { //we make the main request
            checkProblems();
            throw new ServiceConnectionProblemException("Connection Error");
        }

        final String contentAsString = getContentAsString();//check for response
        checkProblems();//check problems
        checkNameAndSize(contentAsString);//extract file name and size from the page
        String apiKey = "f2949bc8f2e7d566279784478033e72a";
        String mUrl = "http://api.flickr.com/services/rest/?method=flickr.photos.getSizes&api_key=" + apiKey + "&photo_id=" + getID() + "\"&format=json&nojsoncallback=1";
        method = getGetMethod(mUrl);


        if (!makeRedirectedRequest(method)) { //we make the main request
            checkProblems();
            throw new ServiceConnectionProblemException("Connection Error");
        }
        logger.info(getContentAsString());
        method = getGetMethod(getSize());
        if (!tryDownloadAndSaveFile(method)) { //we make the main request
            if (!downloadThis(contentAsString, "_o_d.jpg")) {
                checkProblems();
                throw new ServiceConnectionProblemException("Connection Error");
            }
        }
    }

    private String getSize() throws Exception {
        String content = getContentAsString();
        String xMatch = "";
        String xAdd = "";

        if (content.contains("_o.jpg")) xAdd = "_o";
        else if (content.contains("_b.jpg")) xAdd = "_b";
        else if (content.contains("Medium")) xAdd = "";
        else if (content.contains("_m.jpg")) xAdd = "_m";
        else if (content.contains("_t.jpg")) xAdd = "_t";
        else xAdd = "_s";


        xMatch = "farm([0-9]).static.flickr.com\\\\/([0-9]+)\\\\/([0-9]+_[a-z0-9]+" + xAdd + ")(.jpg)";
        logger.info("Match : " + xMatch + ", xAdd : " + xAdd);

        Matcher matcher = PlugUtils.matcher(xMatch, content);
        if (!matcher.find()) {
            throw new ServiceConnectionProblemException("Key have changed, wait for plugin update");

        }
        String finLink = "http://farm" + matcher.group(1) + ".static.flickr.com/" + matcher.group(2) + "/" + matcher.group(3) + "_d" + matcher.group(4);
        logger.info("Found Link : " + finLink);
        return finLink;
    }

    private boolean downloadThis(String content, String code) throws Exception {
        String pictureURL = getServer(content) + code;
        logger.info("Trying URL : " + pictureURL);

        GetMethod method = getGetMethod(pictureURL);
        int ret = client.makeRequest(method, false);
        if (!(ret == 200)) return false;

        logger.info("Working URL : " + pictureURL);

        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
        return true;


    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}