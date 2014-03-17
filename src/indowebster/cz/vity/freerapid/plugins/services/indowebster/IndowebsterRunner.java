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
import org.apache.commons.codec.binary.Base64;


import java.io.IOException;
import java.util.ArrayList;
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
            String action=mb.getAction();
            action=action.replace("\n","");
            action=action.replace("\t","");
            action=action.replace("\r","");
            action=action.replace(" ","%20");
            mb.setMethodAction(action);
            final HttpMethod method2 = mb.toHttpMethod();                        
            if (makeRedirectedRequest(method2)){
                ArrayList<String> linksList=new ArrayList<String>();
                contentAsString = getContentAsString();
                Matcher matcher = PlugUtils.matcher("<div id=\"download[0-9]*\"><a href=\"javascript:\" onclick=\"location\\.href='([^']+)'", contentAsString);
                String link=null;
                while(matcher.find()){
                  Matcher matcherPreviousTag = PlugUtils.matcher("<([a-z]+)( [^>]*)?>[^<]*"+Pattern.quote(matcher.group()), contentAsString);
                  if(matcherPreviousTag.find()){
                    String tagName=matcherPreviousTag.group(1);
                    String tagParams=matcherPreviousTag.group(2);
                    Matcher matcherId=PlugUtils.matcher("id=\"([^\"]+)\"",tagParams);
                    Matcher matcherClass=PlugUtils.matcher("class=\"([^\"]+)\"",tagParams);
                    if(matcherId.find()){
                        Matcher matcherStyleId = PlugUtils.matcher("("+tagName+")?"+"#"+matcherId.group(1)+"(,[^\\{]*)?\\s*\\{[^\\}]*display\\s*:\\s*none;[^\\}]*\\}", contentAsString);
                        linksList.add(matcherId.group(1)+"|"+matcher.group(1)); //adds id|url to the list
                        if(matcherStyleId.find()) continue;
                    }
                    if(matcherClass.find()){
                        Matcher matcherStyleClass = PlugUtils.matcher("("+tagName+")?"+"."+matcherClass.group(1)+"(,[^\\{]*)?\\s*\\{[^\\}]*display\\s*:\\s*none;[^\\}]*\\}", contentAsString);
                        if(matcherStyleClass.find()) continue;
                    }
                  }
                  logger.info("Found normal link");
                  link=matcher.group(1);
                  break;
                }
                if(link==null){
                    Matcher matcherLi=getMatcherAgainstContent("<li><a href=\"#idws7\" onclick=\"location.href='([^']+)'");
                    if(matcherLi.find()){
                        link=matcherLi.group(1);
                        logger.info("Found link in li tag");
                    }
                }
                if(link==null){
                    Matcher tempMatcher=getMatcherAgainstContent("temp=\"([^\"]+)\"");
                    if(tempMatcher.find()){
                        String temp=tempMatcher.group(1);
                        linksearch:for(int i=0;i<5;i++){
                            temp=new String(Base64.decodeBase64(temp.getBytes()));
                            for(String s:linksList){
                                if(s.startsWith(temp+"|")){
                                    link=s.substring(s.indexOf("|")+1);
                                    logger.info("Found base64 coded link");
                                    break linksearch;
                                }
                            }
                        }
                    }
                }                
                if(link!=null){
                    logger.info("Final download link:"+link);
                    final GetMethod getMethod = getGetMethod(link);
                    if (!tryDownloadAndSaveFile(getMethod)) {
                        checkProblems();
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty.");
                    }
                }else throw new PluginImplementationException("Cant find final download link");
            } else throw new PluginImplementationException("Cant find download link 2");
        } else throw new InvalidURLOrServiceProblemException("Cant load download link 1");
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