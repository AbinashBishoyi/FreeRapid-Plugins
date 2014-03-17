package cz.vity.freerapid.plugins.services.upnito;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 */
class UpnitoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UpnitoFileRunner.class.getName());
    private String newUrl;


    private void setEncoding() {
        client.getHTTPClient().getParams().setParameter("pageCharset", "Windows-1250");
        client.getHTTPClient().getParams().setHttpElementCharset("Windows-1250");
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setEncoding();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            final GetMethod getMethod2 = getGetMethod(getURLPresmerovanie());
            client.setReferer(fileURL);
            if (makeRedirectedRequest(getMethod2)) {
                checkNameAndSize(getContentAsString());
            } else throw new PluginImplementationException();
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "bor:</strong>", "<br>");
        PlugUtils.checkFileSize(httpFile, content, "kos\u0165:</strong>", "<br>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setEncoding();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final GetMethod getMethod2 = getGetMethod(getURLPresmerovanie());
            client.setReferer(fileURL);
            if (!makeRedirectedRequest(getMethod2)) {
                throw new PluginImplementationException();
            }
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            client.setReferer(newUrl);
            final PostMethod postMethod = getPostMethod(newUrl);
            PlugUtils.addParameters(postMethod, contentAsString, new String[]{"dl2", "verifytext", "sid", "auth_token"});

            postMethod.addParameter("file", "");
            postMethod.addParameter("userinput", "");
            postMethod.addParameter("validated", "yes");
            postMethod.addParameter("tahaj", "Stiahnu�");

            //downloadTask.sleep(650);
            if (!tryDownloadAndSaveFile(postMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty.");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getURLPresmerovanie() throws PluginImplementationException {
        Matcher matcher = getMatcherAgainstContent("Prebieha\\s*<a href='([^']+)'>presmerovanie");
        if (matcher.find()) {
            newUrl = matcher.group(1);
            return newUrl;
        } else {
            logger.warning("redirect not found");
            throw new PluginImplementationException();
        }
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("bol zmazan") || contentAsString.contains("bor sa na serveri nenach") || contentAsString.contains("bor nebol n")) {
            throw new URLNotAvailableAnymoreException("S�bor bol zmazan�");
        }
        if (contentAsString.contains("za sebou stahovat ten")) {
            throw new ServiceConnectionProblemException("Nemozete tolkokrat za sebou stahovat ten isty subor!");
        }

        if (PlugUtils.find("te stiahnu. zadarmo", contentAsString)) {
            throw new ServiceConnectionProblemException("Z�a� 100% - nem��te stiahnu� zadarmo");
        }

        if (contentAsString.contains("Neplatny download")) {
            throw new YouHaveToWaitException("Neplatny download", 2);
        }

        if (contentAsString.contains("Nepodarilo sa nacitat")) {
            throw new ServiceConnectionProblemException("Nepodarilo sa nacitat nieco ohladne suboru!");
        }

    }

}
