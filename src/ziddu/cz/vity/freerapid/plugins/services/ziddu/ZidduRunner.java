package cz.vity.freerapid.plugins.services.ziddu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 */

class ZidduRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ZidduRunner.class.getName());
    //private String httpSite;
   

    private String baseURL;
    public String actionURL;
    public String capURL;


    public boolean result;
    public boolean cancel;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        //http://www.ziddu.com/downloadlink.php?uid=aqqblpWtbqygnOKnaKqhkZSqZayclZuo8
        //http://www.ziddu.com/downloadlink/1750286/Video_Php_And_Mysql01.txt
        //http://www.ziddu.com/download/1750286/Video_Php_And_Mysql01.txt.html
        //http://www.ziddu.com/downloadfile/1750286/Video_Php_And_Mysql01.txt.html
        logger.info("Starting..."+ fileURL);
        
        fileURL = processURL(fileURL);
        actionURL=fileURL.replace(".html","");
        
        httpFile.setNewURL(new URL(fileURL));
        baseURL = fileURL;
        logger.info("New URL : " + fileURL);


        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameandSize(getContentAsString());
           // getCaptcha(getContentAsString());

        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }
//private void getCaptcha(String contentAsString) throws Exception {
//               Matcher matcher = PlugUtils.matcher("img src=\"(/Cap[^\"]*)", contentAsString);
//        if (matcher.find()) {
//
//        }
//}
    private void checkNameandSize(String contentAsString) throws Exception {

        if (!contentAsString.contains("Thank You For Downloading")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));
        }

        if (contentAsString.contains("fname")) {

            String fn = PlugUtils.getParameter("fname", contentAsString);
            fn = fn.replace(".html", "");//why did you removed it? it's OK with it
            fn = fn.replace("\"", "");
            fn = fn.replace(";", "");

            logger.info("File name " + fn);
            httpFile.setFileName(fn);

            Matcher matcher = PlugUtils.matcher("(([0-9.]* .B))", contentAsString);
            if (matcher.find()) {
                Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + a);
                httpFile.setFileSize(a);
                httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            } else logger.warning("File size was not found" + contentAsString);

        } else logger.warning("File name was not found" + contentAsString);


    }

    @Override
    public void run() throws Exception {
        super.run();
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        if (!fileURL.contains("downloadfile")) {
        fileURL = processURL(fileURL);
        if (!httpFile.getFileUrl().toString().contains(fileURL)) {
            logger.info("Set again : " + fileURL);
            httpFile.setNewURL(new URL(fileURL));

        }}
        
        baseURL = fileURL;
        logger.info("Starting download in TASK!!! " + fileURL);

        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRequest(getMethod)) {
            String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);       //<img src="/CaptchaSecurityImages.php?width=100&amp;height=38&amp;characters=5"
            logger.info("Looking for captcha..");
            
            Matcher matcher = PlugUtils.matcher("img src=\"(/Cap[^\"]*)", contentAsString);
            if (matcher.find()) {
                result = false;
                int count = 0;
                while ((!result) && count < 3) {
                    result = stepCaptcha(contentAsString);
                    if (result) count = 4;
                    if (cancel) throw new CaptchaEntryInputMismatchException();
                    makeRequest(getMethod);
                    contentAsString = getContentAsString();
                    count++;
                }
            } else throw new PluginImplementationException("Cant find captcha image");
        } else throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private String processURL(String mURL) throws Exception {
        String tURL = mURL;

        if (tURL.contains("downloadlink.php")) {
            final GetMethod getLink = getGetMethod(tURL);
            if (makeRequest(getLink)) {
                final String stringLink = getContentAsString();
                if (stringLink.contains("http://www.ziddu.com/download/")) {
                    Matcher lnkMatch = PlugUtils.matcher("class=\"download\">([^<]+)", stringLink);
                    if (lnkMatch.find()) {
                        tURL = lnkMatch.group(1);
                        tURL = tURL.trim();
                    } else throw new InvalidURLOrServiceProblemException("Cant find download link");
                } else throw new InvalidURLOrServiceProblemException("Cant find download link page");
            } else
                throw new InvalidURLOrServiceProblemException("Problem with a connection to service.\nCannot find requested page content");
        } else if (tURL.contains("download.php")) {
            final GetMethod getLink = getGetMethod(tURL);
            if (makeRequest(getLink)) {
                final String stringLink = getContentAsString();
                if (stringLink.contains("href=\"/downloadfile/")) {
                    Matcher lnkMatch = PlugUtils.matcher("href=\"(/downloadfile[^\"]+)", stringLink);
                    if (lnkMatch.find()) {
                        tURL = "http://www.ziddu.com" + lnkMatch.group(1);
                        tURL = tURL.trim();
                    } else throw new InvalidURLOrServiceProblemException("Cant find download link");
                } else throw new InvalidURLOrServiceProblemException("Cant find download link page");
            } else
                throw new InvalidURLOrServiceProblemException("Problem with a connection to service.\nCannot find requested page content");
        }

        if (tURL.contains("www.ziddu.com/downloadlink")) {
            tURL = tURL.replaceFirst("www.ziddu.com/downloadlink", "www.ziddu.com/downloadfile") + ".html";

        } else if (tURL.contains("www.ziddu.com/download/")) {
            tURL = tURL.replaceFirst("www.ziddu.com/download/", "www.ziddu.com/downloadfile/");
        }
        return tURL;
    }

    private boolean stepCaptcha(String contentAsString) throws Exception {

        Matcher matcher = PlugUtils.matcher("img src=\"(/Cap[^\"]*)", contentAsString);
        if (matcher.find()) {
            String s = "http://www.ziddu.com" + matcher.group(1);
            logger.info("Captcha: " + s);
            client.setReferer(baseURL);
            String securitycode = getCaptchaSupport().getCaptcha(s); //returns "" when user pressed OK with no input

            if (securitycode == null) {
                cancel = true;
                return false;
            } else {
                matcher = PlugUtils.matcher("action=\"([^\"]*)", contentAsString);
                if (matcher.find()) {
                    s = "http://www.ziddu.com" + matcher.group(1);
                    logger.info(s);
                    final PostMethod method = getPostMethod(s);

                    String[] parameters = new String[]{"fid", "tid", "fname",  "submit"}; //array of parameter names for parsing
                    PlugUtils.addParameters(method, contentAsString, parameters);
                    method.addParameter("Keyword", "Keyword"); //it always sends 'Ok'
                    method.addParameter("securitycode", securitycode); //it does not work without captcha

                    client.getHTTPClient().getParams().setBooleanParameter("noContentTypeInHeader", true);

                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();

                        logger.warning(getContentAsString());

                        return false;
                    } else return true;
                } else throw new InvalidURLOrServiceProblemException("Cant find action - " + contentAsString);
            }
        }
        return false;
    }


    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));
        }
    }


}