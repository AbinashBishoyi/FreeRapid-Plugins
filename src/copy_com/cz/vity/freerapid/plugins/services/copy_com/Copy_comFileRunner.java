package cz.vity.freerapid.plugins.services.copy_com;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Copy_comFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Copy_comFileRunner.class.getName());

    private String fileData;
    private boolean folder = false;

    @Override
    public void runCheck() throws Exception { //this method validates file
        checkUrl();
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkUrl() {
        if (!fileURL.contains("copy.com/s/"))
            fileURL = fileURL.replaceFirst("copy\\.com/", "copy.com/s/");
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher matchData = PlugUtils.matcher("(?s)Browser.Models.Obj.findOrCreate\\(\\{(.+?)\\}\\);", content);
        fileData = "";
        while (matchData.find())
            fileData = matchData.group(1);
        if (fileData.equals(""))
            throw new PluginImplementationException("File information not found");
        final Matcher matchName = PlugUtils.matcher("\"name\":\"(.+?)\",\"type\":\"(.+?)\"", fileData.substring(0, fileData.indexOf("\"children\":[")));
        if (!matchName.find()) {
            folder = true; //root folder
            final String name = PlugUtils.getStringBetween(fileData, "\"id\":\"", "\"");
            httpFile.setFileName(name.substring(name.lastIndexOf("/") + 1));
        } else {
            httpFile.setFileName(matchName.group(1));
            if (matchName.group(2).equals("dir"))
                folder = true;
        }
        if (folder) {
            httpFile.setFileName("Folder: " + httpFile.getFileName());
            PlugUtils.checkFileSize(httpFile, fileData, "\"children_count\":", "}");
        } else {
            PlugUtils.checkFileSize(httpFile, fileData, "\"size\":", ",");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        checkUrl();
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);//extract file name and size from the page
            if (folder) {
                List<URI> list = new LinkedList<URI>();
                final String childData = fileData.substring(fileData.indexOf("\"children\":[{"), fileData.indexOf("}],"));
                final Matcher matchChildren = PlugUtils.matcher("\"url\":\"(.+?)\",", childData);
                while (matchChildren.find()) {
                    list.add(new URI(matchChildren.group(1).replace("\\/", "/")));
                }
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            } else {
                final String dlUrl = PlugUtils.getStringBetween(fileData, "\"url\":\"", "\",").replace("\\/", "/");
                final HttpMethod httpMethod = getGetMethod(dlUrl + "?download=1");
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("message\":\"Cannot find")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}