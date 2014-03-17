package com.subgraph.orchid.directory.downloader;

import com.subgraph.orchid.RouterDescriptor;
import com.subgraph.orchid.directory.parsing.DocumentParser;

import java.nio.ByteBuffer;

public class BridgeDescriptorFetcher extends DocumentFetcher<RouterDescriptor> {

    @Override
    String getRequestPath() {
        return "/tor/server/authority";
    }

    @Override
    DocumentParser<RouterDescriptor> createParser(ByteBuffer response) {
        return PARSER_FACTORY.createRouterDescriptorParser(response, true);
    }
}
