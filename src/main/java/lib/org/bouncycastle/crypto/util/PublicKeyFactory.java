package lib.org.bouncycastle.crypto.util;


import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

import lib.org.bouncycastle.asn1.ASN1InputStream;
import lib.org.bouncycastle.asn1.ASN1Object;
import lib.org.bouncycastle.asn1.ASN1OctetString;
import lib.org.bouncycastle.asn1.ASN1Sequence;
import lib.org.bouncycastle.asn1.DERBitString;
import lib.org.bouncycastle.asn1.DEREncodable;
import lib.org.bouncycastle.asn1.DERInteger;
import lib.org.bouncycastle.asn1.DERObject;
import lib.org.bouncycastle.asn1.DERObjectIdentifier;
import lib.org.bouncycastle.asn1.DEROctetString;
import lib.org.bouncycastle.asn1.nist.NISTNamedCurves;
import lib.org.bouncycastle.asn1.oiw.ElGamalParameter;
import lib.org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import lib.org.bouncycastle.asn1.pkcs.DHParameter;
import lib.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import lib.org.bouncycastle.asn1.sec.SECNamedCurves;
import lib.org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import lib.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import lib.org.bouncycastle.asn1.x509.DSAParameter;
import lib.org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import lib.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import lib.org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import lib.org.bouncycastle.asn1.x9.X962NamedCurves;
import lib.org.bouncycastle.asn1.x9.X962Parameters;
import lib.org.bouncycastle.asn1.x9.X9ECParameters;
import lib.org.bouncycastle.asn1.x9.X9ECPoint;
import lib.org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import lib.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import lib.org.bouncycastle.crypto.params.DHParameters;
import lib.org.bouncycastle.crypto.params.DHPublicKeyParameters;
import lib.org.bouncycastle.crypto.params.DSAParameters;
import lib.org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import lib.org.bouncycastle.crypto.params.ECDomainParameters;
import lib.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import lib.org.bouncycastle.crypto.params.ElGamalParameters;
import lib.org.bouncycastle.crypto.params.ElGamalPublicKeyParameters;
import lib.org.bouncycastle.crypto.params.RSAKeyParameters;

/**
 * Factory to create asymmetric public key parameters for asymmetric ciphers
 * from range of ASN.1 encoded SubjectPublicKeyInfo objects.
 */
public class PublicKeyFactory
{
    /**
     * Create a public key from a SubjectPublicKeyInfo encoding
     * 
     * @param keyInfoData the SubjectPublicKeyInfo encoding
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(
        byte[] keyInfoData)
        throws IOException
    {
        return createKey(
            SubjectPublicKeyInfo.getInstance(
                ASN1Object.fromByteArray(keyInfoData)));
    }

    /**
     * Create a public key from a SubjectPublicKeyInfo encoding read from a stream
     * 
     * @param inStr the stream to read the SubjectPublicKeyInfo encoding from
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(
        InputStream inStr)
        throws IOException
    {
        return createKey(
            SubjectPublicKeyInfo.getInstance(
                new ASN1InputStream(inStr).readObject()));
    }

    /**
     * Create a public key from the passed in SubjectPublicKeyInfo
     * 
     * @param keyInfo the SubjectPublicKeyInfo containing the key data
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(
        SubjectPublicKeyInfo    keyInfo)
        throws IOException
    {
        AlgorithmIdentifier     algId = keyInfo.getAlgorithmId();
        
        if (algId.getObjectId().equals(PKCSObjectIdentifiers.rsaEncryption)
            || algId.getObjectId().equals(X509ObjectIdentifiers.id_ea_rsa))
        {
            RSAPublicKeyStructure   pubKey = new RSAPublicKeyStructure((ASN1Sequence)keyInfo.getPublicKey());

            return new RSAKeyParameters(false, pubKey.getModulus(), pubKey.getPublicExponent());
        }
        else if (algId.getObjectId().equals(PKCSObjectIdentifiers.dhKeyAgreement)
                 || algId.getObjectId().equals(X9ObjectIdentifiers.dhpublicnumber))
        {
            DHParameter params = new DHParameter((ASN1Sequence)keyInfo.getAlgorithmId().getParameters());
            DERInteger  derY = (DERInteger)keyInfo.getPublicKey();
            
            BigInteger lVal = params.getL();
            int l = lVal == null ? 0 : lVal.intValue();
            DHParameters dhParams = new DHParameters(params.getP(), params.getG(), null, l);

            return new DHPublicKeyParameters(derY.getValue(), dhParams);
        }
        else if (algId.getObjectId().equals(OIWObjectIdentifiers.elGamalAlgorithm))
        {
            ElGamalParameter    params = new ElGamalParameter((ASN1Sequence)keyInfo.getAlgorithmId().getParameters());
            DERInteger          derY = (DERInteger)keyInfo.getPublicKey();

            return new ElGamalPublicKeyParameters(derY.getValue(), new ElGamalParameters(params.getP(), params.getG()));
        }
        else if (algId.getObjectId().equals(X9ObjectIdentifiers.id_dsa)
                 || algId.getObjectId().equals(OIWObjectIdentifiers.dsaWithSHA1))
        {
            DERInteger derY = (DERInteger)keyInfo.getPublicKey();
            DEREncodable de = keyInfo.getAlgorithmId().getParameters();

            DSAParameters parameters = null;
            if (de != null)
            {
                DSAParameter params = DSAParameter.getInstance(de.getDERObject());
                parameters = new DSAParameters(params.getP(), params.getQ(), params.getG());
            }

            return new DSAPublicKeyParameters(derY.getValue(), parameters);
        }
        else if (algId.getObjectId().equals(X9ObjectIdentifiers.id_ecPublicKey))
        {
            X962Parameters      params = new X962Parameters((DERObject)keyInfo.getAlgorithmId().getParameters());
            ECDomainParameters  dParams = null;
            
            if (params.isNamedCurve())
            {
                DERObjectIdentifier oid = (DERObjectIdentifier)params.getParameters();
                X9ECParameters      ecP = X962NamedCurves.getByOID(oid);

                if (ecP == null)
                {
                    ecP = SECNamedCurves.getByOID(oid);

                    if (ecP == null)
                    {
                        ecP = NISTNamedCurves.getByOID(oid);

                        if (ecP == null)
                        {
                            ecP = TeleTrusTNamedCurves.getByOID(oid);
                        }
                    }
                }

                dParams = new ECDomainParameters(
                                            ecP.getCurve(),
                                            ecP.getG(),
                                            ecP.getN(),
                                            ecP.getH(),
                                            ecP.getSeed());
            }
            else
            {
                X9ECParameters ecP = new X9ECParameters(
                            (ASN1Sequence)params.getParameters());
                dParams = new ECDomainParameters(
                                            ecP.getCurve(),
                                            ecP.getG(),
                                            ecP.getN(),
                                            ecP.getH(),
                                            ecP.getSeed());
            }

            DERBitString    bits = keyInfo.getPublicKeyData();
            byte[]          data = bits.getBytes();
            ASN1OctetString key = new DEROctetString(data);

            X9ECPoint       derQ = new X9ECPoint(dParams.getCurve(), key);
            
            return new ECPublicKeyParameters(derQ.getPoint(), dParams);
        }
        else
        {
            throw new RuntimeException("algorithm identifier in key not recognised");
        }
    }
}
