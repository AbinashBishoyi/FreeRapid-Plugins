package cz.vity.freerapid.plugins.services.vimeo;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class VimeoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VimeoFileRunner.class.getName());

    private String fileExtension;
    private String width;
    private String height;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        fileExtension = PlugUtils.getStringBetween(getContentAsString(), "<div class=\"file_extension\">", "</div>").toLowerCase(Locale.ENGLISH);
        if (!fileExtension.startsWith(".")) fileExtension = "." + fileExtension;

        final Matcher matcher = getMatcherAgainstContent("class=\"player\" style=\"width\\s*?:\\s*?(\\d+?)px;\\s*?height\\s*?:\\s*?(\\d+?)px;\"");
        if (!matcher.find()) throw new PluginImplementationException("Video dimensions not found");
        width = matcher.group(1);
        height = matcher.group(2);

        PlugUtils.checkName(httpFile, getContentAsString(), "<div class=\"title\">", "</div>");
        httpFile.setFileName(httpFile.getFileName() + fileExtension);

        PlugUtils.checkFileSize(httpFile, getContentAsString(), ">,\n<strong>", "</strong>");

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            if (!makeRedirectedRequest(getLoadMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            if (!tryDownloadAndSaveFile(getPlayMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod getLoadMethod() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("clip_id\\s*?:\\s*?'(.+?)'");
        if (!matcher.find()) throw new PluginImplementationException("'Clip ID' not found");
        final String clip = matcher.group(1).trim();

        matcher = getMatcherAgainstContent("context\\s*?:\\s*?'(.+?)'");
        if (!matcher.find()) throw new PluginImplementationException("'Context' not found");
        final String context = matcher.group(1).trim();

        matcher = getMatcherAgainstContent("(?s)new Moogaloop\\(.*?\\{(.+?)\\}.*?\\);");
        if (!matcher.find()) throw new PluginImplementationException("'Moogaloop' not found");
        final String moogaloop = matcher.group(1).trim();

        final Map<String, String> parameters = new TreeMap<String, String>();
        matcher = PlugUtils.matcher("(.+?)\\s*?:\\s*?'(.*?)'\\s*?,?", moogaloop);
        while (matcher.find()) {
            parameters.put("param_" + matcher.group(1).trim(), matcher.group(2).trim());
        }
        if (parameters.size() == 0) throw new PluginImplementationException("Parameters not found");

        final MethodBuilder mb = getMethodBuilder().setAction("http://vimeo.com/moogaloop/load/clip:" + clip + "/local/");
        mb.setParameter("moog_width", width).setParameter("moog_height", height).setParameter("embed_location", "");

        for (final Map.Entry<String, String> e : parameters.entrySet()) {
            mb.setParameter(e.getKey(), e.getValue());
        }

        mb.setParameter("context", context);

        return mb.toGetMethod();
    }

    private HttpMethod getPlayMethod() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<caption>", "</caption>");
        httpFile.setFileName(httpFile.getFileName() + fileExtension);

        final String clip = PlugUtils.getStringBetween(getContentAsString(), "<nodeId>", "</nodeId>");
        final String requestSignature = PlugUtils.getStringBetween(getContentAsString(), "<request_signature>", "</request_signature>");
        final String requestSignatureExpires = PlugUtils.getStringBetween(getContentAsString(), "<request_signature_expires>", "</request_signature_expires>");
        final String q = PlugUtils.getStringBetween(getContentAsString(), "<isHD>", "</isHD>").equals("1") ? "hd" : "sd";

        final MethodBuilder mb = getMethodBuilder().setAction("http://vimeo.com/moogaloop/play/clip:" + clip + "/" + requestSignature + "/" + requestSignatureExpires + "/");
        mb.setParameter("q", q).setParameter("type", "local").setParameter("embed_location", "");

        return mb.toGetMethod();
    }

}