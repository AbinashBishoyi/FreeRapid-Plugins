package cz.vity.freerapid.plugins.services.nowdownload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class NowDownloadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NowDownloadFileRunner.class.getName());

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

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("Downloading</span> <br>\\s*?(.+?)\\s+?(.+?)\\s*?</h4>", content);
        if (!match.find())
            throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);//extract file name and size from the page
            HttpMethod httpMethod;
            try {                   // .eu
                httpMethod = getMethodBuilder().setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains("Download Now").toHttpMethod();
            } catch (Exception e) {  // .ch
                final Matcher match = PlugUtils.matcher("eval\\((.+?)\\s*?</script>", content);
                if (!match.find()) throw new PluginImplementationException("script not found");
                String contents = evalScript(match.group(1), "Download your file");

                httpMethod = getMethodBuilder(contents).setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains("Download your file").toHttpMethod();
                final int wait = PlugUtils.getNumberBetween(contents, "var ll=", ";");
                downloadTask.sleep(wait + 1);
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException("Error finding download");
                }
                httpMethod = getMethodBuilder().setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains("Click here to download").toHttpMethod();
            }
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("This file does not exist") || contentAsString.contains("The file is being transfered")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private String evalScript(final String content, final String breakString) throws Exception {
        final Matcher match2 = PlugUtils.matcher("function\\((.+?)\\)\\{(.+?)\\}\\((.+?)\\)\\);", content);
        while (match2.find()) {
            final String clVars = match2.group(1);
            final String sFuncts = match2.group(2).replace("return", "OUTPUT=");
            final String clVals = match2.group(3);

            final String aVars[] = clVars.split(",");
            final String aVals[] = clVals.split(",");
            String setVarVals = "";
            for (int iPos = 0; iPos < aVars.length; iPos++) {
                setVarVals += aVars[iPos] + "=" + aVals[iPos] + ";";
            }
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("JavaScript");
            engine.eval(setVarVals + sFuncts).toString();

            String output = (String) engine.get("OUTPUT");
            output = output.replaceAll("\n", " ").replaceAll("\r", " ");

            if (output.contains(breakString)) {
                return output;
            } else if (output.contains("eval")) {
                final String out = evalScript(output, breakString);
                if (out != null)
                    return out;
            }
        }
        return null;
    }

}