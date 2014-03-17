package cz.vity.freerapid.plugins.services.kewlshare;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 */
class KewlshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(KewlshareRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            String checkcontent = getContentAsString();
            checkNameandSize(checkcontent);
        } else {
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethoda = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethoda)) {
            String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);
            if (contentAsString.contains("name=\"selection")) {  //Confirm 1st Page
                client.setReferer(fileURL);
                final PostMethod page1 = getPostMethod(fileURL);
                String[] parameters = new String[]{"nL", "cL", "selection"};
                PlugUtils.addParameters(page1, contentAsString, parameters);
                if (makeRequest(page1)) { //Request page 2
                    contentAsString = getContentAsString();
                    if (contentAsString.contains("Please wait until the download begins")) { //Confirming Page 2
                        downloadTask.sleep(5);
                        client.setReferer(fileURL);
                        final PostMethod page2 = getPostMethod(fileURL);
                        String[] parameters2 = new String[]{"nL", "cL", "act"};
                        PlugUtils.addParameters(page2, contentAsString, parameters2);
                        if (makeRequest(page2)) { //request page 3
                            contentAsString = getContentAsString();
                            if (contentAsString.contains("Download Now")) {  //Confirm page 3
                                Matcher matcher = getMatcherAgainstContent("<form action=\"([^\"]+)");  //Find Action
                                if (matcher.find()) { //find form action
                                    final PostMethod page3 = getPostMethod(matcher.group(1));
                                    if (!tryDownloadAndSaveFile(page3)) {
                                        checkProblems();
                                        logger.warning(getContentAsString());//something was really wrong, we will explore it from the logs :-)
                                        throw new IOException("File input stream is empty.");
                                    }
                                } else {
                                    throw new InvalidURLOrServiceProblemException("Can't find Page 3 Action");
                                }
                            } else {
                                throw new InvalidURLOrServiceProblemException("Can't find Page 3");
                            }
                        } else {
                            throw new InvalidURLOrServiceProblemException("Can't load Page 3");
                        }
                    } else {  //Can't find 2nd page
                        checkProblems();
                        throw new InvalidURLOrServiceProblemException("Can't find 2nd page");
                    }
                }
            } else {
                throw new InvalidURLOrServiceProblemException("Failed to requst Page 2"); //Can't request 2nd page
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Can't find 1st Page"); //Can't find first page
        }
    }


    private void checkNameandSize(String content) throws Exception {

        if (!content.contains("kewlshare.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("File doesn")) {
            throw new InvalidURLOrServiceProblemException("<b>Kewlshare error:</b><br>File doesn't exist");
        }
        Matcher xmatcher = PlugUtils.matcher("<!-- <title>: ([^<]+)", content);
        if (xmatcher.find()) {
            final String fileName = xmatcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

        } else {
            logger.warning("File name was not found" + content);
        }

        Matcher smatcher = PlugUtils.matcher("(([0-9.]* .B))<", content);
        if (smatcher.find()) {
            final String fileSize = smatcher.group(1);
            logger.info("File size " + fileSize);
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));

        } else {
            logger.warning("File size was not found" + content);
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>Kewlshare Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>Kewlshare Error:</b><br>Currently a lot of users are downloading files."));
        }
        if (getContentAsString().contains("We are forced to limit the access of this file")) {
            throw new ServiceConnectionProblemException(String.format("<b>Kewlshare Error:</b><br>Currently a lot of users are downloading files."));
        }
        // We are forced to limit the access of this file
    }

}