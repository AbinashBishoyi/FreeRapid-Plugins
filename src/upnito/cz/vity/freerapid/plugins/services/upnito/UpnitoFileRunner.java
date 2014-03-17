package cz.vity.freerapid.plugins.services.upnito;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

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
        //novy zpusob stahovani souboru
        if (content.contains("download.php")) {
            //chystáš sa stiahnuť súbor flyshare.frp (<strong>4</strong>KB)</td>
            PlugUtils.checkName(httpFile, content, "bor", "(<strong");
            Matcher matcher = PlugUtils.matcher("\\(<strong>([0-9]+)</strong>(.?B)\\)", content);
            if (matcher.find()) {
                final long size = PlugUtils.getFileSizeFromString(matcher.group(1) + matcher.group(2));
                httpFile.setFileSize(size);
            } else {
                checkProblems();
                logger.warning("File size was not found\n:");
                throw new PluginImplementationException();
            }

        } else {
            //stary zpusob stahovani souboru, maji to zpetne kompatibilni
            PlugUtils.checkName(httpFile, content, "bor:</strong>", "<br>");
            PlugUtils.checkFileSize(httpFile, content, "kos\u0165:</strong>", "<br>");
        }
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

            this.downloadTask.sleep(10);
            final MethodBuilder builder = getMethodBuilder();
            builder.setBaseURL("http://dl1.upnito.sk").setActionFromFormByName("gdl", true).setReferer(newUrl);
            builder.setAndEncodeParameter("tahaj", "Stiahnuť");
            final HttpMethod httpMethod = builder.toHttpMethod();
            addCookie(new Cookie(".upnito.sk", "verifytext", builder.getParameters().get("verifytext")));
//            final PostMethod postMethod = getPostMethod(newUrl);
//            PlugUtils.addParameters(postMethod, contentAsString, new String[]{"dl2", "verifytext", "sid", "auth_token"});
//
//            postMethod.addParameter("file", "");
//            postMethod.addParameter("userinput", "");
//            postMethod.addParameter("validated", "yes");
//            postMethod.addParameter("tahaj", "Stiahnuť");

            //downloadTask.sleep(650);
            if (!tryDownloadAndSaveFile(httpMethod)) {
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
            throw new URLNotAvailableAnymoreException("Súbor bol zmazaný");
        }
        if (contentAsString.contains("za sebou stahovat ten")) {
            throw new YouHaveToWaitException("Nemozete tolkokrat za sebou stahovat ten isty subor!", 60);
        }

        if (PlugUtils.find("te stiahnu. zadarmo", contentAsString)) {
            throw new ServiceConnectionProblemException("Záťaž 100% - nemôžte stiahnuť zadarmo");
        }

        if (contentAsString.contains("Neplatny download")) {
            throw new YouHaveToWaitException("Neplatny download", 2);
        }

        if (contentAsString.contains("Nepodarilo sa nacitat")) {
            throw new ServiceConnectionProblemException("Nepodarilo sa nacitat nieco ohladne suboru!");
        }

    }

}
