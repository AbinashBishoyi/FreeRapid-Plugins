package cz.vity.freerapid.plugins.services.filesmail;

import cz.vity.freerapid.plugins.container.FileInfo;
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

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FilesMailFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesMailFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        if (fileURL.contains("attachmail")) return;
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("<th class=\"do\">")) { // multiple files listed
            httpFile.setFileName("List: " + PlugUtils.getStringBetween(content, "<title>", "скачать ") + " ...");
        } else {
            PlugUtils.checkName(httpFile, content, "<span title=\"", "\">");
            String size = ru2enFileSize(PlugUtils.getStringBetween(content, "<td title=\"", "\">"));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(size));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private String ru2enFileSize(final String size) {
        return size.replaceAll("(\u0411|\u0431)", "B").replace("\u041A", "K").replace("\u043C", "M").replace("\u0413", "G").replace("\uFFFD\uFFFD", "MB");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (fileURL.contains("attachmail")) {
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
            return;
        }
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            if (getContentAsString().contains("<th class=\"do\">")) { // multiple files listed
                final List<FileInfo> list = new LinkedList<FileInfo>();
                final Matcher matchName = PlugUtils.matcher("<div class=\"str\">(.+?)</div>", contentAsString);
                final Matcher matchSize = PlugUtils.matcher("<td>(.+?)</td>\\s+?<td class=\"do\">", contentAsString);
                final Matcher matchLink = PlugUtils.matcher("<a href=\"(.+?)\".+?title=\"\">скачать</a>", contentAsString);
                while (matchName.find() & matchSize.find() & matchLink.find()) {
                    logger.info("Found: " + matchName.group(1) + "\t\t" + matchSize.group(1) + "\t\t" + matchLink.group(1));
                    final FileInfo fileInfo = new FileInfo(new URL(matchLink.group(1)));
                    fileInfo.setFileName(matchName.group(1));
                    fileInfo.setFileSize(PlugUtils.getFileSizeFromString(ru2enFileSize(matchSize.group(1))));
                    list.add(fileInfo);
                }
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueueFromContainer(httpFile, list);
                httpFile.setState(DownloadState.COMPLETED);
                return;
            }
            Matcher match = PlugUtils.matcher("<a href=\"(.+?)\".+?but_color_red.+?>", contentAsString);
            if (!match.find())
                throw new PluginImplementationException("Download link not found");
            final HttpMethod httpMethod = getMethodBuilder().setAction(match.group(1)).toHttpMethod();
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
        if (contentAsString.contains("The page you requested is not available") ||
                contentAsString.contains("Page cannot be displayed") ||
                contentAsString.contains("Не найдено файлов")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("files.mail.ru/faq#dnl_limits")) {
            throw new ErrorDuringDownloadingException("Maximum parallel download limit reached");
        }
        if (contentAsString.contains("errorMessage")) {
            throw new ErrorDuringDownloadingException("Error message received from site");
        }
    }

}