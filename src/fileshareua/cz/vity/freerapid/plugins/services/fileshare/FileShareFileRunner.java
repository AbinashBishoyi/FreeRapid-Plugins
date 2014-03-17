package cz.vity.freerapid.plugins.services.fileshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Saikek
 */
class FileShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileShareFileRunner.class.getName());


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
        PlugUtils.checkName(httpFile, content, "\">\n" + "        <span>", "</span>");
        PlugUtils.checkFileSize(httpFile, content, "\u0420\u0430\u0437\u043C\u0435\u0440: <b>", "</b>");

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        //GREAT THANKS TO @ntoskrnl for his help!!!
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());

            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("\u0421\u043A\u0430\u0447\u0430\u0442\u044C \u0444\u0430\u0439\u043B \u0431\u0435\u0441\u043F\u043B\u0430\u0442\u043D\u043E").toGetMethod();

            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException();
            }
            String url_to_download = PlugUtils.getStringBetween(getContentAsString(), "href=\"", "\" id=\"dl_link");
            httpMethod = getMethodBuilder().setAction(url_to_download).toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    protected String getBaseURL() {
        return "http://fileshare.in.ua";
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("\u0424\u0430\u0439\u043B \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D")) {
            throw new URLNotAvailableAnymoreException("Url is not avaible any more");
        }

        if (contentAsString.contains("\u0422\u0430\u043A\u043E\u0439 \u0441\u0442\u0440\u0430\u043D\u0438\u0446\u044B \u043D\u0430 \u043D\u0430\u0448\u0435\u043C \u0441\u0430\u0439\u0442\u0435 \u043D\u0435\u0442")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }

        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}
