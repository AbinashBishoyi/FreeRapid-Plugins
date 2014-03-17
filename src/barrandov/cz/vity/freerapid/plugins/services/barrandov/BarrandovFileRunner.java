package cz.vity.freerapid.plugins.services.barrandov;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class BarrandovFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(BarrandovFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        checkNameAndSize();
    }

    private String streamName;
    private String server;
    private int videoId;
    private String appName;


    private void checkNameAndSize() throws Exception {
        Matcher m=Pattern.compile(".*barrandov\\.tv/([0-9]+)\\-.*").matcher(fileURL);
        if(!m.matches()) throw new PluginImplementationException("Bad link format");

        videoId=Integer.parseInt(m.group(1));
        Random r=new Random();
        final GetMethod playerDataMethod = getGetMethod("http://www.barrandov.tv/special/videoplayerdata/"+videoId+"?r="+r.nextInt(65536));
        if(!makeRedirectedRequest(playerDataMethod)) throw new PluginImplementationException("Cannot load video data");

        String hostName=PlugUtils.getStringBetween(getContentAsString(), "<hostname>", "</hostname>");
        streamName=PlugUtils.getStringBetween(getContentAsString(), "<streamname>", "</streamname>");
        server=hostName.substring(0,hostName.indexOf("/"));
        appName=hostName.substring(hostName.indexOf("/")+1);
        String parts[]=streamName.split(":");
        if(parts.length>1) {
            String name=parts[1];
            if(name.endsWith(".f4v")) name=name.substring(0,name.length()-4)+".flv";
            httpFile.setFileName(name);
        }
        else {
          String name=parts[0];  
          httpFile.setFileName(name);
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        checkNameAndSize();
        RtmpSession ses = new RtmpSession(server, 1935, appName, streamName);
        if (!tryDownloadAndSaveFile(ses)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        
    }

}