package cz.vity.freerapid.plugins.services.imageshack;

import cz.vity.freerapid.plugins.exceptions.BuildMethodException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;

import java.net.URI;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class ImageShackFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ImageShackFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new PluginImplementationException();
        }

    }

    private void checkNameAndSize(String contentAsString) throws PluginImplementationException {
        PlugUtils.checkName(httpFile, contentAsString, "my.php?image=", "&quot;");
        //name is automatically taken from the file URL, there is no available full file name on their page with extension
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws URLNotAvailableAnymoreException, ServiceConnectionProblemException {
        if (getContentAsString().contains("404 - Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());

            final String action = getMethodBuilder().setActionFromAHrefWhereATagContains("Full Size").getAction();
            final String s = new URI(encode(fileURL)).resolve(action).toURL().toExternalForm();
            logger.info("Link " + s);
            final HttpMethod httpMethod = getMethodBuilder().setAction(s).toHttpMethod();
            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString()); //log the info
                throw new PluginImplementationException(); //unknown problem
            }
        } else {
            checkProblems();
            throw new PluginImplementationException();
        }
    }

    private String encode(String s) throws BuildMethodException {
        try {
            return URIUtil.encodePathQuery(s, "UTF-8");
        } catch (URIException e) {
            throw new BuildMethodException("Cannot create URI");
        }
    }

}