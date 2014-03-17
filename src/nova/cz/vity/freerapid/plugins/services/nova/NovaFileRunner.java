package cz.vity.freerapid.plugins.services.nova;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class NovaFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(NovaFileRunner.class.getName());
    private String movieName = "";
    private String media_id = "";
    private static final String timeServiceUrl = "http://tn.nova.cz/lbin/time.php";
    private static final String secret="tajne.heslo";
    private static final String serviceUrl="http://master-ng.nacevi.cz/cdn.server/PlayerLink.ashx";
    private static final String appId="nova-vod";
    private String time="";
    private HashMap<String,String> mediaList=new HashMap<String,String>();
    private String baseUrl="";
    private NovaSettingsConfig config;
    
    private void setConfig() throws Exception {
        NovaServiceImpl service = (NovaServiceImpl) getPluginService();
        config = service.getConfig();
    }
    

    private String getTimeString() throws Exception {
       if(!makeRequest(getGetMethod(timeServiceUrl))){
          throw new PluginImplementationException("Time service not available");
       }
       return getContentAsString();
    }
    
    private String getHashString(String media_id){
       return appId+"|"+media_id+"|"+time+"|"+secret;
    }
    
    private static byte[] md5ba(String data) {
        String ret = "";
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(data.getBytes());
            return digest.digest();         
        } catch (NoSuchAlgorithmException ex) {
        }
        return null;
    }    
    
    private void getMediaList() throws Exception{
       time=getTimeString().substring(0,14);
       byte md5BA[]=md5ba(getHashString(media_id));
       String base64FromBA=new String(Base64.encodeBase64(md5BA));
       HttpMethod method=getMethodBuilder()
               .setAction(serviceUrl)
               .setParameter("c", appId+"|"+media_id)
               .setParameter("h", "0")
               .setParameter("t", time)
               .setParameter("s", base64FromBA)
               .setParameter("tm", "nova")
               .setParameter("d", "1")
               .setEncodeParameters(true)
               .setEncoding("utf-8")
               .toGetMethod();
       if(!makeRedirectedRequest(method))
       {
          throw new PluginImplementationException("Cannot get media list");
       }
       String content=getContentAsString();
       if(content.contains("<status>Ok</status> ")){
          baseUrl=PlugUtils.getStringBetween(content, "<baseUrl>", "</baseUrl>");
          String mediaListStr=content.substring(content.indexOf("<mediaList>")+"<mediaList>".length(),content.indexOf("</mediaList>"));
          while(mediaListStr.indexOf("<media>")>-1){
             String media=mediaListStr.substring(mediaListStr.indexOf("<media>")+"<media>".length(),mediaListStr.indexOf("</media>"));
             mediaListStr=mediaListStr.substring(mediaListStr.indexOf("</media>")+"</media>".length());
             String quality=PlugUtils.getStringBetween(media, "<quality>", "</quality>");
             String url=PlugUtils.getStringBetween(media, "<url>", "</url>");
             logger.log(Level.INFO, "Found media:  quality={0} url={1}", new Object[]{quality, url});
             mediaList.put(quality, url);
          }
       }else{
          throw new PluginImplementationException("Error while getting media list");
       }
    }
    
    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws Exception {
        setConfig();
        media_id = ""+PlugUtils.getNumberBetween(content, "var media_id = \"", "\";");
        getMediaList();
        movieName=null;
        String qualityMap[]=new String[]{"lq","hq"};
        String quality=qualityMap[config.getQualitySetting()];
        movieName=mediaList.get(quality);
        if(movieName!=null){
           logger.log(Level.INFO, "Prepared to download {0} version", quality);
           String fileName=movieName.substring(movieName.lastIndexOf("/") + 1);
           if(fileName.endsWith(".mp4")){
              fileName=fileName.substring(0,fileName.length()-4);
           }
           if(!fileName.endsWith(".flv")){
              fileName+=".flv";
           }
           httpFile.setFileName(fileName);
           httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }else{
           throw new PluginImplementationException("Cannot find media of appropriate quality");
        }
        
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response            
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            
            Matcher matcher = Pattern.compile("rtmp://([^:]+):([0-9]+)/(.*)$").matcher(baseUrl);
            if (!matcher.matches()) {
                throw new PluginImplementationException("Not a RTMP server");
            }
            String server = matcher.group(1);
            int port = Integer.parseInt(matcher.group(2));
            String app = matcher.group(3);

            RtmpSession ses = new RtmpSession(server, port, app, movieName);
                                    
            if (!tryDownloadAndSaveFile(ses)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}