package cz.vity.freerapid.plugins.services.indowebster;

import cz.vity.freerapid.plugins.exceptions.*;
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
 * @author Alex, JPEXS, zid, tong2shot
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
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            fileURL = method.getURI().toString();
            if (isPassworded()) {
                stepPasswordPage();
            }
            checkNameAndSize();
            Matcher matcher = getMatcherAgainstContent("<a href=\"(.*?)\" class=\"downloadBtn");
            if (!matcher.find()) {
                checkProblems();
                throw new PluginImplementationException("Tautan halaman kedua tidak ditemukan");
            }
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(matcher.group(1))
                    .toGetMethod();
            setPageEncoding("utf-8");
            httpMethod.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String filename = PlugUtils.getStringBetween(getContentAsString(), "<strong id=\"filename\">", "</strong>");
            final int waitTime = PlugUtils.getNumberBetween(getContentAsString(), "var s = ", ";");
            final PostMethod ajaxMethod = getPostMethod(PlugUtils.getStringBetween(getContentAsString(), "$.post('", "',{"));
            httpFile.setFileName(filename.replace("[www.indowebster.com]", "").replace("[files.indowebster.com]", ""));
            matcher = getMatcherAgainstContent("\\$\\.post\\('http://(?:.+?\\.)?indowebster\\.com/ajax/downloads/gdl',\\{(.+?)\\},function");
            if (!matcher.find()) {
                throw new PluginImplementationException("Ajax download params not found");
            }
            final String ajaxDownloadParams = matcher.group(1);
            matcher = PlugUtils.matcher("([^,]*?):'(.*?)'", ajaxDownloadParams);
            while (matcher.find()) {
                ajaxMethod.setParameter(matcher.group(1), matcher.group(2));
            }
            ajaxMethod.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            downloadTask.sleep(waitTime);
            if (!makeRequest(ajaxMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(getContentAsString().replace("[", "%5B").replace("]", "%5D"))
                    .toGetMethod();
            setFileStreamContentTypes("text/plain");
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Terjadi kesalahan di proses pengunduhan");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
        for (int i = 0; i < 3; i++) { //give it a couple of more tries if error occurs
            downloadTask.sleep(6);
            try {
                if (super.tryDownloadAndSaveFile(getMethodBuilder().setReferer(fileURL).setAction(method.getURI().toString()).toGetMethod())) {  //"cloning" method, to prevent method being aborted
                    return true;
                }
            } catch (org.apache.commons.httpclient.InvalidRedirectLocationException e) {
                //they use "[" and "]" chars in redirect url, we have to replace it.
                client.makeRequest(method, false);
                if (method.getResponseHeader("location") == null) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Download link not found");
                }
                final URI finalURI = new URI(method.getResponseHeader("location").getValue().replace("[", "%5B").replace("]", "%5D"), true, method.getParams().getUriCharset());
                HttpMethod method2 = getMethodBuilder()
                        .setAction(finalURI.toString())
                        .setReferer(fileURL)
                        .toGetMethod();
                if (super.tryDownloadAndSaveFile(method2)) {
                    return true;
                }
            }
            logger.warning(method.getURI().toString());
            logger.warning(getContentAsString());
        }
        return false;
    }

    private void checkNameAndSize() throws Exception {
        final String content = getContentAsString();
        if (!isPassworded()) {
            PlugUtils.checkFileSize(httpFile, content, "Size:</strong>", "| Server:");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }

    private void checkProblems() throws Exception {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Storage Maintenance, Back Later")) {
            throw new YouHaveToWaitException("Storage Maintenance, Back Later", 15 * 60);
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
        if (contentAsString.contains("502 Bad Gateway")) {
            throw new ServiceConnectionProblemException("Bad Gateway");
        }
        if (contentAsString.contains("di curi si Buaya Jahat") || contentAsString.contains("Web Server may be down")) {
            throw new YouHaveToWaitException("Server sibuk, tunggu beberapa saat..", 5 * 60);
        }
        if (contentAsString.contains("File upload in progress")) {
            throw new YouHaveToWaitException("File upload in progress", 15 * 60);
        }
    }

    private boolean isPassworded() {
        return getContentAsString().contains("THIS FILE IS PASSWORD PROTECTED");
    }


    private void stepPasswordPage() throws Exception {
        while (isPassworded()) {
            String pwdparam = PlugUtils.getStringBetween(getContentAsString(), "<input type=\"password\" name=\"", "\" AUTOCOMPLETE = \"OFF\"");
            if (!pwdparam.equals("")) {
                final PostMethod postMethod = getPostMethod(fileURL);
                postMethod.addParameter(pwdparam, getPassword());
                logger.info("Posting password to url - " + fileURL);
                if (!makeRedirectedRequest(postMethod)) {
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