package cz.vity.freerapid.plugins.services.imageshack;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class ImageShackFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ImageShackFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        final int status = client.makeRequest(method, false); //main request
        if (status == HttpStatus.SC_OK) {
            final String contentAsString = getContentAsString();//check for response

            //extract download link from page
            String link = PlugUtils.getStringBetween(fileURL, "", ".us/") + ".us/" + //continues on next line...
                    PlugUtils.getStringBetween(contentAsString, "\"Full Size\"/></a><div><a href=\"/", "\" target=");
            logger.info("Link " + link);

            //ImageShack doesn't provide the filename in the file header, hence this
            String filename = grabFilename(link);
            httpFile.setFileName(filename); //try commenting this line out to see why it's necessary
            logger.info("Filename " + filename);

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(link).toHttpMethod();
            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                logger.warning(getContentAsString()); //log the info
                throw new PluginImplementationException(); //unknown problem
            }
        } else {
            if (status / 100 == 3) { //if HttpStatus==redirection
                throw new URLNotAvailableAnymoreException("File not found");
            } else if (getContentAsString().contains("404 - Not Found")) {
                throw new URLNotAvailableAnymoreException("File not found");
            } else {
                throw new ServiceConnectionProblemException(); //unknown problem
            }
        }
    }

    private String grabFilename(String url) { //ugly hack
        String filename = url;
        while (true) {
            try {
                filename = PlugUtils.getStringBetween(filename + "?", "/", "?");
            } catch (PluginImplementationException e) {
                break;
            }
        }
        return filename;
    }

}