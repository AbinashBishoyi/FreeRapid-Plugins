package cz.vity.freerapid.plugins.services.ziddu;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

//import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */

class ZidduRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ZidduRunner.class.getName());
    private String httpSite;
    private String baseURL;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        //http://www.ziddu.com/downloadlink.php?uid=aqqblpWtbqygnOKnaKqhkZSqZayclZuo8
        //http://www.ziddu.com/downloadlink/1750286/Video_Php_And_Mysql01.txt
        //http://www.ziddu.com/download/1750286/Video_Php_And_Mysql01.txt.html
        //http://www.ziddu.com/downloadfile/1750286/Video_Php_And_Mysql01.txt.html

        if (fileURL.contains("downloadlink.php")) {
            final GetMethod getLink = getGetMethod(fileURL);
            if (makeRequest(getLink)) {
                final String stringLink = getContentAsString();
                if (stringLink.contains("http://www.ziddu.com/download/")) {
                    Matcher lnkMatch = PlugUtils.matcher("class=\"download\">([^<]+)", stringLink);
                        if (lnkMatch.find()) {
                            fileURL = lnkMatch.group(1);
                            fileURL = fileURL.trim();


                        } else throw new InvalidURLOrServiceProblemException("Cant find download link");

                } else throw new InvalidURLOrServiceProblemException("Cant find download link page");


            }  else throw new InvalidURLOrServiceProblemException("Problem with a connection to service.\nCannot find requested page content");


        }



        if (fileURL.contains("www.ziddu.com/downloadlink")) {
            fileURL = fileURL.replaceFirst("www.ziddu.com/downloadlink", "www.ziddu.com/downloadfile") + ".html";

        } else if (fileURL.contains("www.ziddu.com/download/")) {
            fileURL = fileURL.replaceFirst("www.ziddu.com/download/", "www.ziddu.com/downloadfile/");
        }

        baseURL = fileURL;

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameandSize(getContentAsString());
        } else throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkNameandSize(String contentAsString) throws Exception {

        if (!contentAsString.contains("Thank You For Downloading")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));

        }
        //Matcher matcher = PlugUtils.matcher("Download ([^,]+), upload", contentAsString);
        if (contentAsString.contains("fname")) {
            //final String fn = new String(matcher.group(1).getBytes("windows-1252"), "UTF-8");
            //String ndpage = PlugUtils.getParameter("2ndpage", contentAsString);
            String fn = PlugUtils.getParameter("fname", contentAsString);
            fn = fn.replace(".html", "");//why did you removed it? it's OK with it
            fn = fn.replace("\"", "");
            fn = fn.replace(";", "");


            logger.info("File name " + fn);
            httpFile.setFileName(fn);

            Matcher matcher = PlugUtils.matcher("(([0-9.]* .B))<", contentAsString);
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
        //baseURL = fileURL;

                if (fileURL.contains("downloadlink.php")) {
            final GetMethod getLink = getGetMethod(fileURL);
            if (makeRequest(getLink)) {
                final String stringLink = getContentAsString();
                if (stringLink.contains("http://www.ziddu.com/download/")) {
                    Matcher lnkMatch = PlugUtils.matcher("class=\"download\">([^<]+)", stringLink);
                        if (lnkMatch.find()) {
                            fileURL = lnkMatch.group(1);
                            fileURL = fileURL.trim();

                        } else throw new InvalidURLOrServiceProblemException("Cant find download link");

                } else throw new InvalidURLOrServiceProblemException("Cant find download link page");


            }  else throw new InvalidURLOrServiceProblemException("Problem with a connection to service.\nCannot find requested page content");


        }

        if (fileURL.contains("www.ziddu.com/downloadlink")) {
            fileURL = fileURL.replaceFirst("www.ziddu.com/downloadlink", "www.ziddu.com/downloadfile") + ".html";

        } else if (fileURL.contains("www.ziddu.com/download/")) {
            fileURL = fileURL.replaceFirst("www.ziddu.com/download/", "www.ziddu.com/downloadfile/");
        }

        baseURL = fileURL;
        //httpSite = fileURL.substring(0, fileURL.lastIndexOf('/'));
        logger.info("Starting download in TASK " + fileURL);

        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRequest(getMethod)) {
            String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);
            //<input type="hidden" name="fid" id="fid" value="2847949">
            // <input type="hidden" name="tid" id="tid" value="MjAwOC0xMi0wNQ==">
            //<input name="securitycode" type="text" id="securitycode" size="10"/>
            //<input type="hidden" name="fname" id="fname" value="1_458215942l.jpg.html"></td>
            //<input type="hidden" name="securecode" id="securecode" value="xvefkc">
            //     <img src="/CaptchaSecurityImages.php?width=100&height=38&characters=5" align="absmiddle" id="image" name="image"/>
            //<input type="hidden" name="Keyword"  value="Keyword">
            //   <input type="submit" name="submit" value="Download"></td>

            //GET CAPTHA

            Matcher matcher = PlugUtils.matcher("img src=\"(/Cap[^\"]*)", contentAsString);
            if (matcher.find()) {


                String result = "error";
                int count =0;

                while ((result.equals("error")) && count<3) {
                       result = stepCaptcha(contentAsString);

                       if (result.equals("success") || result.equals("Cancel")) {
                           count=4;

                       }
                        makeRequest(getMethod);
                       contentAsString = getContentAsString();
                        count++;
                }

               
                }


           


        } else throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

 private String stepCaptcha(String contentAsString) throws Exception {

     Matcher matcher = PlugUtils.matcher("img src=\"(/Cap[^\"]*)", contentAsString);
            if (matcher.find()) {
                String s = "http://www.ziddu.com" + matcher.group(1);
                logger.info(httpSite + s);
                client.setReferer(baseURL);
                String securitycode = getCaptchaSupport().getCaptcha(s); //returns "" when user pressed OK with no input

                if (securitycode == null) {
                    //throw new CaptchaEntryInputMismatchException();
                    return "Cancel";


                } else {
                    matcher = PlugUtils.matcher("action=\"([^\"]*)", contentAsString);
                    if (matcher.find()) {
                        s = "http://www.ziddu.com" + matcher.group(1);
                        logger.info(s);
                        final PostMethod method = getPostMethod(s);

                        String[] parameters = new String[]{"fid", "tid", "fname", "securecode", "submit"}; //array of parameter names for parsing
                        PlugUtils.addParameters(method, contentAsString, parameters);
                        method.addParameter("Keyword", "Ok"); //it always sends 'Ok'
                        method.addParameter("securitycode", securitycode); //it does not work without captcha

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

                    } else throw new InvalidURLOrServiceProblemException("Cant find action - " + contentAsString);


                } 


            } return "error";




 }


    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));

        }

    }


}