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
 * @author Vity
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
        PlugUtils.checkFileSize(httpFile, content, "Размер: <b>", "</b>");

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

            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Скачать файл бесплатно").toGetMethod();

            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException();
            }

            httpMethod = getMethodBuilder().setReferer(httpMethod.getURI().toString()).setActionFromIFrameSrcWhereTagContains(getMethod.getPath() + "?fr").toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException();
            }

            httpMethod = getMethodBuilder().setReferer(httpMethod.getURI().toString()).setActionFromAHrefWhereATagContains("Скачать файл").toGetMethod();

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
        if (contentAsString.contains("Файл не найден")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }

        if (contentAsString.contains("Такой страницы на нашем сайте нет")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }

        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }
}
