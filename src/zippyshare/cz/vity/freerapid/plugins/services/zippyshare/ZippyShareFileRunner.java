package cz.vity.freerapid.plugins.services.zippyshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.plugins.webclient.utils.ScriptUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

/**
 * @author Vity+ntoskrnl+tonyk+CapCap
 */
class ZippyShareFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(ZippyShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
            final String url;
            Matcher matcher = getMatcherAgainstContent("document\\.getElementById\\([^\\(\\)]*?\\)\\.href[^<>]+=([^<>]+?/[^<>]+?\";)");
            if (matcher.find()) {
                final String script = matcher.group(1);
                logger.info("Evaluating script:\n" + script);
                url = "/d/" + String.valueOf(ScriptUtils.evaluateJavaScript(script));
            } else if (getContentAsString().contains("Recaptcha.create(")) {
                url = PlugUtils.getStringBetween(getContentAsString(), "document.location = '", "';");
                final String rcKey = PlugUtils.getStringBetween(getContentAsString(), "Recaptcha.create(\"", "\"");
                final String shortencode = PlugUtils.getStringBetween(getContentAsString(), "shortencode: '", "'");
                do {
                    if (!makeRedirectedRequest(stepCaptcha(rcKey, shortencode))) {
                        throw new ServiceConnectionProblemException();
                    }
                } while (!getContentAsString().contains("true"));
            } else {
                matcher = getMatcherAgainstContent("url\\s*:\\s*'(.+?)'");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Download link not found");
                }
                final String urlParam = matcher.group(1);
                matcher = getMatcherAgainstContent("seed\\s*:\\s*(\\d+)");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Seed parameter not found");
                }
                final int seed = Integer.parseInt(matcher.group(1));
                url = urlParam + "&time=" + getRequestValue(seed);
            }
            if (url == null) {
                throw new PluginImplementationException("Download URL not found");
            }
            logger.info("Download URL: " + url);
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("does not exist on this server")
                || contentAsString.contains("File has expired")
                || contentAsString.contains("<h1>HTTP Status")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkNameAndSize() throws Exception {
        Matcher matcher = getMatcherAgainstContent("document\\.getElementById\\([^\\(\\)]*?\\)\\.href[^<>]+/([^<>]+?)\";");
        if (matcher.find()) {
            httpFile.setFileName(URLDecoder.decode(matcher.group(1), "UTF-8"));
        } else {
            matcher = getMatcherAgainstContent("Name:\\s*?<[^<>]+?>\\s*?<[^<>]+?>([^<>]+?)<[^<>]+?>");
            if (!matcher.find()) {
                throw new PluginImplementationException("File name not found");
            }
            httpFile.setFileName(matcher.group(1));
        }
        matcher = getMatcherAgainstContent("Size:\\s*?<[^<>]+?>\\s*?<[^<>]+?>([^<>]+?)<[^<>]+?>");
        if (matcher.find()) {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
        } else {
            logger.info("File size not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha(final String rcKey, final String shortencode) throws Exception {
        final ReCaptcha r = new ReCaptcha(rcKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAjax()
                .setAction("/rest/captcha/test")
                .setParameter("challenge", r.getChallenge())
                .setParameter("response", r.getRecognized())
                .setParameter("shortencode", shortencode)
                .toPostMethod();
    }

    private int getRequestValue(final int seed) throws Exception {
        final HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("/swf/DownloadButton_v1.14s.swf")
                .toGetMethod();
        final InputStream is = client.makeRequestForFile(method);
        if (is == null) {
            throw new ServiceConnectionProblemException("Error downloading SWF");
        }
        final String swf = readSwfStreamToString(is);

        Matcher matcher = PlugUtils.matcher(Pattern.quote("\u0010\u0000\u002E\u0000\u0002") + "(.....)", swf);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing SWF (1)");
        }
        final int divider = (int) readAbcUInt(matcher.group(1).getBytes("ISO-8859-1"));

        matcher = PlugUtils.matcher(Pattern.quote("\u0024") + "(.)" + Pattern.quote("\u00D1\u00A2"), swf);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing SWF (2)");
        }
        final int multiplier = matcher.group(1).getBytes("ISO-8859-1")[0];

        return multiplier * seed % divider;
    }

    private static String readSwfStreamToString(InputStream is) throws IOException {
        try {
            final byte[] bytes = new byte[2048];
            if (readBytes(is, bytes, 8) != 8) {
                throw new IOException("Error reading from stream");
            }
            if (bytes[0] == 'C' && bytes[1] == 'W' && bytes[2] == 'S') {
                bytes[0] = 'F';
                is = new InflaterInputStream(is);
            } else if (bytes[0] != 'F' || bytes[1] != 'W' || bytes[2] != 'S') {
                throw new IOException("Invalid SWF stream");
            }
            final StringBuilder sb = new StringBuilder(8192);
            sb.append(new String(bytes, 0, 8, "ISO-8859-1"));
            int len;
            while ((len = is.read(bytes)) != -1) {
                sb.append(new String(bytes, 0, len, "ISO-8859-1"));
            }
            return sb.toString();
        } finally {
            try {
                is.close();
            } catch (final Exception e) {
                LogUtils.processException(logger, e);
            }
        }
    }

    private static int readBytes(InputStream is, byte[] buffer, int count) throws IOException {
        int read = 0, i;
        while (count > 0 && (i = is.read(buffer, 0, count)) != -1) {
            count -= i;
            read += i;
        }
        return read;
    }

    private static long readAbcUInt(final byte[] bytes) {
        assert bytes.length == 5;
        long b = bytes[0];
        b &= 0xFF;
        long u32 = b;
        if (!((u32 & 0x00000080) == 0x00000080))
            return u32;
        b = bytes[1];
        b &= 0xFF;
        u32 = u32 & 0x0000007f | b << 7;
        if (!((u32 & 0x00004000) == 0x00004000))
            return u32;
        b = bytes[2];
        b &= 0xFF;
        u32 = u32 & 0x00003fff | b << 14;
        if (!((u32 & 0x00200000) == 0x00200000))
            return u32;
        b = bytes[3];
        b &= 0xFF;
        u32 = u32 & 0x001fffff | b << 21;
        if (!((u32 & 0x10000000) == 0x10000000))
            return u32;
        b = bytes[4];
        b &= 0xFF;
        u32 = u32 & 0x0fffffff | b << 28;
        return u32;
    }

}