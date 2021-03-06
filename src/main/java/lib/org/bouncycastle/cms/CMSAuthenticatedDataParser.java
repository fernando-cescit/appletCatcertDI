package lib.org.bouncycastle.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AlgorithmParameters;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.List;

import lib.org.bouncycastle.asn1.ASN1EncodableVector;
import lib.org.bouncycastle.asn1.ASN1OctetStringParser;
import lib.org.bouncycastle.asn1.ASN1SequenceParser;
import lib.org.bouncycastle.asn1.ASN1SetParser;
import lib.org.bouncycastle.asn1.DEREncodable;
import lib.org.bouncycastle.asn1.DERSet;
import lib.org.bouncycastle.asn1.DERTags;
import lib.org.bouncycastle.asn1.cms.AttributeTable;
import lib.org.bouncycastle.asn1.cms.AuthenticatedDataParser;
import lib.org.bouncycastle.asn1.cms.ContentInfoParser;
import lib.org.bouncycastle.asn1.cms.RecipientInfo;
import lib.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import lib.org.bouncycastle.util.Arrays;


/**
 * Parsing class for an CMS Authenticated Data object from an input stream.
 * <p>
 * Note: that because we are in a streaming mode only one recipient can be tried and it is important
 * that the methods on the parser are called in the appropriate order.
 * </p>
 * <p>
 * Example of use - assuming the first recipient matches the private key we have.
 * <pre>
 *      CMSAuthenticatedDataParser     ad = new CMSAuthenticatedDataParser(inputStream);
 *
 *      RecipientInformationStore  recipients = ad.getRecipientInfos();
 *
 *      Collection  c = recipients.getRecipients();
 *      Iterator    it = c.iterator();
 *
 *      if (it.hasNext())
 *      {
 *          RecipientInformation   recipient = (RecipientInformation)it.next();
 *
 *          CMSTypedStream recData = recipient.getContentStream(privateKey, "BC");
 *
 *          processDataStream(recData.getContentStream());
 *
 *          if (!Arrays.equals(ad.getMac(), recipient.getMac())
 *          {
 *              System.err.println("Data corrupted!!!!");
 *          }
 *      }
 *  </pre>
 *  Note: this class does not introduce buffering - if you are processing large files you should create
 *  the parser with:
 *  <pre>
 *          CMSAuthenticatedDataParser     ep = new CMSAuthenticatedDataParser(new BufferedInputStream(inputStream, bufSize));
 *  </pre>
 *  where bufSize is a suitably large buffer size.
 */
