package cz.vity.freerapid.plugins.services.bagruj;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, Smisek
 */
class BagrujRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BagrujRunner.class.getName());
    private final static Map<String, GetMethod> methodsMap = new HashMap<String, GetMethod>();     // added by Smisek
    private int captchaCounter;

    public BagrujRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            checkCaptcha();     // added by Smisek
        } else
            throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
    }

    public void run() throws Exception {
        super.run();

        Login();     // added by Smisek

        if (checkInQueue())     // added by Smisek
            return;
        captchaCounter = 0;

        /* Bagruj can sometimes redirect to uloz.to
         *  modified by JPEXS
         */
        GetMethod getMethod = getGetMethod(fileURL);
        int ret=client.makeRequest(getMethod,false);        
        if(ret==301){
            String targetLink=getMethod.getResponseHeader("Location").getValue();
            logger.info("Redirection to:"+targetLink);
            getMethod.releaseConnection();
            if(targetLink.matches("http://(www\\.)?(uloz\\.to|ulozto\\.net|ulozto\\.cz|ulozto\\.sk)/.+")){                
                final List<URI> uriList = new LinkedList<URI>();
                uriList.add(new URI(targetLink));
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
                return;
            }else{
                getMethod = getGetMethod(targetLink);
                if(!makeRedirectedRequest(getMethod)){
                    throw new ServiceConnectionProblemException();
                }
            }
        }        
            if (getContentAsString().contains("testcaptcha")) {
                checkNameAndSize(getContentAsString());
                while (getContentAsString().contains("testcaptcha")) {
                    Matcher matcher = getMatcherAgainstContent("<span id=\"countdown\">([0-9]+)</span>");
                    PostMethod method = stepCaptcha(getContentAsString());
                    if (matcher.find()) {
                        int time = Integer.parseInt(matcher.group(1));
                        downloadTask.sleep(time - 1);
                    }
                    if (!makeRedirectedRequest(method))
                        throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
                }
                if (!getContentAsString().contains("pro tvoji IP adresu n")) {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }

                Matcher matcher = getMatcherAgainstContent("(http://[^\"]+)\">\\1");

                if (!matcher.find()) {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }

                String finalURL = matcher.group(1);
                GetMethod finalMethod = getGetMethod(finalURL);

                if (!tryDownloadAndSaveFile(finalMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }

            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
    }

    // added by Smisek
    private boolean checkInQueue() throws Exception {
        if (!methodsMap.containsKey(fileURL))
            return false;

        GetMethod method = methodsMap.get(fileURL);
        methodsMap.remove(fileURL);

        httpFile.setState(DownloadState.GETTING);
        return tryDownloadAndSaveFile(method);
    }

    // added by Smisek
    private void checkCaptcha() throws Exception {
        if (getContentAsString().contains("captcha_code")) {
            checkNameAndSize(getContentAsString());
            while (getContentAsString().contains("captcha_code")) {
                Matcher matcher = getMatcherAgainstContent("<span id=\"countdown\">([0-9]+)</span>");
                PostMethod method = stepCaptcha(getContentAsString());
                if (matcher.find()) {
                    int time = Integer.parseInt(matcher.group(1));
                    downloadTask.sleep(time - 1);
                }
                if (!makeRedirectedRequest(method))
                    throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
            }
            if (!getContentAsString().contains("pro tvoji IP adresu n")) {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException("Cannot find requested page content");
            }

            Matcher matcher = getMatcherAgainstContent("(http://[^\"]+)\">\\1");

            if (!matcher.find()) {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException("Cannot find requested page content");
            }

            String finalURL = matcher.group(1);
            GetMethod finalMethod = getGetMethod(finalURL);

            methodsMap.put(fileURL, finalMethod);
        } else {
            checkProblems();
            logger.info(getContentAsString());
            throw new PluginImplementationException();
        }
    }

    //added by Smisek
    private void Login() throws Exception {
        synchronized (BagrujRunner.class) {
            BagrujServiceImpl service = (BagrujServiceImpl) getPluginService();
            service.setMaxDown(1);
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                return;
            }

            Matcher matcher;
            String postURL = "http://bagruj.cz/";

            PostMethod postmethod = getPostMethod(postURL);

            postmethod.addParameter("login", pa.getUsername());
            postmethod.addParameter("password", pa.getPassword());
            postmethod.addParameter("op", "login");

            if (makeRedirectedRequest(postmethod)) {
                getContentAsString();
                matcher = getMatcherAgainstContent("op=logout\">Odhl.sit</a>");
                if (!matcher.find()) {
                    return;
                }
                GetMethod getMethod = getGetMethod(fileURL);
                if (!makeRedirectedRequest(getMethod)) {
                    throw new PluginImplementationException();
                } else {
                    service.setMaxDown(2);
                }
            }
        }
    }

    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("bagruj.cz")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("No such file with this filename")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>No such file with this filename.</b><br>"));
        }

        Matcher matcher = getMatcherAgainstContent("Soubor:((<[^>]*>)|\\s)*([^<]*)<");
        if (matcher.find()) {
            String fn = matcher.group(matcher.groupCount());
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }

        matcher = getMatcherAgainstContent("\\(([0-9.]+ bytes)\\)");
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }
        // konec odebirani jmena


    }


    private PostMethod stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("captcha_code")) {
            CaptchaSupport captchaSupport = getCaptchaSupport();
            Matcher matcher = PlugUtils.matcher("(http://bagruj.cz/captchas/[^\"]+)", contentAsString);
            if (matcher.find()) {
                String s = matcher.group(1);
                logger.info("Captcha URL " + s);


                final String captcha;
                if (captchaCounter < 4) {
                    ++captchaCounter;
                    final BufferedImage captchaImage = captchaSupport.getCaptchaImage(s);
                    captcha = new CaptchaRecognizer().recognize(captchaImage);
                } else {
                    captcha = captchaSupport.getCaptcha(s);
                }

                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {
                    client.setReferer(fileURL);
                    final PostMethod postMethod = getPostMethod(fileURL);
                    postMethod.addParameter("op", "download2");
                    String[] parameters = new String[]{"id", "rand", "method_free", "method_free", "down_direct"};
                    PlugUtils.addParameters(postMethod, contentAsString, parameters);
                    postMethod.addParameter("code", captcha);
                    return postMethod;

                }
            } else {
                logger.warning(contentAsString);
                throw new PluginImplementationException("Captcha picture was not found");
            }
        }
        throw new PluginImplementationException();
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("No such file")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>No such file with this filename.</b><br>"));
        }
        if (getContentAsString().contains("The page you are looking for is temporarily unavailable")) {
            throw new ServiceConnectionProblemException(String.format("<b>The page you are looking for is temporarily unavailable</b><br>"));

        }


    }

}