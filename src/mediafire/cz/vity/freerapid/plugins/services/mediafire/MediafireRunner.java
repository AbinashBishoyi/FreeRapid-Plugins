package cz.vity.freerapid.plugins.services.mediafire;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.mediafire.js.JsDocument;
import cz.vity.freerapid.plugins.services.mediafire.js.JsElement;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import sun.org.mozilla.javascript.internal.Context;
import sun.org.mozilla.javascript.internal.Scriptable;
import sun.org.mozilla.javascript.internal.ScriptableObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
public class MediafireRunner extends AbstractRunner {
    public final static Logger logger = Logger.getLogger(MediafireRunner.class.getName());

    private final static String SCRIPT_HEADER =
            "function alert(s) { Packages.cz.vity.freerapid.plugins.services.mediafire.MediafireRunner.logger.warning('JS: ' + s); }\n" +
                    "function aa(s) { alert(s); }\n" +
                    "var document = new JsDocument();\n" +
                    "function dummyparent() {\n" +
                    "    this.document = document;\n" +
                    "}\n" +
                    "var parent = new dummyparent();\n" +
                    "function jQuery() { }\n";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
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
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (isList()) {
                runList();
                return;
            }
            checkNameAndSize();
            while (getContentAsString().contains("dh('');")) { //handle password
                HttpMethod postPwd = getMethodBuilder()
                        .setReferer(fileURL)
                        .setBaseURL("http://www.mediafire.com/")
                        .setActionFromFormByName("form_password", true)
                        .setAndEncodeParameter("downloadp", getPassword())
                        .toPostMethod();
                if (!makeRedirectedRequest(postPwd)) {
                    throw new ServiceConnectionProblemException("Some issue while posting password");
                }
            }
            method = findDownloadUrl();
            setFileStreamContentTypes("text/plain");
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod findDownloadUrl() throws Exception {
        final List<String> elementsOnPage = findElementsOnPage();
        final Context context = Context.enter();
        try {
            final Scriptable scope = prepareContext(context);
            final HttpMethod method = findFirstUrl(context, scope);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            return findSecondUrl(elementsOnPage, context, scope);
        } finally {
            Context.exit();
        }
    }

    private Scriptable prepareContext(final Context context) throws ErrorDuringDownloadingException {
        final Scriptable scope = context.initStandardObjects();
        try {
            ScriptableObject.defineClass(scope, JsDocument.class);
            ScriptableObject.defineClass(scope, JsElement.class);
            context.evaluateString(scope, SCRIPT_HEADER, "<script>", 1, null);
        } catch (Exception e) {
            throw new PluginImplementationException("Script header execution failed", e);
        }
        return scope;
    }

    private List<String> findElementsOnPage() throws ErrorDuringDownloadingException {
        final List<String> list = new LinkedList<String>();
        final Matcher matcher = getMatcherAgainstContent("<div[^<>]*?id=\"([^\"]+?)\"[^<>]*?>Preparing download");
        while (matcher.find()) {
            list.add(matcher.group(1));
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("Element IDs not found");
        }
        return list;
    }

    private HttpMethod findFirstUrl(final Context context, final Scriptable scope) throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("(?s)value=\"download\">\\s*?<script[^<>]*?>(?:<!\\-\\-)?(.+?)</script>");
        if (!matcher.find()) {
            throw new PluginImplementationException("First JavaScript not found");
        }
        final String rawScript = matcher.group(1);
        //logger.info(rawScript);

