package cz.vity.freerapid.plugins.services.servupcoil;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
        final PostMethod postMethod = getPostMethod(fileURL);
        postMethod.addParameter("op", "download1");
        postMethod.addParameter("id", fileURL.split("/")[4]);
        postMethod.addParameter("method_free", "הורדה בחינם");
        if (makeRedirectedRequest(postMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<b>שם הקובץ:</b></td><td nowrap>", "</td></tr>");
        PlugUtils.checkFileSize(httpFile, content, "<small>(", ")</small>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final PostMethod method = getPostMethod(fileURL);
        method.addParameter("op", "download1");
        method.addParameter("id", fileURL.split("/")[4]);
        method.addParameter("method_free", "הורדה בחינם");
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            // The following section is for CAPTCHA breaking, probably the easiest in the world
            StringBuilder code = new StringBuilder();
            HashMap digits = new HashMap();
            Matcher matcher = PlugUtils.matcher("<span style='left:(.+?)px;padding-top:(.+?)px;position:absolute'>&#(.+?);</span>", contentAsString);
            while (matcher.find()) {
                //logger.info("Left = " + matcher.group(1) + ", digit= " + Integer.toString(Integer.parseInt(matcher.group(3)) - 48));
                digits.put(new Integer(matcher.group(1)), Integer.parseInt(matcher.group(3)) - 48);
            }

            ArrayList keys = new ArrayList();
            keys.addAll(digits.keySet());
            Collections.sort(keys);
            Iterator i = keys.iterator();
            while (i.hasNext())
                code.append(digits.get(i.next()));

            logger.info("Supposedly CAPTCHA is " + code.toString());

            final HttpMethod httpMethod = getMethodBuilder().setActionFromFormByName("F1", true).
                    setParameter("code", code.toString()).setAction(fileURL).toPostMethod();

            downloadTask.sleep(45); //Needed, the server won't serve the file before

            makeRequest(httpMethod);
            checkProblems();

            if (httpMethod.getStatusCode() / 100 != 3)
                throw new URLNotAvailableAnymoreException(String.format("Unexpected status line: %s", httpMethod.getStatusLine()));

            /* Workaround, inspired by "freakshare" plugin. The problem is that last request is redirected
             * and file encoding is gzip. This workaround seperates requests and doesn't allow gzip encoding. 
             */
            final HttpMethod httpMethod2 = getGetMethod(httpMethod.getResponseHeader("Location").getValue());
            httpMethod2.setRequestHeader("Accept-Encoding", "");
            client.getHTTPClient().getParams().setParameter("considerAsStream", "");

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod2)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
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
            Matcher matcher = PlugUtils.matcher("<p class=\"err\">You have to wait (.+?) דקות, (.+?) שניות till next download", contentAsString);
            matcher.find();
            throw new YouHaveToWaitException("You have to wait ", Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2)));
        } else if (contentAsString.contains("<p class=\"err\">Wrong captcha</p>")) {
            throw new YouHaveToWaitException("Wrong captcha, try again...", 5);
        } else if (contentAsString.contains("You have reached the download-limit")) {
            throw new YouHaveToWaitException("Download limit is reached for current day", 60 * 60 * 24);
        }
    }

}