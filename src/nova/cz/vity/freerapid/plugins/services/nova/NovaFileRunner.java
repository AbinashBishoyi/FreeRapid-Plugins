package cz.vity.freerapid.plugins.services.nova;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.rtmp.RtmpClient;
import cz.vity.freerapid.plugins.webclient.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class NovaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NovaFileRunner.class.getName());
    private String movieName="";
    private int site_id=0;
    private int media_id=0;
    private String serverId="";

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
        media_id=PlugUtils.getNumberBetween(content, "var media_id = \"", "\";");
        site_id=PlugUtils.getNumberBetween(content, "var site_id = ", ";");
        final GetMethod method = getGetMethod("http://tn.nova.cz/bin/player/serve.php?site_id="+site_id+"&media_id="+media_id+"&userad_id=0&section_id=0&noad_count=0&fv=&session_id=0&ad_file=noad");
        makeRequest(method);
        content=getContentAsString();
        movieName=PlugUtils.getStringBetween(content, "src=\"", "\"");
        serverId=PlugUtils.getStringBetween(content, "server=\"", "\"");
        httpFile.setFileName(movieName.substring(movieName.lastIndexOf("/")+1)+".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    private boolean myTryDownloadAndSaveFile(final RtmpSession rtmpSession) throws Exception {
        httpFile.setState(DownloadState.GETTING);
        logger.info("Starting RTMP download");

        httpFile.getProperties().remove(DownloadClient.START_POSITION);
        httpFile.getProperties().remove(DownloadClient.SUPPOSE_TO_DOWNLOAD);

        final String fn = httpFile.getFileName();
        if (fn == null || fn.isEmpty())
            throw new IOException("No defined file name");
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(fn), "_"));

        client.getHTTPClient().getParams().setBooleanParameter("noContentLengthAvailable", true);


        RtmpClient rtmpClient = null;
        try {
            rtmpClient = new RtmpClient(rtmpSession);
            rtmpClient.connect();

            InputStream in = rtmpClient.getStream();

            if (in != null) {
                logger.info("Saving to file");
                downloadTask.saveToFile(in);
                return true;
            } else {
                logger.info("Saving file failed");
                return false;
            }
        } catch (Throwable e) {
            throw new PluginImplementationException("RTMP error (" + e.toString() + ")", e);
        } finally {
            if (rtmpClient != null) {
                try {
                    //no need to specifically close any streams, this method handles that too
                    rtmpClient.disconnect();
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
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

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction("http://tn.nova.cz/bin/player/config.php?site_id="+site_id+"&").toHttpMethod();
            if(!makeRequest(httpMethod)){
                throw new PluginImplementationException("Cannot connect to server list");
            }
            Matcher matcher=getMatcherAgainstContent("<flvserver id=\""+serverId+"\" url=\"([^\"]+)\"");
            if(!matcher.find()){
                throw new PluginImplementationException("Cannot find mediaURL");
            }
            String mediaURL=matcher.group(1);
            matcher=Pattern.compile("rtmp://([^:]+):([0-9]+)/(.*)$").matcher(mediaURL);
            if(!matcher.matches()){
                throw new PluginImplementationException("Not a RTMP server");
            }
            String server=matcher.group(1);
            int port=Integer.parseInt(matcher.group(2));
            String app=matcher.group(3);

            RtmpSession ses = new RtmpSession(server, port, app, movieName);


            //here is the download link extraction
            if (!myTryDownloadAndSaveFile(ses)) {
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