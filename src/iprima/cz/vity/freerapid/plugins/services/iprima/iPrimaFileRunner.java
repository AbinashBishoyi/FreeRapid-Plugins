package cz.vity.freerapid.plugins.services.iprima;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import java.io.*;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import org.apache.commons.httpclient.Header;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class iPrimaFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(iPrimaFileRunner.class.getName());
    private iPrimaSettingsConfig config;
    private String streamName;
    
    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }
    
    private void setConfig() throws Exception {
        iPrimaServiceImpl service = (iPrimaServiceImpl) getPluginService();
        config = service.getConfig();
    }
 

    private void checkNameAndSize(String content) throws Exception {
        setConfig();
        int quality=config.getQualitySetting();   
        switch(quality)
        {
           case 0:
              streamName=PlugUtils.getStringBetween(content, "'lq_id':'", "'");
              break;
           case 1: 
              streamName=PlugUtils.getStringBetween(content, "'hq_id':'", "'");
              break; 
        }
        httpFile.setFileName(streamName);        
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    
    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            Random rnd=new Random();
            HttpMethod m2=getMethodBuilder().setReferer(fileURL).setAction("http://embed.livebox.cz/iprimaplay/player-embed-v2.js?__tok"+rnd.nextInt(1073741824) +"__="+rnd.nextInt(1073741824)).toGetMethod();        
            System.out.println(m2.getURI());
            m2.setRequestHeader("Accept-Encoding", ""); //FRD nerozbali gzip v content typu application/x-javascript
            client.getHTTPClient().executeMethod(m2);
               String content=m2.getResponseBodyAsString();
               String playName=streamName;
               if(playName.endsWith(".mp4"))
               {
                  playName="mp4:"+streamName;
               }
               RtmpSession rtmpSession = new RtmpSession("rtmp://bcastmw.livebox.cz:80/iprima_token?auth="+PlugUtils.getStringBetween(content, "'?auth=", "';"),playName);
               rtmpSession.getConnectParams().put("pageUrl", fileURL);
               rtmpSession.getConnectParams().put("swfUrl", "http://embed.livebox.cz/iprimaplay/flash/LiveboxPlayer.swf?nocache="+(new Date()).getTime());
               if (!tryDownloadAndSaveFile(rtmpSession)) {
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