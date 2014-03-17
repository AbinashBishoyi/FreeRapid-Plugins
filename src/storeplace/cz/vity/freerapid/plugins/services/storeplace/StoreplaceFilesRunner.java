package cz.vity.freerapid.plugins.services.storeplace;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.regex.Matcher;

/**
 * @author RickCL
 */
class StoreplaceFilesRunner extends AbstractRunner {
    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        try {
            PlugUtils.checkFileSize(httpFile, content, "<td align=left><b>File size:</b></td>\n       <td align=left>", "</td>");
            PlugUtils.checkName(httpFile, content, "<td align=left width=350px>", "</td>");
        } catch(PluginImplementationException e) {
            checkProblems("checkNameAndSize");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());

            Matcher matcher = getMatcherAgainstContent("var timeout='([\\d]+)'");
            int wait=15;
            if( matcher.find() ) {
                wait = Integer.parseInt(matcher.group(1)) + 1;
            }
            downloadTask.sleep(wait);

            final HttpMethod httpMethod = getMethodBuilder().setActionFromAHrefWhereATagContains("Right click").toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems("Download Fail !!");
            }

        } else
            throw new PluginImplementationException();
    }

    private void checkProblems(String step) throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException, PluginImplementationException {
        String content = getContentAsString();
        if (content.contains("\u0421 \u0432\u0430\u0448\u0435\u0433\u043e IP \u0430\u0434\u0440\u0435\u0441\u0430")) {
            throw new ServiceConnectionProblemException(String.format("<b>Your IP is already downloading a file from our system.</b><br>You cannot download more than one file in parallel."));
        }
        if (content.contains("Your requested file is not found")) {
            throw new URLNotAvailableAnymoreException(String.format("Your requested file is not found."));
        }
        /** For debug
        try {
            FileOutputStream f = new FileOutputStream("error-content.html");
            f.write( content.getBytes() );
            f.close();
        } catch(Exception e) {}
        /**/
        throw new PluginImplementationException("Step " + step);
    }

}
