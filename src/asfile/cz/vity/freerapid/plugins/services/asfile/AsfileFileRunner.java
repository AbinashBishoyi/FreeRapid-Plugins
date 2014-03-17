package cz.vity.freerapid.plugins.services.asfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;


/**
 * Class which contains main code
 *
 * @author RickCL
 */
class AsfileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AsfileFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "Download: <strong>", "</strong>");
        PlugUtils.checkFileSize(httpFile, content, "<br/> (", ")");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    @Override
    public void run() throws Exception {
        super.run();

        GetMethod getMethod = getGetMethod(fileURL);
        if(!makeRedirectedRequest(getMethod)) {
            throw new ServiceConnectionProblemException();
        }

        HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains("Regular download")
                        .toGetMethod();
        if(!makeRedirectedRequest(httpMethod)) {
            throw new ServiceConnectionProblemException();
        }
        int wait = 160;
        try {
            String sWait = PlugUtils.getStringBetween(getContentAsString(), "Waiting, please", "seconds");
            sWait = sWait.replaceAll("<[^<>]+>", "").trim();
            wait = Integer.parseInt(sWait);
        } catch (NumberFormatException ignored) {}
        downloadTask.sleep(wait + 1);
        //http://asfile.com/en/index/convertHashToLink
        //hash=7de06d35bcede0fc45a03686233589ed  path=Lw27o6I  storage=s13.asfile.com  name=13032303.rar
        final String hash = PlugUtils.getStringBetween(getContentAsString(), "hash: '", "'");
        final String path = PlugUtils.getStringBetween(getContentAsString(), "path: '", "'");
        final String storage = PlugUtils.getStringBetween(getContentAsString(), "storage: '", "'");
        final String name = PlugUtils.getStringBetween(getContentAsString(), "name: '", "'");
        httpMethod = getMethodBuilder().setReferer(fileURL)
                .setActionFromTextBetween("$.post('", "'")
                .setParameter("hash", hash)
                .setParameter("path", path)
                .setParameter("storage", storage)
                .setParameter("name", name)
                .toPostMethod();
        if(!makeRedirectedRequest(httpMethod)) {
            throw new ServiceConnectionProblemException();
        }
        final String downloadURL = PlugUtils.getStringBetween(getContentAsString(), "{\"url\":\"", "\"").replaceAll("\\\\/", "/");
        logger.info( downloadURL );

        httpMethod = getMethodBuilder().setReferer(fileURL)
                .setAction(downloadURL)
                .toGetMethod();
        if (!tryDownloadAndSaveFile(httpMethod)) {
            throw new ServiceConnectionProblemException("Error starting download:" + downloadURL);
        }

    }

}