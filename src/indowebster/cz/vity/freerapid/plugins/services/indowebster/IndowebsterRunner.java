package cz.vity.freerapid.plugins.services.indowebster;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import javax.swing.JOptionPane;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

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
        
        String contentAsString = getContentAsString();
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
            MethodBuilder mb=getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download from IDWS");
            mb.setMethodAction(mb.getAction().replace("\n",""));
            final HttpMethod method2 = mb.toHttpMethod();
            
            if (makeRedirectedRequest(method2)){
                contentAsString = getContentAsString();
                Matcher matcher = PlugUtils.matcher("<div id=\"download[0-9]?\"><a href=\"javascript:\" onclick=\"location\\.href='([^']+)'", contentAsString);
                String nofind="<div id=\"hallo\">";
                String link=null;
                while(matcher.find()){
                  String before=contentAsString.substring(matcher.start()-nofind.length(),matcher.start());               
                  if(!before.equals(nofind)){
                    link=matcher.group(1);                  
                    break;
                  }
                }
                if(link!=null){
                    final GetMethod getMethod = getGetMethod(link);
                    if (!tryDownloadAndSaveFile(getMethod)) {
                        checkProblems();
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty.");
                    }
                }else throw new PluginImplementationException("Cant find download link 3");
            } else throw new PluginImplementationException("Cant find download link 2");
        } else throw new InvalidURLOrServiceProblemException("Cant load download link 1");
    }

    private void checkNameandSize(String content) throws Exception {

        if (!content.contains("indowebster.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("File doesn")) {
            throw new URLNotAvailableAnymoreException("<b>Indowebster error:</b><br>File doesn't exist");
        }
        PlugUtils.checkName(httpFile, content, "Original name : &quot;<!--INFOLINKS_ON--> ", "<!--INFOLINKS_OFF-->");
        PlugUtils.checkFileSize(httpFile, content, "<b>Size :</b> ", "</div>");
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