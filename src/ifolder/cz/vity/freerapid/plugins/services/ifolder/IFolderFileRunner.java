package cz.vity.freerapid.plugins.services.ifolder;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import org.apache.commons.httpclient.HttpClient;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class IFolderFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(IFolderFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            throw new PluginImplementationException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "\u041D\u0430\u0437\u0432\u0430\u043D\u0438\u0435: <b>", "</b>");
        String sizeString = PlugUtils.getStringBetween(content, "\u0420\u0430\u0437\u043C\u0435\u0440: <b>", "</b>");
        //Replace azbuka letters with latin:
        sizeString = sizeString.replace('\u041C', 'M');
        sizeString = sizeString.replace('\u0431', 'B');
        sizeString = sizeString.replace('\u043A', 'K');
        sizeString = sizeString.replace('\u041A', 'K');
        sizeString = sizeString.replace('\u0433', 'G');
        sizeString = sizeString.replace('\u0413', 'G');
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(sizeString));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            Matcher matcher = getMatcherAgainstContent("(http://ints.ifolder.ru/ints/\\?(?:[a-zA-Z0-9\\-]+?.)?ifolder.ru/[0-9]+\\?ints_code=)");
            if (!matcher.find()) {
                throw new PluginImplementationException("Cannot find link on first page");
            }
            String secondUrl = matcher.group(1);
            HttpMethod method2 = getMethodBuilder().setReferer(fileURL).setAction(secondUrl).toGetMethod();
            HttpClient hc = client.getHTTPClient();
            hc.executeMethod(method2);
            String redirectedUrl = method2.getResponseHeader("Location").getValue();
            method2 = getMethodBuilder().setReferer(fileURL).setAction(redirectedUrl).toGetMethod();

            if (makeRedirectedRequest(method2)) {
                do {
                    contentAsString = getContentAsString();
                    String sessionId = PlugUtils.getStringBetween(contentAsString, "session=", "&mem");
                    CaptchaSupport captchaSupport = getCaptchaSupport();
                    String host = PlugUtils.getStringBetween(fileURL, "http://", "/");
                    String hidden_code = PlugUtils.getStringBetween(contentAsString, "var c = ['hidden_code', 'hh", "'];");

                    String captchaR = captchaSupport.getCaptcha("http://" + host + "/random/images/?session=" + sessionId + "&mem");

                    final HttpMethod method3 = getMethodBuilder().setReferer(redirectedUrl).setActionFromFormByName("form1", true).setParameter("confirmed_number", captchaR).setParameter("hidden_code", hidden_code).setParameter("activate_ads_free", "0").setAction(redirectedUrl).toHttpMethod();
                    if(!makeRedirectedRequest(method3)){
                        throw new PluginImplementationException();
                    }
                } while (getContentAsString().contains("confirmed_number"));

                final HttpMethod method4 = getMethodBuilder().setReferer(redirectedUrl).setActionFromAHrefWhereATagContains("c\u043A\u0430\u0447\u0430\u0442\u044C").toHttpMethod();
                if (!tryDownloadAndSaveFile(method4)) {
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }

            } else {
                throw new PluginImplementationException();
            }            
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("\u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D") || contentAsString.contains("\u0443\u0434\u0430\u043B\u0435\u043D")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }
}
