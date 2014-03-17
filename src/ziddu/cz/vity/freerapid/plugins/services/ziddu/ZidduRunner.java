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
        //http://www.ziddu.com/download.php?uid=a6uelpmnaLKZlOKnZ6qhkZSoZKqfm5So7
        //http://www.ziddu.com/downloadlink.php?uid=aqqblpWtbqygnOKnaKqhkZSqZayclZuo8
        //http://www.ziddu.com/downloadlink/1750286/Video_Php_And_Mysql01.txt
        //http://www.ziddu.com/download/1750286/Video_Php_And_Mysql01.txt.html
        //http://www.ziddu.com/downloadfile/1750286/Video_Php_And_Mysql01.txt.html
        logger.info("Checking..." + fileURL);
        fileURL = processURL(fileURL);


        httpFile.setNewURL(new URL(fileURL));
        baseURL = fileURL;
        logger.info("New URL : " + fileURL);

        if (!fileURL.contains("http://downloads.ziddu.com/downloadfile/")) {
            throw new PluginImplementationException("Link Error");
        }

    }



    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting " + fileURL);

        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);

        fileURL = processURL(fileURL);
        logger.info("Set New URL for ziddu : " + fileURL);

        httpFile.setNewURL(new URL(fileURL));

        baseURL = fileURL;
        logger.info("Starting download in TASK!!! " + fileURL);

        String contentAsString = clickDownload(fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        logger.info("Looking for captcha..");

        Matcher matcher = PlugUtils.matcher("img src=\"(/Cap[^\"]*)", contentAsString);
        if (matcher.find()) {
            result = false;
            int count = 0;
            while ((!result) && count < 5) {
                result = stepCaptcha(contentAsString);
                if (!result) {
                    clickDownload(fileURL);
                }
                if (result) count = 6;
                if (cancel) throw new CaptchaEntryInputMismatchException();
                makeRequest(getMethod);
                contentAsString = getContentAsString();
                count++;
            }
        } else throw new PluginImplementationException("Cant find captcha image");
    }

    private String clickDownload(String mURL) throws Exception {
        client.setReferer(mURL);

        PostMethod pMethod = getPostMethod(mURL);
        pMethod.addParameter("mmemid", "0");
        pMethod.addParameter("mname", "");
        pMethod.addParameter("lang", "english");
        pMethod.addParameter("Submit", "");
        pMethod.addParameter("Submit2", " Share ");
        pMethod.addParameter("Submit3", "Link");
        if (makeRequest(pMethod)) {
            logger.info("Success requested");
        }
        makeRequest(pMethod);
        return getContentAsString();

    }

    private String processURL(String mURL) throws Exception {
        String tURL = mURL;
        String myURL = mURL;

//      http://www.ziddu.com/download.php?uid=a6uelpmnaLKZlOKnZ6qhkZSoZKqfm5So7    -> Click Download -> Captha
//      http://www.ziddu.com/downloadlink.php?uid=aqqblpWtbqygnOKnaKqhkZSqZayclZuo8  -> Find URL -> Click Download -> Captha
//
//      http://www.ziddu.com/downloadlink/1750286/Video_Php_And_Mysql01.txt          	-> Process URL -> Captha
//      http://www.ziddu.com/download/1750286/Video_Php_And_Mysql01.txt.html		-> Process URL -> Captha
//      http://www.ziddu.com/downloadfile/1750286/Video_Php_And_Mysql01.txt.html	-> Process URL -> Captha
//      Final Link:
//      http://downloads.ziddu.com/downloadfile/1750286/Video_Php_And_Mysql01.txt.html  -> Captha

        if (tURL.contains("http://downloads.ziddu.com/downloadfile/")) {
            logger.info("Final Link Found " + mURL);
            return mURL;
        }

        if (tURL.contains("http://www.ziddu.com/download.php") || tURL.contains("http://www.ziddu.com/downloadlink.php")) {
            logger.info("Find Link!");
            GetMethod getMethod = getGetMethod(mURL);
            if (makeRequest(getMethod)) {
                String cs = getContentAsString();
                String rx = "(http://www.ziddu.com/download/[^\\<]+)";
                if (tURL.contains("http://www.ziddu.com/download.php")) {
                    rx = "<form action=\"(http://downloads.ziddu.com/downloadfile/[^\"]+)";
                }

                Matcher tmpURL = PlugUtils.matcher(rx, cs);
                if (tmpURL.find()) {
                    myURL = tmpURL.group(1);

                    if (tURL.contains("http://www.ziddu.com/downloadlink.php")) {
                        myURL = getFinalURL(myURL);
                    }
                    logger.info("Final Link Found " + myURL);
                    return myURL;
                } else
                    throw new PluginImplementationException("Can't Find Download Link");
            } else
                throw new PluginImplementationException("Can't Find Download Link");
        }

        if (mURL.contains("/downloadlink/") || mURL.contains("/download/") || mURL.contains("/downloadfile/")) {
            myURL = getFinalURL(mURL);
            return myURL;
        }
        return myURL;
    }

    private String getFinalURL(String mURL) throws Exception {
        Matcher finURL = PlugUtils.matcher("http://www.ziddu.com/[a-z]+/([0-9)]+)/(.+)", mURL);
        if (finURL.find()) {
            String fURL;

            fURL = "http://downloads.ziddu.com/downloadfile/" + finURL.group(1) + "/" + finURL.group(2);
            logger.info("Final URL: " + fURL);
            return fURL;
        }
        throw new PluginImplementationException("Can't find appropriate link format");

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

                    String[] parameters = new String[]{"fid", "tid", "fname", "submit"}; //array of parameter names for parsing
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