package lib.org.bouncycastle.cms;

import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import lib.org.bouncycastle.asn1.ASN1EncodableVector;
import lib.org.bouncycastle.asn1.ASN1OctetString;
import lib.org.bouncycastle.asn1.DERObjectIdentifier;
import lib.org.bouncycastle.asn1.DEROctetString;
import lib.org.bouncycastle.asn1.DERSequence;
import lib.org.bouncycastle.asn1.cms.PasswordRecipientInfo;
import lib.org.bouncycastle.asn1.cms.RecipientInfo;
import lib.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import lib.org.bouncycastle.asn1.x509.AlgorithmIdentifier;


class PasswordRecipientInfoGenerator implements RecipientInfoGenerator
{
    private AlgorithmIdentifier derivationAlg;
    private SecretKey wrapKey;

    PasswordRecipientInfoGenerator()
    {
    }

    void setDerivationAlg(AlgorithmIdentifier derivationAlg)
    {
        this.derivationAlg = derivationAlg;
    }

    void setWrapKey(SecretKey wrapKey)
    {
        this.wrapKey = wrapKey;
    }

    public RecipientInfo generate(SecretKey key, SecureRandom random,
            Provider prov) throws GeneralSecurityException
    {
        // TODO Consider passing in the wrapAlgorithmOID instead

        CMSEnvelopedHelper helper = CMSEnvelopedHelper.INSTANCE;
        String wrapAlgName = helper.getRFC3211WrapperName(wrapKey.getAlgorithm());
        Cipher keyCipher = helper.createAsymmetricCipher(wrapAlgName, prov);
        keyCipher.init(Cipher.WRAP_MODE, wrapKey, random);
        ASN1OctetString encKey = new DEROctetString(keyCipher.wrap(key));

        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new DERObjectIdentifier(wrapKey.getAlgorithm()));
        v.add(new DEROctetString(keyCipher.getIV()));
        AlgorithmIdentifier keyEncAlg = new AlgorithmIdentifier(
                PKCSObjectIdentifiers.id_alg_PWRI_KEK, new DERSequence(v));

        return new RecipientInfo(new PasswordRecipientInfo(derivationAlg,
                keyEncAlg, encKey));
    }

}
