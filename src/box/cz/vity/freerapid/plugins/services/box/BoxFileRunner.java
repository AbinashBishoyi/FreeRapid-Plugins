package cz.vity.freerapid.plugins.services.box;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class BoxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BoxFileRunner.class.getName());
    private String fileId;

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        fixUrl();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        int httpStatus = client.makeRequest(getMethod, false);
        if (httpStatus / 100 == 3) {
        } else if (httpStatus == 200) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void fixUrl() {
        fileURL = fileURL.replaceFirst("//box.com", "//app.box.com");
        fileURL = fileURL.replaceFirst("//www.box.com", "//app.box.com");
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (fileURL.matches("https?://app.box.com/s/.+?/\\d/\\d+?/\\d+?/\\d")) {
            final Matcher matchID = PlugUtils.matcher("https?://app.box.com/s/.+?/\\d/\\d+?/(\\d+?)/\\d", fileURL);
            if (!matchID.find()) throw new PluginImplementationException("File ID not found");
            fileId = matchID.group(1);
            PlugUtils.checkName(httpFile, content, fileId + "\\\" name=\\\"", "\\\"");
            final Matcher matchS = PlugUtils.matcher(Pattern.quote("item_size\\\">") + "([^<]+?)" + Pattern.quote("<\\/li><li><ul class=\\\"inline_list_ext\\\"><li id=\\\"link_info_") + fileId, content);
            if (!matchS.find()) throw new PluginImplementationException("File size not found");
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matchS.group(1)));
        } else {
            PlugUtils.checkName(httpFile, content, "title\" content=\"", "\"");
            if (content.contains("\"type\":\"folder\"")) {
                httpFile.setFileName("Folder: " + httpFile.getFileName());
                httpFile.setFileSize(PlugUtils.getNumberBetween(content, "\"items_count\":", ","));
            } else
                PlugUtils.checkFileSize(httpFile, content, ">(", ")</span>");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        fixUrl();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        int httpStatus = client.makeRequest(method, false);
        if (httpStatus / 100 == 3) {
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else if (httpStatus == 200) {
            if (fileURL.matches("https?://app.box.com/s/.+?/\\d/\\d+?/\\d+?/\\d")) {
            } else {
                if (getContentAsString().contains("\"type\":\"folder\"")) {
                    List<URI> list = new LinkedList<URI>();
                    final Matcher matchNodes = PlugUtils.matcher("\"url_for_page_link\":\"(.+?)\",", getContentAsString());
                    while (matchNodes.find()) {
                        list.add(new URI("https://app.box.com" + matchNodes.group(1).replace("\\/", "/")));
                    }
                    if (list.isEmpty()) throw new PluginImplementationException("No links found");
                    getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);

                    httpFile.setFileName("Link(s) Extracted !");
                    httpFile.setState(DownloadState.COMPLETED);
                    httpFile.getProperties().put("removeCompleted", true);
                    return;
                }
                fileId = PlugUtils.getStringBetween(getContentAsString(), "fileid=\"", "\"");
            }
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);
            final String sharedName = PlugUtils.getStringBetween(contentAsString, "shared_name=", "\"");
            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("https://app.box.com/index.php")
                    .setParameter("rm", "box_download_shared_file")
                    .setParameter("shared_name", sharedName)
                    .setParameter("file_id", "f_" + fileId)
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("error_message_not_found") ||
                contentAsString.contains("This shared file or folder link has been removed")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("The user hosting this content is out of bandwidth")) {
            throw new NotRecoverableDownloadException("The user hosting this content is out of bandwidth");
        }
    }

}