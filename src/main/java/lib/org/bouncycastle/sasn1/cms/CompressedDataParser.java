package lib.org.bouncycastle.sasn1.cms;


import java.io.IOException;

import lib.org.bouncycastle.asn1.ASN1InputStream;
import lib.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import lib.org.bouncycastle.sasn1.Asn1Integer;
import lib.org.bouncycastle.sasn1.Asn1Sequence;
import lib.org.bouncycastle.sasn1.DerSequence;

/** 
 * RFC 3274 - CMS Compressed Data.
 * <pre>
 * CompressedData ::= SEQUENCE {
 *  version CMSVersion,
 *  compressionAlgorithm CompressionAlgorithmIdentifier,
 *  encapContentInfo EncapsulatedContentInfo
 * }
 * </pre>
 * @deprecated use corresponding class in org.bouncycastle.asn1.cms
 */
public class CompressedDataParser
{
    private Asn1Integer          _version;
    private AlgorithmIdentifier  _compressionAlgorithm;
    private ContentInfoParser    _encapContentInfo;
    
    public CompressedDataParser(
        Asn1Sequence seq)
        throws IOException
    {
        this._version = (Asn1Integer)seq.readObject();
        this._compressionAlgorithm = AlgorithmIdentifier.getInstance(new ASN1InputStream(((DerSequence)seq.readObject()).getEncoded()).readObject());
        this._encapContentInfo = new ContentInfoParser((Asn1Sequence)seq.readObject());
    }

    public Asn1Integer getVersion()
    {
        return _version;
    }

    public AlgorithmIdentifier getCompressionAlgorithmIdentifier()
    {
        return _compressionAlgorithm;
    }

    public ContentInfoParser getEncapContentInfo()
    {
        return _encapContentInfo;
    }
}
