package cz.vity.freerapid.plugins.services.kewlshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
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
 * @author Arthur Gunawan
 */
class KewlshareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(KewlshareFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            throw new PluginImplementationException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "File Name : <strong>", " || ");//TODO
        PlugUtils.checkFileSize(httpFile, content, " || ", "</strong>");//TODO
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
//            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            //final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("Download!", true).toHttpMethod();//TODO
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormByIndex(2, true).toHttpMethod();

            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't post first page");//some unknown problem
            }
            logger.info("First Post OK!");
            //logger.info(getContentAsString());

            httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormByIndex(1, true).toHttpMethod();
            downloadTask.sleep(5);

            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't post second page");//some unknown problem
            }
            logger.info("Second Post OK!");
            //logger.info(getContentAsString());

            httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormByIndex(1, true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't post third page");//some unknown problem
            }
            logger.info("Third Post OK!");
            //logger.info(getContentAsString());
            //<a href="http://face.kewlshare.com/dl/96a763754fa501430fbaf1b62feabc77ff83a3d93d702353106d6c9445ba2021727dbc0922e2f8c6a3f9850734ad5e7428e41bc9cdf24b624ec04a3ce64292429d5e0150f7229637d6df3163dbaf8efce009f0d7daa6defacb9dbe7ef68e9875f3121d1ff08a7316460e6e20a885fc5b/864a60b6779c00ae8fc2787d6402ba96/Cewe_amoi_cantik_kaya_bidadari.rar"> <span class="stylet">
            String finURL = PlugUtils.getStringBetween(getContentAsString(), "<a href=\"", "\"> <span class=\"stylet\">");
            method = getGetMethod(finURL);

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
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
