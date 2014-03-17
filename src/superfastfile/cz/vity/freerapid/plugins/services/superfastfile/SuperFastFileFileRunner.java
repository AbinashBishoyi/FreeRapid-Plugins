package cz.vity.freerapid.plugins.services.superfastfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class SuperFastFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SuperFastFileFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h3>", "</h3>");
        PlugUtils.checkFileSize(httpFile, content, "<h2>", "</h2>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final HttpMethod m = getMethodBuilder().setReferer(fileURL).setActionFromFormByIndex(1, true).setAction(fileURL).removeParameter("method_premium").toPostMethod();
            if (makeRequest(m)) {
                checkProblems();//check problems                
                final int sleep = PlugUtils.getNumberBetween(getContentAsString(), "\"countdown\">", "</span>");

                final Matcher matcher = getMatcherAgainstContent("padding-left: ?(\\d+)px; ?padding-top: ?\\d+px;'>(\\d)</span>");
                int start = 0;

                List<CaptchaEntry> list = new ArrayList<CaptchaEntry>(4);
                while (matcher.find(start)) {
                    list.add(new CaptchaEntry(matcher.group(1), matcher.group(2)));
                    start = matcher.end();
                }
                Collections.sort(list);
                StringBuilder builder = new StringBuilder();
                for (CaptchaEntry entry : list) {
                    builder.append(entry.value);
                }
                final String captcha = builder.toString();
                if (!captcha.isEmpty()) {
                    //   throw new PluginImplementationException("Captcha not found");
                    logger.info("Captcha:" + captcha);
                    logger.info("File url:" + fileURL);
                }
                final MethodBuilder methodBuilder = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("F1", true).setAction(fileURL);


                methodBuilder.setParameter("code", captcha);
                methodBuilder.setParameter("referer", fileURL);
                methodBuilder.setParameter("btn_download", "Sending File...");

                this.downloadTask.sleep(sleep + 1);

                //here is the download link extraction
//                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download file").toPostMethod();
                client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain");
                if (!tryDownloadAndSaveFile(methodBuilder.toPostMethod())) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }

            } else throw new PluginImplementationException("Main form not found");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("No such")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Wrong captcha")) {
            throw new PluginImplementationException("Wrong captcha");
        }
        final Matcher content = getMatcherAgainstContent("You have to wait (\\d+)");
        if (content.find()) {
            throw new YouHaveToWaitException(content.group(), Integer.parseInt(content.group(1)));
        }

    }

    private static class CaptchaEntry implements Comparable<CaptchaEntry> {
        private Integer position;
        String value;

        CaptchaEntry(String position, String value) {
            this.position = new Integer(position);
            this.value = value;
        }

        public int compareTo(CaptchaEntry o) {
            return position.compareTo(o.position);
        }
    }

}