package cz.vity.freerapid.plugins.services.indowebster;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex,JPEXS,zid,tong2shot
 */
class IndowebsterRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(IndowebsterRunner.class.getName());


    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method1 = getGetMethod(fileURL);
        if (makeRedirectedRequest(method1)) {
            checkProblems();
            fileURL = method1.getURI().toString();
            if (isPassworded()) {
                stepPasswordPage();
            }
            String contentAsString = getContentAsString();
            checkNameAndSize();
            final String secondURLRule = "<a href=\"(.*?)\" class=\"downloadBtn";
            Matcher secondURLRuleMatcher = PlugUtils.matcher(secondURLRule, contentAsString);
            secondURLRuleMatcher.find();
            final String secondURL = secondURLRuleMatcher.group(1);
            final HttpMethod method2 = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(secondURL)
                    .toGetMethod();
            setPageEncoding("utf-8");
            setClientParameter("X-Requested-With", "XMLHttpRequest");
            if (makeRedirectedRequest(method2)) {
                contentAsString = getContentAsString();
                final String filename = PlugUtils.getStringBetween(contentAsString, "<strong id=\"filename\">", "</strong>");
                httpFile.setFileName(filename);
                final int waitTime = PlugUtils.getNumberBetween(contentAsString, "var s = ", ";");
                downloadTask.sleep(waitTime);
                final String strAjax = PlugUtils.getStringBetween(contentAsString, "$.post('http://www.indowebster.com/ajax/downloads/gdl',{", "},function");
                final String ajaxRule = "([^,]*?):'(.*?)'";
                Matcher ajaxMatcher = PlugUtils.matcher(ajaxRule, strAjax);

                final PostMethod ajaxMethod = getPostMethod(PlugUtils.getStringBetween(contentAsString, "$.post('", "',{"));
                while (ajaxMatcher.find()) {
                    ajaxMethod.setParameter(ajaxMatcher.group(1), ajaxMatcher.group(2));
                }
                if (makeRequest(ajaxMethod)) {
                    contentAsString = getContentAsString();
                    final GetMethod method3 = getGetMethod(contentAsString.replace("[", "%5B").replace("]", "%5D"));
                    client.makeRequest(method3, false);
                    URI finalURI = new URI(method3.getResponseHeader("location").getValue().replace("[", "%5B").replace("]", "%5D"), true, method3.getParams().getUriCharset());
                    setFileStreamContentTypes("text/plain");
                    final HttpMethod finalMethod = getMethodBuilder()
                            .setAction(finalURI.toString())
                            .setReferer(fileURL)
                            .toGetMethod();
                    if (!tryDownloadAndSaveFile(finalMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                }
            }

        }
    }

    private void checkNameAndSize() throws Exception {
        final String content = getContentAsString();
        if (!isPassworded()) {
            PlugUtils.checkFileSize(httpFile, content, "Size:</strong>", "| Server:");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }

    private void checkProblems() throws ServiceConnectionProblemException, InvalidURLOrServiceProblemException, URLNotAvailableAnymoreException {
        final String contentAsString = getContentAsString();
        /*
        if (!content.contains("indowebster.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        */
        if (contentAsString.contains("Storage Maintenance, Back Later")) {
            throw new InvalidURLOrServiceProblemException("Storage Maintenance, Back Later");
        }
        if (contentAsString.contains("reported and removed")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Indowebster Error:</b><br>This files has been reported and removed due to terms of use violation"));
        }
        if (contentAsString.contains("File doesn")) {
            throw new URLNotAvailableAnymoreException("<b>Indowebster error:</b><br>File doesn't exist");
        }
        if (contentAsString.contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>Indowebster Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (contentAsString.contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>Indowebster Error:</b><br>Currently a lot of users are downloading files."));
        }
        if (contentAsString.contains("504 Gateway Time-out")) {
            throw new ServiceConnectionProblemException("Gateway Time-out");
        }
    }

    private boolean isPassworded() {
        return getContentAsString().contains("THIS FILE IS PASSWORD PROTECTED");
    }


    private void stepPasswordPage() throws Exception {
        while (isPassworded()) {
            String pwdparam = PlugUtils.getStringBetween(getContentAsString(), "<input type=\"password\" name=\"", "\" AUTOCOMPLETE = \"OFF\"");
            if (!pwdparam.equals("")) {
                PostMethod post1 = getPostMethod(fileURL);
                post1.addParameter(pwdparam, getPassword());
                logger.info("Posting password to url - " + fileURL);
                if (!makeRedirectedRequest(post1)) {
                    throw new ServiceConnectionProblemException();
                }
            } else throw new ServiceConnectionProblemException("Error posting password");
        }

    }

    private String getPassword() throws Exception {
        final String password = getDialogSupport().askForPassword("Indowebster");
        if (password == null) {
            throw new NotRecoverableDownloadException("This file is secured with a password");
        } else {
            return password;
        }
    }

}