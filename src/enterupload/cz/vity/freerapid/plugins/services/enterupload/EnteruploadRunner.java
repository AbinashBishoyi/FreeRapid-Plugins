package cz.vity.freerapid.plugins.services.enterupload;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */

class EnteruploadRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EnteruploadRunner.class.getName());
    private String httpSite;
    private String baseURL;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        //http://www.enterupload.com/meybvc1ty6am/Website.Layout.Maker.Ultra.Edition.v2.4.rar.html

        baseURL = fileURL;

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameandSize(getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkNameandSize(String contentAsString) throws Exception {
//<META NAME="description" CONTENT="Download Website.Layout.Maker.Ultra.Edition.v2.4.rar">
        if (!contentAsString.contains("Download File")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));

        }
        //Matcher matcher = PlugUtils.matcher("Download ([^,]+), upload", contentAsString);
        if (contentAsString.contains("CONTENT=\"Download")) {
            Matcher matcher = PlugUtils.matcher("CONTENT=\"Download ([^\"]+)", contentAsString);
            if (matcher.find()) {
                String fn = matcher.group(1);
                httpFile.setFileName(fn);
                logger.info("File name " + fn);
            } else logger.warning("File name was not found" + contentAsString);

            matcher = PlugUtils.matcher("small>(([^)]+ bytes))", contentAsString);
            if (matcher.find()) {
                String s = matcher.group(1) + " " + "b";
                s = s.substring(1);


                long a = PlugUtils.getFileSizeFromString(s);
                logger.info("File size " + a);
                httpFile.setFileSize(a);
                httpFile.setFileState(FileState.CHECKED_AND_EXISTING);


            } else logger.warning("File size was not found" + contentAsString);


        } else logger.warning("File data was not found" + contentAsString);


    }

    @Override
    public void run() throws Exception {
        super.run();
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);


        baseURL = fileURL;
        logger.info("Starting download in TASK " + fileURL);

        GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);

        if (makeRequest(getMethod)) {
            String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);

//(http://www\.enterupload\.com/captchas/[^\"]+)
            Matcher matcher = PlugUtils.matcher("captchas", contentAsString);
            if (matcher.find()) {


                String result = "error";
                int count = 0;

                while ((result.equals("error")) && count < 3) {
                    result = stepCaptcha(contentAsString);

                    if (result.equals("success") || result.equals("Cancel")) {
                        count = 4;

                    }

                    makeRequest(getMethod);
                    contentAsString = getContentAsString();


                    count++;
                }


            }


        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private String stepCaptcha(String contentAsString) throws Exception {

        Matcher matcher = PlugUtils.matcher("(http://www.enterupload.com/captchas/[^\"]+)", contentAsString);
        if (matcher.find()) {
            String s = matcher.group(1);
            logger.info(httpSite + s);
            client.setReferer(baseURL);
            String code = getCaptchaSupport().getCaptcha(s); //returns "" when user pressed OK with no input

            if (code == null) {
                //throw new CaptchaEntryInputMismatchException();
                return "error";


            } else {
                downloadTask.sleep(15);//extract sleep time from the website :-)
                String spost = "http://www.enterupload.com/";
                client.setReferer(fileURL);//referer
                final PostMethod method = getPostMethod(fileURL);//file url

                String[] parameters = new String[]{"op", "id", "rand", "method_free", "method_premium", "down_script"}; //array of parameter names for parsing
                PlugUtils.addParameters(method, contentAsString, parameters);
                //method.addParameter("Keyword", "Ok"); //it always sends 'Ok'
                method.addParameter("code", code); //it does not work without captcha

                client.getHTTPClient().getParams().setBooleanParameter("noContentTypeInHeader", true);

                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    //if (getContentAsString().contains("Please enter") || getContentAsString().contains("w="))
                    //   return false;
                    logger.warning(getContentAsString());
                    //logger.warning("Wrong captcha");
                    return "error";
                    //throw new IOException("File input stream is empty.");


                } else return "success";

                //} else throw new InvalidURLOrServiceProblemException("Cant find action - " + contentAsString);


            }


        }
        return "error";


    }


    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));

        }

    }


}