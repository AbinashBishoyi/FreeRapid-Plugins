package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import java.io.UnsupportedEncodingException;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.net.URLDecoder;

/**
 * @author Kajda, JPEXS
 * @since 0.82
 */
class YouTubeFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(YouTubeFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.youtube.com";
    private YouTubeSettingsConfig config;
    private int fmt = 0;
    private String fileExtension = ".flv";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkSeriousProblems();
            checkName();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkAllProblems();
            setConfig();
            checkFmtParameter();
            checkName();
            
            
            String fmt_url_map=PlugUtils.getStringBetween(getContentAsString(), "\"fmt_url_map\": \"", "\"");
            fmt_url_map=URLDecoder.decode(fmt_url_map,"UTF-8");            
            Matcher matcher = PlugUtils.matcher(""+fmt+"\\|(http[^\\|]+)(,[0-9]+\\||$)",fmt_url_map);

            if (matcher.find()) {
                client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);
                getMethod = getGetMethod(matcher.group(1));
                if (!tryDownloadAndSaveFile(getMethod)) {
                        checkAllProblems();
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty");
                }
            } else {
                throw new PluginImplementationException("Cannot find specified video format("+fmt+")");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("class=\"errorBox\">((?:.|\\s)+?)</div");

        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(matcher.group(1));
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkName() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<h1 (?:dir='rtl')?>(.+?)</h1>");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim() + fileExtension;
            logger.info("File name " + fileName);
            httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(fileName), "_"));
        } else {
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void setConfig() throws Exception {
        YouTubeServiceImpl service = (YouTubeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void checkFmtParameter() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("fmt=(\\d+)", fileURL.toLowerCase());

        if (matcher.find()) {
            final String fmtCode = matcher.group(1);

            if (fmtCode.length() <= 2) {
                fmt=Integer.parseInt(fmtCode);
                setFileExtension(fmt);
            }
        } else {
            processConfig();
        }
    }

    private void processConfig() throws ErrorDuringDownloadingException {
        String fmt_map=PlugUtils.getStringBetween(getContentAsString(), "\"fmt_map\": \"", "\"");
        try {
            fmt_map=URLDecoder.decode(fmt_map, "UTF-8");
        } catch (UnsupportedEncodingException ex) {

        }
        String formats[]=fmt_map.split(",");
        int quality=config.getQualitySetting();
        if(quality>=formats.length) quality=formats.length-1;
        String selectedFormat=formats[formats.length-1-quality];
        fmt=Integer.parseInt(selectedFormat.substring(0,selectedFormat.indexOf("/")));
        setFileExtension(fmt);          
    }

    private void setFileExtension(int fmtCode) {
        switch (fmtCode) {            
            case 13:
            case 17:
                fileExtension = ".3gp";
                break;
            case 18:
            case 22:
                fileExtension = ".mp4";
                break;
        }
    }
}