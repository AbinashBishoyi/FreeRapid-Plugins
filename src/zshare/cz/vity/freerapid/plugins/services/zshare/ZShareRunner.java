package cz.vity.freerapid.plugins.services.zshare;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Prathap
 */
class ZShareRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(ZShareRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = processURL(fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameandSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameandSize(String contentAsString) throws Exception {
        PlugUtils.checkName(httpFile, contentAsString, "<title>zSHARE -", "- Free File Hosting Service | Audio and Video Sharing | Image Uploading | Web storage</title>");
        PlugUtils.checkFileSize(httpFile, contentAsString, "File Size: <font color=\"#666666\">", "</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setClientParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        fileURL = processURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameandSize(getContentAsString());
            Matcher matcher = getMatcherAgainstContent("form name=\"form1\" method=\"post\" action=");
            if (!matcher.find()) {
                throw new PluginImplementationException();
            }
            String s = matcher.group(0);
            logger.info("Found Download button - " + s);
            final PostMethod postMethod = getPostMethod(fileURL);
            postMethod.addParameter("download", "1");
            if (makeRedirectedRequest(postMethod)) {
                matcher = getMatcherAgainstContent("var link_enc=new Array(.*)");
                if (matcher.find()) {
                    String link = matcher.group(1);
                    link = this.processDownloadLink(link);
                    if (!link.equals("")) {
                        logger.info("Download URL: " + link);
                        final GetMethod method = getGetMethod(link);
                        downloadTask.sleep(50);
                        if (!tryDownloadAndSaveFile(method)) {
                            checkProblems();
                            throw new IOException("File input stream is empty.");
                        }
                    } else throw new PluginImplementationException();
                } else {
                    throw new PluginImplementationException();
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String processURL(String mURL) throws Exception {
        String tURL = mURL;
        if (tURL.contains("www.zshare.net/audio")) {
            tURL = tURL.replaceFirst("www.zshare.net/audio", "www.zshare.net/download");

        } else if (tURL.contains("www.zshare.net/image")) {
            tURL = tURL.replaceFirst("www.zshare.net/image", "www.zshare.net/download");

        } else if (tURL.contains("www.zshare.net/audio")) {
            tURL = tURL.replaceFirst("www.zshare.net/audio", "www.zshare.net/download");

        } else if (tURL.contains("www.zshare.net/video")) {
            tURL = tURL.replaceFirst("www.zshare.net/video", "www.zshare.net/download");

        } else if (tURL.contains("www.zshare.net/downloadlink")) {
            tURL = tURL.replaceFirst("www.zshare.net/downloadlink", "www.zshare.net/download");

        } else if (tURL.contains("www.zshare.net/download/")) {
            tURL = tURL.replaceFirst("www.zshare.net/download/", "www.zshare.net/download/");
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL");
        }
        return tURL;
    }

    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException {
        if (getContentAsString().toLowerCase().contains("file not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));
        }
    }

    private String processDownloadLink(String link) throws Exception {
        String tmp = "";
        if (link != null) {
            for (int i = 0; i < link.length(); i++) {
                String chr = link.charAt(i) + "";
                if (chr.equals(";")) {
                    break;
                }
                //replace all the special characters
                if (!chr.equals(",") && !chr.equals("'") && !chr.equals("(") &&
                        !chr.equals(")") /*|| !chr.equals("%")*/) {
                    tmp += link.charAt(i);
                }
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Cant find download link");
        }
        return tmp;
    }

}
