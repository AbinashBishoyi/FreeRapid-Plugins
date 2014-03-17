package cz.vity.freerapid.plugins.services.servupcoil;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Frishrash
 */
class ServUpCoIlFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ServUpCoIlFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie(".servup.co.il", "lang", "english", "/", 86400, false));
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(fileURL)
                .setParameter("op", "download1")
                .setParameter("id", fileURL.split("/")[4])
                .setParameter("method_free", "Free+Download")
                .toPostMethod();
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<b>Filename:</b></td><td nowrap>", "</td></tr>");
        PlugUtils.checkFileSize(httpFile, content, "<small>(", ")</small>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".servup.co.il", "lang", "english", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(fileURL)
                .setParameter("op", "download1")
                .setParameter("id", fileURL.split("/")[4])
                .setParameter("method_free", "Free+Download")
                .toPostMethod();
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            // The following section is for CAPTCHA breaking, probably the easiest in the world
            StringBuilder code = new StringBuilder();
            Map<Integer, Character> digits = new TreeMap<Integer, Character>();
            Matcher matcher = PlugUtils.matcher("<span style='left:(\\d+?)px;padding-top:(.+?)px;position:absolute'>&#(\\d+?);</span>", contentAsString);
            while (matcher.find()) {
                digits.put(Integer.valueOf(matcher.group(1)), (char) Integer.parseInt(matcher.group(3)));
            }

            for (Character c : digits.values()) {
                code.append(c);
            }

            logger.info("Supposedly CAPTCHA is " + code.toString());

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("F1", true)
                    .setParameter("code", code.toString()).setAction(fileURL).toPostMethod();

            matcher = getMatcherAgainstContent("Wait <span id=\".+?\">(\\d+?)</span> seconds");
            if (!matcher.find()) throw new PluginImplementationException("Waiting time not found");
            downloadTask.sleep(Integer.parseInt(matcher.group(1)) + 1); //Needed, the server won't serve the file before

            makeRequest(httpMethod);
            checkProblems();

            if (httpMethod.getStatusCode() / 100 != 3) {
                checkProblems();
                throw new ServiceConnectionProblemException("Unexpected status line: " + httpMethod.getStatusLine());
            }

            /*
             * Workaround, inspired by "freakshare" plugin. The problem is that last request is redirected
             * and file encoding is gzip. This workaround seperates requests and doesn't allow gzip encoding. 
             */
            final HttpMethod httpMethod2 = getMethodBuilder().setReferer(fileURL).setAction(httpMethod.getResponseHeader("Location").getValue()).toGetMethod();
            httpMethod2.setRequestHeader("Accept-Encoding", "");
            client.getHTTPClient().getParams().setParameter("considerAsStream", "");

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod2)) {
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
        if (contentAsString.contains("<font class=\"err\">No such file")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        } else if (contentAsString.contains("<p class=\"err\">You have to wait")) {
            Matcher matcher = getMatcherAgainstContent("<p class=\"err\">You have to wait (\\d+?).+?(\\d+?).+?till next download");
            if (matcher.find()) {
                throw new YouHaveToWaitException("You have to wait between downloads", Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2)));
            } else {
                throw new PluginImplementationException("You have to wait between downloads (waiting time not found)");
            }
        } else if (contentAsString.contains("<p class=\"err\">Wrong captcha</p>")) {
            throw new YouHaveToWaitException("Wrong captcha, try again...", 5);
        } else if (contentAsString.contains("You have reached the download-limit")) {
            throw new YouHaveToWaitException("Download limit is reached for current day", 60 * 60 * 24);
        }
    }

}