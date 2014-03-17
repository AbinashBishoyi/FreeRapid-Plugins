package cz.vity.freerapid.plugins.services.mimima;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 */
class MimimaRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MimimaRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        logger.info(fileURL);
        if (makeRequest(method)) {
            //fetch.php
            //http://www6.mimima.com/link.php?ref=PwHnePBkHK
            String contentAsString = getContentAsString();
            //String code = PlugUtils.getParameter("code", contentAsString);
            client.setReferer("http://www6.mimima.com/fetch.php");
            final PostMethod pmethod = getPostMethod("http://www6.mimima.com/fetch.php");
            //String[] parameters = new String[]{"code"};
            //pmethod.addParameter("code", code);
            //<input type=text name=code size=30 value=PwHnePBkHK >
            Matcher matcher = PlugUtils.matcher("<input type=text name=code size=30 value=([^>]+)", contentAsString);
            if (matcher.find()) {
                String code = matcher.group(1);
                logger.info(code);
                pmethod.addParameter("code", code);
                client.getHTTPClient().getParams().setParameter("noContentTypeInHeader", true);//take everything 
                if (!tryDownloadAndSaveFile(pmethod)) {
                    checkProblems();
                    //if (getContentAsString().contains("Please enter") || getContentAsString().contains("w="))
                    //   return false;
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                    //logger.warning("Wrong captcha");

                    //throw new IOException("File input stream is empty.");


                } //else throw new ServiceConnectionProblemException();

            }

            //PlugUtils.addParameters(pmethod, contentAsString, parameters);
            //client.getHTTPClient().getParams().setBooleanParameter("noContentTypeInHeader", true);

            // inal PostMethod method = getPostMethod(fileURL);//file url

            //        String[] parameters = new String[]{"op", "id", "rand", "method_free", "method_premium", "down_script"}; //array of parameter names for parsing
            //        PlugUtils.addParameters(method, contentAsString, parameters);
            //method.addParameter("Keyword", "Ok"); //it always sends 'Ok'
            //        method.addParameter("code", code); //it does not work without captcha

            //        client.getHTTPClient().getParams().setBooleanParameter("noContentTypeInHeader", true);
//
            //        if (!tryDownloadAndSaveFile(method))

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("was not found")) {
            throw new URLNotAvailableAnymoreException("The page you requested was not found in our database.");
        }
    }

}
