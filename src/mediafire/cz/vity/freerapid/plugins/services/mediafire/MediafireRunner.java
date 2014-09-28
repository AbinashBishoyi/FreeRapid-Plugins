package cz.vity.freerapid.plugins.services.mediafire;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.services.solvemediacaptcha.SolveMediaCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
class MediafireRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MediafireRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkUrl();
        final HttpMethod method = getGetMethod(fileURL);
        int httpStatus = client.makeRequest(method, false);
        if (httpStatus == 301) { // permanent redirection to html page
            httpStatus = client.makeRequest(method, true);
        } else if (httpStatus / 100 == 3) { // redirection to file
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            return;
        }
        if (httpStatus == 200) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkUrl() {
        // HTTPS works but redirects to HTTP in browser
        fileURL = fileURL.replaceFirst("https:", "http:");
    }

    private void checkNameAndSize() throws Exception {
        if (isFolder()) {
            final String id = fileURL.substring(fileURL.indexOf('?') + 1);
            final HttpMethod method = getMethodBuilder()
                    .setAction("http://www.mediafire.com/api/folder/get_info.php")
                    .setParameter("folder_key", id)
                    .setParameter("response_format", "json")
                    .setParameter("rand", "" + (int) (10000 * Math.random()))
                    .toGetMethod();
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String name = PlugUtils.unescapeUnicode(PlugUtils.getStringBetween(getContentAsString(), "\"name\":\"", "\","));
            final long contents = Long.parseLong(PlugUtils.getStringBetween(getContentAsString(), "\"file_count\":\"", "\",")) +
                    Long.parseLong(PlugUtils.getStringBetween(getContentAsString(), "\"folder_count\":\"", "\","));
            httpFile.setFileName("Folder: " + name + " >");
            httpFile.setFileSize(contents);

        } else {
            PlugUtils.checkName(httpFile, getContentAsString(), "<div class=\"fileName\">", "</div>");
            final Matcher matcher = getMatcherAgainstContent("oFileSharePopup\\.ald\\('.+?','.+?','(\\d+?)'");
            if (!matcher.find()) {
                throw new PluginImplementationException("File size not found");
            }
            httpFile.setFileSize(Long.parseLong(matcher.group(1)));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The key you provided for file download")
                || content.contains("How can MediaFire help you?")
                || content.contains("File Removed for Violation")
                || content.contains("File Belongs to Suspended Account")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("Unknown or invalid Folder")) {
            throw new URLNotAvailableAnymoreException("Folder not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        while (true) {
            /**
             * Grab the current captcha state for use after the initial request.
             *
             * Getting the state and loading initial page should actually be an
             * atomic operation for strict thread safety, but getting the wrong
             * state only results in one redundant page load in the worst case.
             */
            final CaptchaState captchaState = CaptchaState.getFor(client.getSettings());

            final HttpMethod method = getGetMethod(fileURL);

            int httpStatus = client.makeRequest(method, false);
            if (httpStatus == 301) { // permanent redirection to html page
                httpStatus = client.makeRequest(method, true);
            } else if (httpStatus / 100 == 3) {  // redirection to file
                setFileStreamContentTypes("text/plain");
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
                return;
            }
            if (httpStatus != 200) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            if (isFolder()) {
                parseFolder();
                return;
            }
            checkNameAndSize();

            stepPassword();

            if (!isCaptcha()) {
                break;
            }
            if (captchaState.setSolved()) {
                /**
                 * We were the first to notice the captcha.
                 * Solve it and signal others that we did so.
                 */
                try {
                    stepCaptcha();
                    break;
                } finally {
                    CaptchaState.removeFor(client.getSettings());
                    captchaState.signalSolved();
                }
            } else {
                /**
                 * Somebody else is already solving the captcha.
                 * Wait for that and reload the page.
                 */
                captchaState.awaitSolved();
            }
        }

        final HttpMethod method = getMethodBuilder().setActionFromTextBetween("kNO = \"", "\";").toGetMethod();
        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    /**
     * MediaFire asks for a captcha if it detects a lot of downloads from an IP.
     * The captcha has to be solved only once; after that, several downloads can
     * proceed normally without entering further captchas. These states are used
     * to ensure that a captcha only has to be solved once per IP, and that
     * downloads without a captcha can still be processed in parallel.
     */
    private static class CaptchaState {
        private final static Map<ConnectionSettings, CaptchaState> STATES = new WeakHashMap<ConnectionSettings, CaptchaState>(1);

        private final AtomicBoolean solved = new AtomicBoolean();
        private final CountDownLatch latch = new CountDownLatch(1);

        public static CaptchaState getFor(final ConnectionSettings connectionSettings) {
            synchronized (STATES) {
                CaptchaState state = STATES.get(connectionSettings);
                if (state == null) {
                    state = new CaptchaState();
                    STATES.put(connectionSettings, state);
                }
                return state;
            }
        }

        public static void removeFor(final ConnectionSettings connectionSettings) {
            synchronized (STATES) {
                STATES.remove(connectionSettings);
            }
        }

        /**
         * Sets the state of this captcha to solved.
         * Further invocations of this method will return false.
         *
         * @return true if this invocation changed the state to solved,
         *         false if the state was already solved
         */
        public boolean setSolved() {
            return !solved.getAndSet(true);
        }

        public void signalSolved() {
            latch.countDown();
        }

        public void awaitSolved() throws InterruptedException {
            latch.await();
        }
    }

    private boolean isFolder() {
        return getContentAsString().contains("<body class=\"myfiles");
    }

    private void parseFolder() throws Exception {
        final String id = fileURL.substring(fileURL.indexOf('?') + 1);
        final List<FolderItem> list = new LinkedList<FolderItem>();
        if (id.contains(",")) {
            for (final String s : id.split(",")) {
                list.add(new FolderItem(s, null));
            }
        } else {
            // get sub-folders
            final HttpMethod method1 = getMethodBuilder()
                    .setAction("http://www.mediafire.com/api/folder/get_content.php")
                    .setParameter("r", "bdmz")
                    .setParameter("content_type", "folders")
                    .setParameter("filter", "all")
                    .setParameter("folder_key", id)
                    .setParameter("order_by", "name")
                    .setParameter("order_direction", "asc")
                    .setParameter("response_format", "json")
                    .setParameter("version", "2")
                    .toGetMethod();
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            if (!makeRedirectedRequest(method1)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final Matcher matcher1 = getMatcherAgainstContent("\"folderkey\":\"(.+?)\",.*?\"name\":\"(.+?)\"");
            while (matcher1.find()) {
                list.add(new FolderItem(matcher1.group(1), matcher1.group(2)));
            }
            // get files
            final HttpMethod method = getMethodBuilder()
                    .setAction("http://www.mediafire.com/api/folder/get_content.php")
                    .setParameter("r", "ying")
                    .setParameter("content_type", "files")
                    .setParameter("filter", "all")
                    .setParameter("folder_key", id)
                    .setParameter("order_by", "name")
                    .setParameter("order_direction", "asc")
                    .setParameter("response_format", "json")
                    .setParameter("version", "2")
                    .toGetMethod();
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final Matcher matcher = getMatcherAgainstContent("\"quickkey\":\"(.+?)\",.*?\"filename\":\"(.+?)\"");
            while (matcher.find()) {
                list.add(new FolderItem(matcher.group(1), matcher.group(2)));
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        final List<URI> uriList = new LinkedList<URI>();
        for (final FolderItem item : list) {
            try {
                uriList.add(new URI(item.getFileUrl()));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        httpFile.getProperties().put("removeCompleted", true);
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private static class FolderItem implements Comparable<FolderItem> {
        private final String fileId;
        private final String fileName;

        public FolderItem(final String fileId, final String fileName) {
            this.fileId = fileId;
            this.fileName = fileName;
        }

        public String getFileUrl() {
            return "http://www.mediafire.com/?" + fileId;
        }

        @Override
        public int compareTo(final FolderItem that) {
            return this.fileName.compareTo(that.fileName);
        }
    }

    private boolean isCaptcha() {
        return getContentAsString().contains("\"form_captcha\"");
    }

    private void stepCaptcha() throws Exception {
        while (isCaptcha()) {
            final String content = getContentAsString();
            MethodBuilder builder = getMethodBuilder(content)
                    .setReferer(fileURL)
                    .setActionFromFormByName("form_captcha", true);
            if (content.contains("solvemedia")) {
                final Matcher matcher = getMatcherAgainstContent("challenge.noscript\\?k=([^\"]+)");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Captcha key not found");
                }
                final String captchaKey = matcher.group(1);
                final SolveMediaCaptcha solveMediaCaptcha = new SolveMediaCaptcha(captchaKey, client, getCaptchaSupport());
                solveMediaCaptcha.askForCaptcha();
                builder = solveMediaCaptcha.modifyResponseMethod(builder);
            } else {  // ReCaptcha
                final Matcher matcher = getMatcherAgainstContent("challenge\\?k=([^\"]+)");
                if (!matcher.find()) {
                    throw new PluginImplementationException("ReCaptcha key not found");
                }
                final ReCaptcha r = new ReCaptcha(matcher.group(1), client);
                final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                }
                r.setRecognized(captcha);
                builder = r.modifyResponseMethod(builder);
            }
            if (!makeRedirectedRequest(builder.toPostMethod())) {
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private boolean isPassworded() {
        return getContentAsString().contains("\"form_password\"");
    }

    private void stepPassword() throws Exception {
        while (isPassworded()) {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormByName("form_password", true)
                    .setParameter("downloadp", getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private String getPassword() throws Exception {
        final String password = getDialogSupport().askForPassword("MediaFire");
        if (password == null) {
            throw new NotRecoverableDownloadException("This file is secured with a password");
        } else {
            return password;
        }
    }

}
