package cz.vity.freerapid.plugins.services.rapidu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class RapiduFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RapiduFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1 title=\"", "\">");
        final Matcher match = PlugUtils.matcher("</h1>\\s*?<small>(.+?)<", content);
        if (!match.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        doLogin();
        final GetMethod method = getGetMethod(fileURL); //create GET request
        final int status = client.makeRequest(method, false);
        if (status / 100 == 3) {
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else if (status == 200) {
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final String dlURL;
            if (contentAsString.contains("Konto: <b>Premium")) {
                dlURL = PlugUtils.getStringBetween(contentAsString, "href=\"", "\" class=\"premium\"");
            } else {
                final String fileID = PlugUtils.getStringBetween(contentAsString, "downloadFreeFile('", "');");
                final HttpMethod waitMethod = getMethodBuilder()
                        .setAjax().setReferer(fileURL)
                        .setAction("/ajax.php?a=getLoadTimeToDownload")
                        .setParameter("_go", "")
                        .toPostMethod();
                if (!makeRedirectedRequest(waitMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                final int wait = (int) (Long.parseLong(PlugUtils.getStringBetween(getContentAsString(), "{\"timeToDownload\":", "}")) - (System.currentTimeMillis() / 1000));
                if (wait > 0)
                    downloadTask.sleep(wait + 1);
                int loop = 0;
                do {
                    loop++;
                    if (loop > 5)
                        throw new ErrorDuringDownloadingException("Excessive failed captcha attempts");
                    final HttpMethod httpMethod = doCaptcha(getMethodBuilder()
                            .setAjax().setReferer(fileURL)
                            .setAction("/ajax.php?a=getCheckCaptcha")
                            .setParameter("fileId", fileID)
                            .setParameter("_go", "")
                    ).toPostMethod();
                    if (!makeRedirectedRequest(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                } while (getContentAsString().contains("\"message\":\"error\""));
                if (getContentAsString().trim().equals(""))
                    throw new PluginImplementationException("Error");
                if (!getContentAsString().contains("\"message\":\"success\""))
                    throw new PluginImplementationException("Error getting download link");
                dlURL = PlugUtils.getStringBetween(getContentAsString(), "url\":\"", "\"").replace("\\/", "/");
            }
            if (!tryDownloadAndSaveFile(getGetMethod(dlURL))) {
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
        if (contentAsString.contains("Nie znaleziono pliku")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Trwają prace techniczne")) {
            throw new NotRecoverableDownloadException("Technical work on the target server underway");
        }
        if (contentAsString.contains("h2>To gain access to any files")) {
            throw new NotRecoverableDownloadException("Premium account needed - " + PlugUtils.getStringBetween(contentAsString, "<h1>", "</h1>"));
        }
    }

    public MethodBuilder doCaptcha(final MethodBuilder methodBuilder) throws Exception {
        final HttpMethod jsMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("/js/global.engine.js")
                .toGetMethod();
        if (!makeRedirectedRequest(jsMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        final Matcher reCaptchaKeyMatcher = PlugUtils.matcher("Recaptcha.create\\('(.+?)',", getContentAsString());
        if (!reCaptchaKeyMatcher.find()) {
            throw new PluginImplementationException("ReCaptcha key not found");
        }
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1);
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        methodBuilder.setParameter("captcha1", r.getChallenge());
        methodBuilder.setParameter("captcha2", captcha);
        return methodBuilder;
    }

    private void doLogin() throws Exception {
        synchronized (RapiduFileRunner.class) {
            RapiduServiceImpl service = (RapiduServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                logger.info("No Rapidu account");
            } else {
                final HttpMethod method = getMethodBuilder()
                        .setAjax().setReferer(getBaseURL())
                        .setAction("/ajax.php?a=getUserLogin")
                        .setParameter("login", pa.getUsername())
                        .setParameter("pass", pa.getPassword())
                        .setParameter("remember", "1")
                        .setParameter("_go", "")
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException("Error posting login info");
                }
                if (getContentAsString().contains("message\":\"error")) {
                    throw new BadLoginException("Invalid Rapidu account login information!");
                }
                if (!getContentAsString().contains("message\":\"success")) {
                    throw new PluginImplementationException("Unknown error logging into Rapidu account!");
                }
                logger.info("Logged into Rapidu account");
            }
        }
    }
}