public class CMSAuthenticatedDataParser
    extends CMSContentInfoParser
{
    RecipientInformationStore   _recipientInfoStore;
    AuthenticatedDataParser authData;

    private AlgorithmIdentifier macAlg;
    private byte[]              mac;
    private AttributeTable authAttrs;
    private AttributeTable unauthAttrs;

    private boolean authAttrNotRead;
    private boolean unauthAttrNotRead;

    public CMSAuthenticatedDataParser(
        byte[]    envelopedData)
        throws CMSException, IOException
    {
        this(new ByteArrayInputStream(envelopedData));
    }

    public CMSAuthenticatedDataParser(
        InputStream    envelopedData)
        throws CMSException, IOException
    {
        super(envelopedData);

        this.authAttrNotRead = true;
        this.authData = new AuthenticatedDataParser((ASN1SequenceParser)_contentInfo.getContent(DERTags.SEQUENCE));

        // TODO Validate version?
        //DERInteger version = this.authData.getVersion();

        //
        // load the RecipientInfoStore
        //
        ASN1SetParser s = authData.getRecipientInfos();
        List          baseInfos = new ArrayList();

        DEREncodable entry;
        while ((entry = s.readObject()) != null)
        {
            baseInfos.add(RecipientInfo.getInstance(entry.getDERObject()));
        }

        this.macAlg = authData.getMacAlgorithm();

        //
        // read the encrypted content info
        //
        ContentInfoParser data = authData.getEnapsulatedContentInfo();


        //
        // prime the recipients
        //
        InputStream contentStream = ((ASN1OctetStringParser)data.getContent(DERTags.OCTET_STRING)).getOctetStream();
        List infos = CMSEnvelopedHelper.readRecipientInfos(
            baseInfos.iterator(), contentStream, null, macAlg, null);

        _recipientInfoStore = new RecipientInformationStore(infos);
    }

    /**
     * return the object identifier for the mac algorithm.
     */
    public String getMacAlgOID()
    {
        return macAlg.getObjectId().toString();
    }

    /**
     * return the ASN.1 encoded encryption algorithm parameters, or null if
     * there aren't any.
     */
    public byte[] getMacAlgParams()
    {
        try
        {
            return encodeObj(macAlg.getParameters());
        }
        catch (Exception e)
        {
            throw new RuntimeException("exception getting encryption parameters " + e);
        }
    }

    /**
     * Return an AlgorithmParameters object giving the encryption parameters
     * used to encrypt the message content.
     *
     * @param provider the name of the provider to generate the parameters for.
     * @return the parameters object, null if there is not one.
     * @throws lib.org.bouncycastle.cms.CMSException if the algorithm cannot be found, or the parameters can't be parsed.
     * @throws java.security.NoSuchProviderException if the provider cannot be found.
     */
    public AlgorithmParameters getMacAlgorithmParameters(
        String  provider)
        throws CMSException, NoSuchProviderException
    {
        return getMacAlgorithmParameters(CMSUtils.getProvider(provider));
    }

    /**
     * Return an AlgorithmParameters object giving the encryption parameters
     * used to encrypt the message content.
     *
     * @param provider the provider to generate the parameters for.
     * @return the parameters object, null if there is not one.
     * @throws lib.org.bouncycastle.cms.CMSException if the algorithm cannot be found, or the parameters can't be parsed.
     */
    public AlgorithmParameters getMacAlgorithmParameters(
        Provider provider)
        throws CMSException
    {
        return CMSEnvelopedHelper.INSTANCE.getEncryptionAlgorithmParameters(getMacAlgOID(), getMacAlgParams(), provider);
    }

    /**
     * return a store of the intended recipients for this message
     */
    public RecipientInformationStore getRecipientInfos()
    {
        return _recipientInfoStore;
    }

    public byte[] getMac()
        throws IOException
    {
        if (mac == null)
        {
            getAuthAttrs();
            mac = authData.getMac().getOctets();
        }
        return Arrays.clone(mac);
    }

    /**
     * return a table of the unauthenticated attributes indexed by
     * the OID of the attribute.
     * @exception java.io.IOException
     */
    public AttributeTable getAuthAttrs()
        throws IOException
    {
        if (authAttrs == null && authAttrNotRead)
        {
            ASN1SetParser             set = authData.getAuthAttrs();

            authAttrNotRead = false;

            if (set != null)
            {
                ASN1EncodableVector v = new ASN1EncodableVector();
                DEREncodable        o;

                while ((o = set.readObject()) != null)
                {
                    ASN1SequenceParser    seq = (ASN1SequenceParser)o;

                    v.add(seq.getDERObject());
                }

                authAttrs = new AttributeTable(new DERSet(v));
            }
        }

        return authAttrs;
    }

    /**
     * return a table of the unauthenticated attributes indexed by
     * the OID of the attribute.
     * @exception java.io.IOException
     */
    public AttributeTable getUnauthAttrs()
        throws IOException
    {
        if (unauthAttrs == null && unauthAttrNotRead)
        {
            ASN1SetParser             set = authData.getUnauthAttrs();

            unauthAttrNotRead = false;

            if (set != null)
            {
                ASN1EncodableVector v = new ASN1EncodableVector();
                DEREncodable        o;

                while ((o = set.readObject()) != null)
                {
                    ASN1SequenceParser    seq = (ASN1SequenceParser)o;

                    v.add(seq.getDERObject());
                }

                unauthAttrs = new AttributeTable(new DERSet(v));
            }
        }

        return unauthAttrs;
    }

    private byte[] encodeObj(
        DEREncodable    obj)
        throws IOException
    {
        if (obj != null)
        {
            return obj.getDERObject().getEncoded();
        }

        return null;
    }
}
