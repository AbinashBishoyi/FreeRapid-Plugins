package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginContext;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 */
class RapidShareRunner extends AbstractRunner {
    
    private final static Logger logger = Logger.getLogger(RapidShareRunner.class.getName());
    ConfigurationStorageSupport storage;
    PluginContext context ;
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            enterCheck();
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    public void run() throws Exception {
        super.run();
 //       storage = getPluginService().getPluginContext().getConfigurationStorageSupport();
       context =  getPluginService().getPluginContext();
 
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            enterCheck();
            Matcher matcher = getMatcherAgainstContent("form id=\"ff\" action=\"([^\"]*)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
            String s = matcher.group(1);
            logger.info("Found File URL - " + s);
            client.setReferer(fileURL);
            final PostMethod postMethod = getPostMethod(s);
            postMethod.addParameter("dl.start", "Free");
            if (makeRequest(postMethod)) {
                matcher = getMatcherAgainstContent("var c=([0-9]+);");
                if (!matcher.find()) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
                }
                s = matcher.group(1);

                int seconds = new Integer(s);
                matcher = getMatcherAgainstContent("form name=\"dlf\" action=\"([^\"]*)\"");
                if (matcher.find()) {
                    s = matcher.group(1);
                    logger.info("Download URL: " + s);
                  String myUrl =  getPrefferedMirror();
               if(!"".equals(myUrl)) s=myUrl; 
                    downloadTask.sleep(seconds + 1);
                    final PostMethod method = getPostMethod(s);
                    method.addParameter("mirror", "on");
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new IOException("File input stream is empty.");
                    }
                } else {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                }
            }
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private String getPrefferedMirror() throws Exception{
       RapidShareServiceImpl service = (RapidShareServiceImpl) getPluginService();
      RapidShareMirrorConfig config = service.getConfig();
       MirrorChooser chooser = new MirrorChooser(context,config);
        chooser.setContent(getContentAsString());
        return chooser.getPreferredURL(getContentAsString());
    }

    private void enterCheck() throws URLNotAvailableAnymoreException, InvalidURLOrServiceProblemException {
        Matcher matcher;
        if (!getContentAsString().contains("form id=\"ff\" action=")) {

            matcher = getMatcherAgainstContent("class=\"klappbox\">((\\s|.)*?)</div>");
            if (matcher.find()) {
                final String error = matcher.group(1);
                if (error.contains("illegal content") || error.contains("file has been removed") || error.contains("has removed") || error.contains("file is neither allocated to") || error.contains("limit is reached"))
                    throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>" + error);
                if (error.contains("file could not be found"))
                    throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>" + error);
                logger.warning(getContentAsString());
                throw new InvalidURLOrServiceProblemException("<b>RapidShare error:</b><br>" + error);
            }
            if (getContentAsString().contains("has removed file"))
                throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>The uploader has removed this file from the server.");
            if (getContentAsString().contains("file could not be found"))
                throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>The file could not be found. Please check the download link.");
            if (getContentAsString().contains("illegal content"))
                throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>Illegal content. File was removed.");
            if (getContentAsString().contains("file has been removed"))
                throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>Due to a violation of our terms of use, the file has been removed from the server.");
            if (getContentAsString().contains("limit is reached"))
                throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>To download this file, the uploader either needs to transfer this file into his/her Collector's Account, or upload the file again. The file can later be moved to a Collector's Account. The uploader just needs to click the delete link of the file to get further information.");

            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        //| 5277 KB</font>
        matcher = getMatcherAgainstContent("\\| (.*? .B)</font>");
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }

    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException {
        Matcher matcher;//Your IP address XXXXXX is already downloading a file.  Please wait until the download is completed.
        if (getContentAsString().contains("You have reached the")) {
            matcher = getMatcherAgainstContent("try again in about ([0-9]+) minute");
            if (matcher.find()) {
                throw new YouHaveToWaitException("You have reached the download-limit for free-users.", Integer.parseInt(matcher.group(1)) * 60 + 10);
            }
            throw new ServiceConnectionProblemException("<b>RapidShare error:</b><br>You have reached the download-limit for free-users.");
        }
        matcher = getMatcherAgainstContent("IP address (.*?) is already");
        if (matcher.find()) {
            final String ip = matcher.group(1);
            throw new ServiceConnectionProblemException(String.format("<b>RapidShare error:</b><br>Your IP address %s is already downloading a file. <br>Please wait until the download is completed.", ip));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            matcher = getMatcherAgainstContent("Please try again in ([0-9]+) minute");
            if (matcher.find()) {
                throw new YouHaveToWaitException("Currently a lot of users are downloading files.", Integer.parseInt(matcher.group(1)) * 60 + 20);
            }
            throw new ServiceConnectionProblemException("<b>RapidShare error:</b><br>Currently a lot of users are downloading files.");
        }
    }

}

