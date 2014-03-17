package cz.vity.freerapid.plugins.services.linkto;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class LinkToFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinkToFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            final Matcher match = PlugUtils.matcher("<iframe id=\"iframe\".+?src=\"(.+?)\\s*\"", contentAsString);
            if (match.find()) {
                this.httpFile.setNewURL(new URL(match.group(1))); //to setup new URL
                this.httpFile.setFileState(FileState.NOT_CHECKED);
                this.httpFile.setPluginID(""); //to run detection what plugin should be used for new URL, when file is in QUEUED state
                this.httpFile.setState(DownloadState.QUEUED);
            } else {
                throw new ErrorDuringDownloadingException("Unable to locate Link");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("El Link No se encuentra o fue removido")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}