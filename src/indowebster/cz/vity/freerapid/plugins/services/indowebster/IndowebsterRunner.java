package cz.vity.freerapid.plugins.services.indowebster;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhou_id
 */
class IndowebsterRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(IndowebsterRunner.class.getName());

    private void stepPasswordPage() throws Exception {
        while (getContentAsString().contains("<span class=\"medium\">INSERT PASSWORD</span>")) {
            String pwdparam = PlugUtils.getStringBetween(getContentAsString(), "<input type=\"password\" name=\"", "\" AUTOCOMPLETE = \"OFF\"");
            if (pwdparam != "")
            {
                PostMethod post1 = getPostMethod(fileURL);
                post1.addParameter(pwdparam, getPassword());
                logger.info("Posting password to url - " + fileURL);
                if (!makeRedirectedRequest(post1)) {
                    throw new PluginImplementationException();
                }
            }
            else throw new ServiceConnectionProblemException("Error posting password");
        }

    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        makeRequest(getMethod);
        if (isPassworded()) {
            stepPasswordPage();
        }
        checkNameandSize(getContentAsString());
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method1 = getGetMethod(fileURL);
        if (makeRedirectedRequest(method1)) {
            if (isPassworded()) {
                stepPasswordPage();
            }
            String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);
            Matcher matcher = getMatcherAgainstContent("<a id=\"download\" href=\"(http.*)\"><input type=\"button\" ");
            if (matcher.find()) {
                String secondUrl = matcher.group(1);
                final HttpMethod method2 = getMethodBuilder().setReferer(fileURL).setAction(secondUrl).toGetMethod();
                client.getHTTPClient().getParams().setHttpElementCharset("iso-8859-1");
                client.getHTTPClient().getParams().setParameter("pageCharset", "iso-8859-1");
                if (makeRedirectedRequest(method2)) {
                    String content = getContentAsString();
                    matcher = getMatcherAgainstContent("<p id=\"link-download\" align=\"center\"><a href=\"(http.*)\" class=\"dlm-h2\">Klik Disini untuk Mengunduh</a></p>");
                    if (matcher.find()) {
                        String thirdUrl = matcher.group(1);
                        final int waittime = PlugUtils.getNumberBetween(content, "document.counter.dl2.value='", "'");
                        downloadTask.sleep(waittime);
                        final HttpMethod method3 = getMethodBuilder().setReferer(secondUrl).setAction(thirdUrl).toGetMethod();
                        method3.setFollowRedirects(true);
                        if (!tryDownloadAndSaveFile(method3)) {
                            checkProblems();
                            logger.warning(getContentAsString());
                            throw new IOException("File input stream is empty.");
                        }
                    } else throw new InvalidURLOrServiceProblemException("Final link not found");
                } else throw new InvalidURLOrServiceProblemException("Cant get second url");
            } else throw new InvalidURLOrServiceProblemException("Cant find download link");
        }  else throw new InvalidURLOrServiceProblemException("Cant load first link");
    }

    private boolean isPassworded() {
        return getContentAsString().contains("THIS FILE IS PASSWORD PROTECTED");
    }

    private void checkNameandSize(String content) throws Exception {

        if (!content.contains("indowebster.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (getContentAsString().contains("Storage Maintenance, Back Later")) {
            throw new InvalidURLOrServiceProblemException("Storage Maintenance, Back Later");
        }
        if (getContentAsString().contains("reported and removed")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Indowebster Error:</b><br>This files has been reported and removed due to terms of use violation"));
        }
        if (content.contains("File doesn")) {
            throw new URLNotAvailableAnymoreException("<b>Indowebster error:</b><br>File doesn't exist");
        }
        PlugUtils.checkName(httpFile, content, "class=\"dl-title\" title=\"", "\">");
        PlugUtils.checkFileSize(httpFile, content, "Size : <span style=\"float:none;\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>Indowebster Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>Indowebster Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

    private String getPassword() throws Exception {
        IndowebsterPasswordUI ps = new IndowebsterPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on Indowebster")) {
            return (ps.getPassword());
        } else throw new NotRecoverableDownloadException("This file is secured with a password");
    }

}