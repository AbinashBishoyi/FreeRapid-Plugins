package cz.vity.freerapid.plugins.services.ifolder;

import java.io.IOException;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 *
 * @author JPEXS
 */
public class GetMethodNoStatus extends GetMethod{
    public GetMethodNoStatus(){

    }

    public GetMethodNoStatus(String uri) {
        super(uri);
    }




    @Override
    protected void readResponse(HttpState state, HttpConnection conn)
    throws IOException, HttpException {
        this.statusLine = new StatusLine("HTTP/1.1 200 OK");
        processStatusLine(state, conn);        
        readResponseBody(state, conn);
        processResponseBody(state, conn);
    }

}
