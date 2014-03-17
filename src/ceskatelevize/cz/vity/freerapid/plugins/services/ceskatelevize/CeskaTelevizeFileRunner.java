package cz.vity.freerapid.plugins.services.ceskatelevize;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import java.util.HashMap;
import java.util.TreeSet;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class CeskaTelevizeFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(CeskaTelevizeFileRunner.class.getName());
    private CeskaTelevizeSettingsConfig config;
    private String videoSrc=null;
    private String base=null;

    private void setConfig() throws Exception {
        CeskaTelevizeServiceImpl service = (CeskaTelevizeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        checkNameAndSize();
    }

    private void checkNameAndSize() throws Exception {                 
        setConfig();
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request                        
           String callSoapParams=PlugUtils.getStringBetween(getContentAsString(), "callSOAP(", ");");
           ScriptEngineManager factory = new ScriptEngineManager();          
            ScriptEngine engine = factory.getEngineByName("JavaScript");
            HashMap<String,String> params=new HashMap<String,String>();
            engine.put("params", params);          
            try{
            engine.eval("function isArray(a){return Object.prototype.toString.apply(a) === '[object Array]';}; "
                    + "function walkObject(a,path){"
                    + "if(a==null) {"
                    + "walk('null',path);"
                    + "}"
                    + "for(var key in a){"
                    + "if(path==''){walk(a[key],key);}"
                    + " else {walk(a[key],path+'['+key+']');};"                    
                    + "}"
                    + "};"
                    + "function walk(a,path){"
                    + " if(isArray(a)) {walkArray(a,path);}"
                    + " else if(typeof a=='object'){ walkObject(a,path);}"
                    + " else params.put(path,''+a);"
                    + "}"
                    + "function walkArray(a,path){"
                    + "for(var i=0;i<a.length;i++){"
                    + " walk(a[i],path+'['+i+']');"
                    + "}"
                    + "}"
                    + "function callSOAP(obj){"
                    + "walkObject(obj,'');"
                    + "};"
                    + "callSOAP("+callSoapParams+");");
            }catch(Exception ex){
               throw new PluginImplementationException("Cannot get Playlist");
            }
            MethodBuilder mb=getMethodBuilder();
            mb.setReferer(fileURL).setAction("http://www.ceskatelevize.cz/ajax/playlistURL.php");            
            for(String key:params.keySet()){
               mb.setParameter(key, params.get(key));
            }            
            HttpMethod getPlayListMethod=mb.toPostMethod();
            getPlayListMethod.setRequestHeader("X-Requested-With", "X-Requested-With");
            getPlayListMethod.setRequestHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
            if(makeRedirectedRequest(getPlayListMethod)) {  
               if(!getMatcherAgainstContent("http.*").matches())
               {                    
                     throw new PluginImplementationException("Server returned invalid playlist URL");
               }
               String playlistURL=getContentAsString();
               final HttpMethod playlistMethod=new GetMethod(playlistURL);
               if(!makeRedirectedRequest(playlistMethod)){
                   throw new PluginImplementationException("Cannot connect to playlist");
               }
               Matcher switchMatcher=Pattern.compile("<switchItem id=\"([^\"]+)\" base=\"([^\"]+)\" begin=\"([^\"]+)\" duration=\"([^\"]+)\" clipBegin=\"([^\"]+)\">\\s*(<video[^>]*>\\s*)*</switchItem>",Pattern.MULTILINE+Pattern.DOTALL).matcher(getContentAsString());

               TreeSet<SwitchItem> body=new TreeSet<SwitchItem>();
               while(switchMatcher.find()){
                   SwitchItem newItem=new SwitchItem();
                   String swItemText=switchMatcher.group(0);
                   newItem.base=switchMatcher.group(2).replace("&amp;", "&");
                   newItem.duration=Double.parseDouble(switchMatcher.group(4));
                   Matcher videoMatcher=Pattern.compile("<video src=\"([^\"]+)\" system-bitrate=\"([0-9]+)\" label=\"([0-9]+)p\" enabled=\"true\" */>").matcher(swItemText);
                   while(videoMatcher.find()){
                       newItem.videos.add(new Video(videoMatcher.group(1),videoMatcher.group(3)));
                   }
                   body.add(newItem);
               }
               if(body.isEmpty()){
                   throw new PluginImplementationException("No stream found.");
               }
               SwitchItem selectedSwitch=body.first();
               base=selectedSwitch.base;

            int preferredQualityInt=config.getQualitySetting();

               videoSrc=null;
               String nearestHigherSrc=null;
               String nearestLowerSrc=null;
               int nearestHigher=0;
               int nearestLower=0;
               int highestQuality=0;
               String highestQualitySrc=null;
               int lowestQuality=0;
               String lowestQualitySrc=null;
               for(Video video:selectedSwitch.videos) {
                   int qualInt=Integer.parseInt(video.label);
                   if(qualInt>highestQuality){
                       highestQuality=qualInt;
                       highestQualitySrc=video.src;
                   }
                   if((lowestQuality==0)||(qualInt<lowestQuality)){
                       lowestQuality=qualInt;
                       lowestQualitySrc=video.src;
                   }
                   if(preferredQualityInt>0){
                           if(qualInt>preferredQualityInt){
                               if((nearestHigher==0)||(nearestHigher>qualInt)){
                                   nearestHigher=qualInt;
                                   nearestHigherSrc=video.src;
                               }
                           }
                           if(qualInt<preferredQualityInt){
                               if((nearestLower==0)||(nearestLower<qualInt)){
                                   nearestLower=qualInt;
                                   nearestLowerSrc=video.src;
                               }
                           }
                   }
                   if(qualInt==preferredQualityInt){
                       videoSrc=video.src;
                       break;
                   }
               }
               if(preferredQualityInt==-1){
                   videoSrc=lowestQualitySrc;
               }
               else
               if(preferredQualityInt==-2){
                   videoSrc=highestQualitySrc;
               }else
               if(videoSrc==null){
                   if(nearestLower!=0){
                       videoSrc=nearestLowerSrc;
                   }else if(nearestHigher!=0){
                       videoSrc=nearestHigherSrc;
                   }
               }

               if(videoSrc==null){
                   throw new PluginImplementationException("Cannot select preferred quality");
               }

               Matcher filenameMatcher=Pattern.compile("/([^/]+)\\....$").matcher(videoSrc);
               if(filenameMatcher.find()){
                   httpFile.setFileName(filenameMatcher.group(1)+".flv");
               }
            }else{
               throw new PluginImplementationException("Cannot load playlist URL");
            }
        }else{
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();        
        logger.info("Starting download in TASK " + fileURL);
        checkNameAndSize();

            RtmpSession rtmpSession=new RtmpSession(base, videoSrc);

            if (!tryDownloadAndSaveFile(rtmpSession)) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
            }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Neexistuj")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}