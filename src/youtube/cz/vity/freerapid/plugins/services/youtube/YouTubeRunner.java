package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda, JPEXS
 * @since 0.82
 */
class YouTubeFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(YouTubeFileRunner.class.getName());
    private YouTubeSettingsConfig config;
    private int fmt = 0;
    private String fileExtension = ".flv";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            setConfig();
            checkFmtParameter();
            checkName();

            String fmt_url_map = PlugUtils.getStringBetween(getContentAsString(), "&fmt_url_map=", "&");
            fmt_url_map = URLDecoder.decode(fmt_url_map, "UTF-8");
            Matcher matcher = PlugUtils.matcher("," + fmt + "\\|(http[^\\|]+)(,[0-9]+\\||$)", "," + fmt_url_map);

            if (matcher.find()) {
                client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);
                getMethod = getGetMethod(matcher.group(1));
                if (!tryDownloadAndSaveFile(getMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                throw new PluginImplementationException("Cannot find specified video format (" + fmt + ")");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("class=\"errorBox\">((?:.|\\s)+?)</div");

        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(matcher.group(1));
        }
    }

    private void checkName() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<meta name=\"title\" content=\"(.+?)\">");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim() + fileExtension;
            logger.info("File name " + fileName);
            httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(fileName), "_"));
        } else {
            throw new PluginImplementationException("File name was not found");
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void setConfig() throws Exception {
        YouTubeServiceImpl service = (YouTubeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void checkFmtParameter() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("fmt=(\\d+)", fileURL.toLowerCase(Locale.ENGLISH));

        if (matcher.find()) {
            final String fmtCode = matcher.group(1);

            if (fmtCode.length() <= 2) {
                fmt = Integer.parseInt(fmtCode);
                setFileExtension(fmt);
            }
        } else {
            processConfig();
        }
    }

    private void processConfig() throws ErrorDuringDownloadingException {
        String fmt_map = PlugUtils.getStringBetween(getContentAsString(), "&fmt_map=", "&");
        try {
            fmt_map = URLDecoder.decode(fmt_map, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LogUtils.processException(logger, e);
        }
        String formats[] = fmt_map.split(",");
        int quality = config.getQualitySetting();
        if (quality == 4) quality = formats.length - 1; //maximum available
        if (quality >= formats.length) quality = formats.length - 1;
        String selectedFormat = formats[formats.length - 1 - quality];
        fmt = Integer.parseInt(selectedFormat.substring(0, selectedFormat.indexOf("/")));
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