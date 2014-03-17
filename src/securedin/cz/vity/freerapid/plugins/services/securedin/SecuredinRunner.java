package cz.vity.freerapid.plugins.services.securedin;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.securedin.cypher.Cypher;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 */
class SecuredinRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SecuredinRunner.class.getName());

    final private List<URI> queye = new LinkedList<URI>();

    private static final String HTTP_BASE = "http://secured.in/";
    private static final String HTTP_AJAX = HTTP_BASE + "/ajax-handler.php";
    private static final String HTTP_JAVASCRIPT = "/resources/cypher.js";

    @Override
    public void run() throws Exception {
        super.run();

        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        logger.info(fileURL);
        Matcher matcher;
        if (!makeRequest(method)) {
            throw new PluginImplementationException("Can't load main page");
        }
        String content = getContentAsString();
        while( content.contains("captcha_hash") ) {
            matcher = PlugUtils.matcher("<input name=\"captcha_hash\".*value=\"(.*)\"", content);
            if (matcher.find()) {
                String captchaHash = matcher.group(1);
                matcher = PlugUtils.matcher("<img src=\"(captcha-[^\"]+)", content);
                if (matcher.find()) {
                    String captchaImg = HTTP_BASE + matcher.group(1);
                    logger.info("Captcha image = " + captchaImg);
                    String captchaKey = getCaptchaSupport().getCaptcha(captchaImg);
                    if (captchaKey != null) {
                        PostMethod pMethod = getPostMethod(fileURL);
                        pMethod.addParameter("captcha_key", captchaKey);
                        pMethod.addParameter("captcha_hash", captchaHash);
                        if (!makeRequest(pMethod)) {
                            throw new PluginImplementationException("Error posting Captcha Code ! Please Retry !");
                        } else
                            content = getContentAsString();
                    } else {
                        throw new PluginImplementationException("Can't load captcha image");
                    }
                } else {
                    throw new PluginImplementationException("Can't find captcha image");
                }
            }
        }
        matcher = PlugUtils.matcher("accessDownload\\([\\w|,|\\s]+\\'([\\w|-]+)\\'", content);
        while (matcher.find()) {
            doAjaxCommand(matcher.group(1));
        }

        httpFile.setState(DownloadState.COMPLETED);
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, queye);
    }

    private void doAjaxCommand(String cmdParameter) throws Exception {
        final PostMethod pMethod = getPostMethod(HTTP_AJAX);
        pMethod.addParameter("cmd", "download");
        pMethod.addParameter("download_id", cmdParameter);
        if (makeRequest(pMethod)) {
            String content = getContentAsString();

            String newUrl = Cypher.cypher(content);
            queye.add(new URI(newUrl));
        } else
            throw new PluginImplementationException("Can't load ajax command");

    }

    /**
     * Method to decypher URI, with javascript from site.
     *
     * @param args arguments for application
     *
     * TODO: I don't know, why not work with GetMethod jsMethod = getGetMethod(HTTP_JAVASCRIPT );
     *       if (makeRequest(jsMethod)) { String jsContent = jsMethod.getResponseBodyAsString(); }
     *
     * @deprecated
     */
    private String cypher(String key) throws PluginImplementationException {
        try {
            InputStream jsStream = this.getClass().getResourceAsStream(HTTP_JAVASCRIPT);
            InputStreamReader jsReader = new InputStreamReader(jsStream);
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("JavaScript");
            engine.eval(jsReader);
            Invocable inv = (Invocable) engine;
            String newUrl = inv.invokeFunction("cypher", key).toString();
            return newUrl.trim();
        } catch (NoSuchMethodException e) {
            throw new PluginImplementationException("Can't load ajax command", e);
        } catch (ScriptException e) {
            throw new PluginImplementationException("Can't load ajax command", e);
        }

    }
}
