package cz.vity.freerapid.plugins.services.ifolder;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

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
        PlugUtils.checkName(httpFile, content, "\u041D\u0430\u0437\u0432\u0430\u043D\u0438\u0435:</span> <b>", "</b>");
        String sizeString = PlugUtils.getStringBetween(content, "\u0420\u0430\u0437\u043C\u0435\u0440:</span> <b>", "</b>");
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
            Matcher matcher = getMatcherAgainstContent("(http://ints\\.ifolder\\.ru/ints/\\?(?:[a-zA-Z0-9\\-]+?.)?(?:ifolder\\.ru|files\\.metalarea\\.org)/[0-9]+\\?ints_code=)");
            if (!matcher.find()) {
                throw new PluginImplementationException("Cannot find link on first page");
            }
            final HttpMethod method2 = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();
            if (makeRedirectedRequest(method2)) {
                contentAsString = getContentAsString();//http://ints.ifolder.ru/ints/sponsor/?bi=577&session
                matcher = PlugUtils.matcher("<a href=(http\\:\\/\\/ints\\.ifolder\\.ru/ints/sponsor[^>]*)>", contentAsString);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Cannot find ads link on second page");
                }
                String nextAction = matcher.group(1);
                final HttpMethod method3 = getMethodBuilder().setReferer("").setAction(nextAction).toHttpMethod();
                if (makeRedirectedRequest(method3)) {
                    final HttpMethod method4 = getMethodBuilder().setReferer("").setActionFromTextBetween("<frame id=\"f_top\" name = \"f_top\" src=\"", "\"").setBaseURL("http://ints.ifolder.ru/").toHttpMethod();
                    if (makeRedirectedRequest(method4)) {
                        int delay = PlugUtils.getWaitTimeBetween(getContentAsString(), "var delay = ", ";", TimeUnit.SECONDS);
                        downloadTask.sleep(delay);


                        /*
                         * Note: Server sends response with no Status-Line and no headers, Special method must be executed
                         */
                        final HttpMethod method4b = new GetMethodNoStatus("http://ints.ifolder.ru" + method4.getPath() + "?" + method4.getQueryString());
                        if (makeRedirectedRequest(method4b)) {
                            do {
                                CaptchaSupport captchaSupport = getCaptchaSupport();
                                String s = getMethodBuilder().setActionFromImgSrcWhereTagContains("src=\"/random/").getAction();
                                s = "http://ints.ifolder.ru" + s;
                                logger.info("Captcha URL " + s);
                                String interstitials_session = PlugUtils.getStringBetween(getContentAsString(), "if(tag){tag.value = \"", "\"");
                                String captchaR = captchaSupport.getCaptcha(s);
                                if (captchaR == null) {
                                    throw new CaptchaEntryInputMismatchException();
                                }
                                final HttpMethod method5 = getMethodBuilder().setReferer("").setActionFromFormByName("form1", true).setParameter("confirmed_number", captchaR).setParameter("interstitials_session", interstitials_session).setBaseURL("http://ints.ifolder.ru/ints/frame/").toHttpMethod();
                                if (!makeRedirectedRequest(method5)) {
                                    throw new ServiceConnectionProblemException();
                                }
                            } while (getContentAsString().contains("name=\"confirmed_number\""));
                            downloadTask.sleep(5); //Needed for full speed

                            final HttpMethod method6 = getMethodBuilder().setReferer("").setActionFromAHrefWhereATagContains("download").toHttpMethod();
                            if (!tryDownloadAndSaveFile(method6)) {
                                logger.warning(getContentAsString());//log the info
                                throw new PluginImplementationException();//some unknown problem
                            }

                        } else {
                            throw new ServiceConnectionProblemException();
                        }
                    } else {
                        throw new ServiceConnectionProblemException();
                    }
                } else {
                    throw new ServiceConnectionProblemException();
                }
            } else {
                throw new ServiceConnectionProblemException();
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