        matcher = PlugUtils.matcher("function\\s*?[a-z\\d]+?\\(", rawScript);
        int lastFunctionIndex = -1;
        while (matcher.find()) {
            lastFunctionIndex = matcher.start();
        }
        if (lastFunctionIndex < 0) {
            logger.warning(rawScript);
            throw new PluginImplementationException("Last function in first JavaScript not found");
        }
        // gysl8luzk='';oq1w66x=unescape(......;   var cb=Math.random();
        //(.....................................) <-- this part is what we want
        matcher = PlugUtils.matcher("((?:eval\\(\")?[a-z\\d]+?\\s*?=\\s*?\\\\?'\\\\?';\\s*?[a-z\\d]+?\\s*?=\\s*?unescape\\(.+?;)[^;]+?Math\\.random\\(\\)", rawScript);
        if (!matcher.find(lastFunctionIndex)) {
            logger.warning(rawScript);
            throw new PluginImplementationException("Error parsing last function in first JavaScript");
        }
        final String partOfFunction = matcher.group(1);
        //logger.info(partOfFunction);

        final String preparedScript = rawScript.replace("setTimeout(", "return '/dynamic/download.php?qk=' + qk + '&pk1=' + pk1 + '&r=' + pKr; setTimeout(");
        final String script = new StringBuilder(preparedScript.length() + 1 + partOfFunction.length())
                .append(preparedScript)
                .append('\n')
                .append(partOfFunction)
                .toString();
        //logger.info(script);

        final String result;
        try {
            scope.put("pk", scope, 0);
            result = Context.toString(context.evaluateString(scope, script, "<script>", 1, null));
        } catch (Exception e) {
            logger.warning(script);
            throw new PluginImplementationException("Script 1 execution failed", e);
        }
        logger.info(result);
        return getMethodBuilder().setReferer(fileURL).setAction(result).toGetMethod();
    }

    private HttpMethod findSecondUrl(final List<String> elementsOnPage, final Context context, final Scriptable scope) throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("(?s)<script[^<>]*?>\\s*?(?!</script>)(?:<!\\-\\-)?(.+?)</script>");
        if (!matcher.find()) {
            throw new PluginImplementationException("Second JavaScript not found");
        }
        final String rawScript = matcher.group(1);
        //logger.info(rawScript);

        matcher = getMatcherAgainstContent("<body[^<>]*?onload=[\"'](.+?)[\"']");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing second page");
        }
        final String functionToCall = matcher.group(1);
        //logger.info(functionToCall);

        final String script = new StringBuilder(rawScript.length() + 1 + functionToCall.length())
                .append(rawScript)
                .append('\n')
                .append(functionToCall)
                .toString();
        //logger.info(script);

        final JsDocument document;
        try {
            context.evaluateString(scope, script, "<script>", 1, null);
            document = (JsDocument) scope.get("document", scope);
        } catch (Exception e) {
            logger.warning(script);
            throw new PluginImplementationException("Script 2 execution failed", e);
        }

        for (final String id : elementsOnPage) {
            final JsElement element = document.getElements().get(id);
            if (element != null && element.isVisible()) {
                return findThirdUrl(element.getText());
            }
        }
        throw new PluginImplementationException("Download link element not found");
    }

    private HttpMethod findThirdUrl(final String text) throws ErrorDuringDownloadingException {
        logger.info(text);
        return getMethodBuilder(text).setReferer(fileURL).setActionFromAHrefWhereATagContains("").toGetMethod();
    }

    private void checkNameAndSize() throws Exception {
        if (isList()) return;
        final String content = getContentAsString();
        PlugUtils.checkFileSize(httpFile, content, "sharedtabsfileinfo1-fs\" value=\"", "\">");
        PlugUtils.checkName(httpFile, content, "sharedtabsfileinfo1-fn\" value=\"", "\">");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The key you provided for file download was invalid")
                || content.contains("How can MediaFire help you?")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void runList() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("src=\"(/js/myfiles.php[^\"]+?)\"");
        if (!matcher.find()) throw new PluginImplementationException("URL to list not found");
        final HttpMethod listMethod = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();

        if (makeRedirectedRequest(listMethod)) {
            parseList();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void parseList() {
        final Matcher matcher = getMatcherAgainstContent("oe\\[[0-9]+\\]=Array\\('([^']+?)'");
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find()) {
            final String link = "http://www.mediafire.com/download.php?" + matcher.group(1);
            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private boolean isList() {
        return (fileURL.contains("?sharekey="));
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
