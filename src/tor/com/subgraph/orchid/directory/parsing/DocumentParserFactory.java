package com.subgraph.orchid.directory.parsing;

import com.subgraph.orchid.ConsensusDocument;
import com.subgraph.orchid.KeyCertificate;
import com.subgraph.orchid.RouterDescriptor;
import com.subgraph.orchid.RouterMicrodescriptor;

import java.nio.ByteBuffer;

public interface DocumentParserFactory {
    DocumentParser<RouterDescriptor> createRouterDescriptorParser(ByteBuffer buffer, boolean verifySignatures);

    DocumentParser<RouterMicrodescriptor> createRouterMicrodescriptorParser(ByteBuffer buffer);

    DocumentParser<KeyCertificate> createKeyCertificateParser(ByteBuffer buffer);

    DocumentParser<ConsensusDocument> createConsensusDocumentParser(ByteBuffer buffer);
}
