package cz.vity.freerapid.plugins.services.enterupload;

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
        if (contentAsString.contains("CONTENT=\"Download")){
            Matcher matcher = PlugUtils.matcher("CONTENT=\"Download ([^\"]+)", contentAsString);
            if (matcher.find()) {
               String fn = matcher.group(1);
                httpFile.setFileName(fn);
                logger.info("File name " + fn);
            } else logger.warning("File name was not found" + contentAsString);

            matcher = PlugUtils.matcher("small>(([^)]+ bytes))", contentAsString);
            if (matcher.find()) {
                long a = PlugUtils.getFileSizeFromString(matcher.group(1));
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

        if (makeRequest(getMethod)) {
            final String contentAsString = getContentAsString();
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
//(http://www\.enterupload\.com/captchas/[^\"]+)
//(http://www\.enterupload\.com/captchas/[^\"]+)
            Matcher matcher = PlugUtils.matcher("captchas", contentAsString);
            if (matcher.find()) {


                String result = "error";
                int count =0;

                while ((result.equals("error")) && count<3) {
                       result = stepCaptcha(contentAsString);

                       if (result.equals("success") || result.equals("Cancel")) {
                           count=4;

                       }

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
                    //matcher = PlugUtils.matcher("action=\"([^\"]*)", contentAsString);
                    //if (matcher.find()) {
                        //s = "http://www.ziddu.com" + matcher.group(1);
                        //logger.info(s);

//                    <Form name="F1" method="POST" onSubmit="if($('btn_download').disabled)return false;$('btn_download').value='Sending File...';$('btn_download').disabled=true;return true;">
//<input type="hidden" name="op" value="download2">
//<input type="hidden" name="id" value="meybvc1ty6am">
//<input type="hidden" name="rand" value="zcddl7mc">

//<input type="hidden" name="method_free" value="">
//<input type="hidden" name="method_premium" value="">
//<tr><td><img src="http://www.enterupload.com/captchas/meybvc1ty6amzcddl7mc.jpg"></td><td valign=middle><input type="text" name="code" class="captcha_code"></td></tr>
//<br><span id="countdown_str">Wait <span id="countdown">15</span> seconds</span>
//<input type="hidden" name="down_script" value="1">
//<input type="submit" id="btn_download" value="Download File">


//</Form>
                        final PostMethod method = getPostMethod(baseURL);

                        String[] parameters = new String[]{"op", "id", "rand", "method_free", "method_premium","down_script","btn_download"}; //array of parameter names for parsing
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


            } return "error";




 }


    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));

        }

    }


}