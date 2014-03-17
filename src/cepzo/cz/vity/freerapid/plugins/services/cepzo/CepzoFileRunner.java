package cz.vity.freerapid.plugins.services.cepzo;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class CepzoFileRunner extends XFileSharingRunner {

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        //doesn't catch these :
        //<button type="submit" name="method_premium" value="Premium Download">  <img src="http://www.filesin.com/images/maxspeed.gif" width="195" alt=alt="Premium Download"><br>HIGH SPEED DOWNLOAD</button>&nbsp; &nbsp; &nbsp;
        //<button type="submit" name="method_free" value="Free Download">  <img src="http://www.filesin.com/images/lowspeed.gif" width="195" alt=alt="Free Download"><br>SLOW DOWNLOAD</button><
        final MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .setAction(fileURL);
        //we have do add them manually
        final Matcher matcher = getMatcherAgainstContent("<button type=\"submit\" name=\"(method_premium|method_free)\" value=\"(.+?)\"");
        while (matcher.find()) {
            methodBuilder.setParameter(matcher.group(1), matcher.group(2));
        }
        if (!methodBuilder.getParameters().get("method_free").isEmpty()) {
            methodBuilder.removeParameter("method_premium");
        }
        return methodBuilder;
    }
}