package cz.vity.freerapid.plugins.services.upnito;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 */
class UpnitoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UpnitoFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        //zov: <strong>nhl.09.part06.rar<br>
        Matcher matcher = PlugUtils.matcher("zov: <strong>(.*?)<", content);
        if (matcher.find()) {
            final String fileName = matcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);
            //: <strong>204800</strong>KB<br>
            matcher = PlugUtils.matcher(": <strong>([0-9]+)</strong>(.?B)<", content);
            if (matcher.find()) {
                final long size = PlugUtils.getFileSizeFromString(matcher.group(1) + matcher.group(2));
                httpFile.setFileSize(size);
            } else {
                checkProblems();
                logger.warning("File size was not found\n:");
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkNameAndSize(contentAsString);
            client.setReferer(fileURL);
            final PostMethod postMethod = getPostMethod(fileURL);
            PlugUtils.addParameters(postMethod, contentAsString, new String[]{"dl2", "verifytext", "sid", "auth_token"});

            postMethod.addParameter("file", "");
            postMethod.addParameter("userinput", "");
            postMethod.addParameter("validated", "yes");
            postMethod.addParameter("tahaj", "Stiahnu");


            if (!tryDownloadAndSaveFile(postMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty.");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("bol zmazan") || contentAsString.contains("bor sa na serveri nenach") || contentAsString.contains("bor nebol n")) {
            throw new URLNotAvailableAnymoreException("Súbor bol zmazaný");
        }

        if (contentAsString.contains("za sebou stahovat ten")) {
            throw new ServiceConnectionProblemException("Nemozete tolkokrat za sebou stahovat ten isty subor!");
        }

        if (contentAsString.contains("Neplatny download")) {
            throw new YouHaveToWaitException("Neplatny download", 2);
        }
    }

}
