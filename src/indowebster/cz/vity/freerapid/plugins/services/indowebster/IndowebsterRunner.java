package cz.vity.freerapid.plugins.services.indowebster;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author zid
 */
class IndowebsterRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(IndowebsterRunner.class.getName());

    /*
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
    */

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        makeRedirectedRequest(getMethod);
        /*
        if (isPassworded()) {
            stepPasswordPage();
        }
        */
        checkNameandSize(getContentAsString());
    }

    @Override
    public void run() throws Exception {

        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method1 = getGetMethod(fileURL);

        if (makeRedirectedRequest(method1)) {
            String contentAsString = getContentAsString();
            checkProblems();
            checkNameandSize(contentAsString);

            final String secondURLRule = "<a href=\"(.*?)\" class=\"downloadBtn";
            Matcher secondURLRuleMatcher = PlugUtils.matcher(secondURLRule, contentAsString);
            secondURLRuleMatcher.find();
            final String secondURL = secondURLRuleMatcher.group(1);

            final HttpMethod method2 = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(secondURL)
                    .toGetMethod();

            client.getHTTPClient().getParams().setHttpElementCharset("utf-8");
            client.getHTTPClient().getParams().setParameter("pageCharset", "utf-8");
            client.getHTTPClient().getParams().setParameter("X-Requested-With", "XMLHttpRequest");
            logger.info("preparing method2... : " + secondURL);
            if (makeRedirectedRequest(method2)) {
                contentAsString = getContentAsString();
                final String filename = PlugUtils.getStringBetween(contentAsString, "<strong id=\"filename\">", "</strong>");
                httpFile.setFileName(filename);
                final int waitTime = PlugUtils.getNumberBetween(contentAsString, "var s = ", ";");
                downloadTask.sleep(waitTime);

                //processing ajax request
                final String strAjax = PlugUtils.getStringBetween(contentAsString, "$.post('http://www.indowebster.com/ajax/downloads/gdl',{", "},function");
                final String ajaxRule = "([^,]*?):'(.*?)'";
                Matcher ajaxMatcher = PlugUtils.matcher(ajaxRule, strAjax);

                //logger.info(strAjax);

                final PostMethod ajaxMethod = getPostMethod(PlugUtils.getStringBetween(contentAsString, "$.post('", "',{"));
                while (ajaxMatcher.find()) {
                    ajaxMethod.setParameter(ajaxMatcher.group(1), ajaxMatcher.group(2));
                    //logger.info(ajaxMatcher.group(1) + " : " + ajaxMatcher.group(2));
                }

                logger.info("making ajax request ...");
                if (makeRequest(ajaxMethod)) {
                    contentAsString = getContentAsString();
                    logger.info("Ajax respond : " + contentAsString);

                    final GetMethod method3 = getGetMethod(contentAsString.replace("[", "%5B").replace("]", "%5D"));
                    client.makeRequest(method3, false);
                    logger.info(method3.getResponseHeader("location").getValue());
                    URI finalURI = new URI(method3.getResponseHeader("location").getValue().replace("[", "%5B").replace("]", "%5D"), true, method3.getParams().getUriCharset());
                    
                    client.getHTTPClient().getParams().setParameter(DownloadClientConsts.CONSIDER_AS_STREAM,"text/plain");

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

    /*
    private boolean isPassworded() {
        return getContentAsString().contains("THIS FILE IS PASSWORD PROTECTED");
    }
    */

    private void checkNameandSize(String content) throws Exception {

        PlugUtils.checkFileSize(httpFile, content, "Size:</strong> ", " | Server:");
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

    /*
    private String getPassword() throws Exception {
        IndowebsterPasswordUI ps = new IndowebsterPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on Indowebster")) {
            return (ps.getPassword());
        } else throw new NotRecoverableDownloadException("This file is secured with a password");
    }
    */

}