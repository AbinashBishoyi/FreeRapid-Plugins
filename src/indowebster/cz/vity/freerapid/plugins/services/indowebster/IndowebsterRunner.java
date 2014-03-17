package cz.vity.freerapid.plugins.services.indowebster;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;


import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alex, JPEXS
 */
class IndowebsterRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(IndowebsterRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        makeRequest(getMethod);        
        checkNameandSize(getContentAsString());        
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method1 = getGetMethod(fileURL);
        if (makeRedirectedRequest(method1)) {
            String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);
            Matcher matcher=getMatcherAgainstContent("<b>Download Link</b>.*<textarea[^>]*>(http.*)</textarea>");
            if(matcher.find()){
                String secondUrl=matcher.group(1);
                final HttpMethod method2=getMethodBuilder().setReferer(fileURL).setAction(secondUrl).toGetMethod();
                client.getHTTPClient().getParams().setHttpElementCharset("iso-8859-1");
                client.getHTTPClient().getParams().setParameter("pageCharset", "iso-8859-1");
                if (makeRedirectedRequest(method2)){                    
                    String content=getContentAsString();
                    matcher=getMatcherAgainstContent("eval\\(.*\\)");
                    if(matcher.find()){
                        String script1=JSUnpacker.unpackJavaScript(matcher.group(0));
                        if(matcher.find()){
                            String script2=JSUnpacker.unpackJavaScript(matcher.group(0));
                            matcher=Pattern.compile("var [a-zA-Z0-9]+='([^']+)';").matcher(script1);
                            if(matcher.find()){
                                String cipherText=matcher.group(1);
                                matcher=Pattern.compile("var password='?([^']+)'?;var nBits='?([0-9]+)'?;").matcher(script2);
                                if(matcher.find()){
                                    String password=matcher.group(1);
                                    int nBits=Integer.parseInt(matcher.group(2));
                                    String openText=AES.AESDecryptCtr(cipherText, password, nBits);
                                    matcher=Pattern.compile("location\\.href=\\\\'(http.*)\\\\';").matcher(openText);
                                    if(matcher.find()){
                                        final HttpMethod method3=getMethodBuilder().setReferer(secondUrl).setAction(matcher.group(1)).toGetMethod();
                                        if (!tryDownloadAndSaveFile(method3)) {
                                            checkProblems();
                                            logger.warning(getContentAsString());
                                            throw new IOException("File input stream is empty.");
                                        }
                                    }else{
                                        throw new InvalidURLOrServiceProblemException("Final link not found");
                                    }                                    
                                }else{
                                throw new InvalidURLOrServiceProblemException("Bad format of second javascript");
                                }
                            }else{
                                throw new InvalidURLOrServiceProblemException("Bad format of first javascript");
                            }
                        }else{
                            throw new InvalidURLOrServiceProblemException("Second javascript not found");
                        }
                    }else{
                        throw new InvalidURLOrServiceProblemException("First javascript not found");
                    }                    
                } else throw new InvalidURLOrServiceProblemException("Cant connect to link in textarea");
                
            } else {                
                throw new InvalidURLOrServiceProblemException("Cant find download link in textarea");
            }
           } else throw new InvalidURLOrServiceProblemException("Cant load first link");
    }

    private void checkNameandSize(String content) throws Exception {

        if (!content.contains("indowebster.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (getContentAsString().contains("reported and removed")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Indowebster Error:</b><br>This files has been reported and removed due to terms of use violation"));
        }
        if (content.contains("File doesn")) {
            throw new URLNotAvailableAnymoreException("<b>Indowebster error:</b><br>File doesn't exist");
        }
        PlugUtils.checkName(httpFile, content, "<b>Original name : </b><!--INFOLINKS_ON--> ", "<!--INFOLINKS_OFF-->");
        PlugUtils.checkFileSize(httpFile, content, "<b>Size : </b>", "</div>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>Indowebster Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>Indowebster Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

}