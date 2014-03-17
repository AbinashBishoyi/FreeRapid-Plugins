package cz.vity.freerapid.plugins.services.nahraj;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.io.IOException;

/**
 * @author Kajda
 */
class NahrajFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NahrajFileRunner.class.getName());
    
    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }
    
    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);
        
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
            
            Matcher matcher = getMatcherAgainstContent("enctype=\"multipart/form-data\" action=\"(.+?)\"");
            
            if (matcher.find()) {
                String finalURL = matcher.group(1);
                client.setReferer(finalURL);
                //client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain");
                getMethod = getGetMethod(finalURL);
                
                if (!tryDownloadAndSaveFile(getMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty");
                }
            } else {
                throw new PluginImplementationException("Download link was not found");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("Neznam. soubor");
        
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("Neznamý soubor");
        }

        matcher = getMatcherAgainstContent("V.cen.sobn. download");

        if (matcher.find()) {
            throw new YouHaveToWaitException("Vícenásobný download", 60);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("class=\"title\">(.+?)<");
        
        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);
            
            matcher = getMatcherAgainstContent("class=\"size\">(.+?)<");
            
            if (matcher.find()) {
                final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + fileSize);
                httpFile.setFileSize(fileSize);
            } else {
                logger.warning("File size was not found");
                throw new PluginImplementationException();
            }
        } else {
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }
        
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}
