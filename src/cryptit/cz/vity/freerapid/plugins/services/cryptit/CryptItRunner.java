package cz.vity.freerapid.plugins.services.cryptit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.methods.PostMethod;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * @author RickCL
 */
class CryptItRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CryptItRunner.class.getName());

    final private List<URI> queye = new LinkedList<URI>();

    private static final String HTTP_BASE = "http://crypt-it.com/";
    private static final String HTTP_ENGINE = HTTP_BASE + "/engine/";

    byte[] b = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x11, 0x63, 0x72, 0x79, 0x70, 0x74, 0x69, 0x74,
            0x32, 0x2e, 0x67, 0x65, 0x74, 0x46, 0x69, 0x6c, 0x65, 0x73, 0x00, 0x02, 0x2f, 0x31, 0x00, 0x00, 0x00, 0x11,
            0x0a, 0x00, 0x00, 0x00, 0x02, 0x02, 0x00, 0x06 };
    byte[] b2 = new byte[] { 0x02, 0x00, 0x00 };

    private String cryptitcod;

    @Override
    public void run() throws Exception {
        super.run();

        logger.info("Starting run task " + fileURL);
        Matcher matcher = PlugUtils.matcher("(?:http|ccf)://[.]*crypt-it.com/[s|e|d|c|a]/([a-zA-Z0-9]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Wrong Page !!!");
        }
        cryptitcod = matcher.group(1);

        final PostMethod pMethod = getPostMethod(HTTP_ENGINE);
        pMethod.addRequestHeader("Content-Type", "application/x-amf");
        pMethod.setRequestBody(new String(b) + cryptitcod + new String(b2));

        InputStream icontent = client.makeRequestForFile(pMethod);
        if (icontent == null) {
            throw new PluginImplementationException("Can't load main page !!!");
        }

        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(icontent));
        String line = null;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        if( content.length() == 0) {
            throw new PluginImplementationException("Can't locate any file !!!");
        }
        //System.out.println(content);

        matcher = PlugUtils.matcher("http:[^$\\x00\\x04]*", content.toString());
        while (matcher.find()) {
            queye.add(new URI(matcher.group(0)));
        }

        httpFile.setState(DownloadState.COMPLETED);
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, queye);
    }

}