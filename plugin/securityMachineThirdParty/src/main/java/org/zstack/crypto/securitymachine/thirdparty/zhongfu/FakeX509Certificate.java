package org.zstack.crypto.securitymachine.thirdparty.zhongfu;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;

public class FakeX509Certificate extends X509Certificate {
	private BigInteger serialNumber;

	private String issuerDN;

	private String subjectDN;

	private long effectiveTime;

	private long expirationTime;

	private String algorithm;

	public static class Builder {
		final FakeX509Certificate certificate = new FakeX509Certificate();

		public Builder serialNumber(BigInteger serialNumber) {
			this.certificate.serialNumber = serialNumber;
			return this;
		}

		public Builder serialNumber(long serialNumber) {
			this.certificate.serialNumber = BigInteger.valueOf(serialNumber);
			return this;
		}

		public Builder issuerDN(String issuerDN) {
			this.certificate.issuerDN = issuerDN;
			return this;
		}

		public Builder subjectDN(String subjectDN) {
			this.certificate.subjectDN = subjectDN;
			return this;
		}

		public Builder algorithm(String algorithm) {
			this.certificate.algorithm = algorithm;
			return this;
		}

		public Builder effectiveTime(LocalDateTime time) {
			this.certificate.effectiveTime = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			return this;
		}

		public Builder effectiveTime(Instant time) {
			this.certificate.effectiveTime = time.toEpochMilli();
			return this;
		}

		public Builder effectiveTime(long time) {
			this.certificate.effectiveTime = time;
			return this;
		}

		public Builder expirationTime(LocalDateTime time) {
			this.certificate.expirationTime = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			return this;
		}

		public Builder expirationTime(Instant time) {
			this.certificate.expirationTime = time.toEpochMilli();
			return this;
		}

		public Builder expirationTime(long time) {
			this.certificate.expirationTime = time;
			return this;
		}

		public X509Certificate build() {
			return this.certificate;
		}
	}

	public static class FakePrincipal implements Principal {
		String name;

		public FakePrincipal(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public String toString() {
			return getName();
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	private FakeX509Certificate() {}

	public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {}

	public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
		long time = date.getTime();
		if (time < this.effectiveTime || time > this.expirationTime)
			throw new CertificateExpiredException();
	}

	public int getVersion() {
		return 0;
	}

	public BigInteger getSerialNumber() {
		return this.serialNumber;
	}

	public Principal getIssuerDN() {
		return new FakePrincipal(this.issuerDN);
	}

	public Principal getSubjectDN() {
		return new FakePrincipal(this.subjectDN);
	}

	public Date getNotBefore() {
		return new Date(this.effectiveTime);
	}

	public Date getNotAfter() {
		return new Date(this.expirationTime);
	}

	public byte[] getTBSCertificate() throws CertificateEncodingException {
		throw new IllegalStateException("not support");
	}

	public byte[] getSignature() {
		throw new IllegalStateException("not support");
	}

	public String getSigAlgName() {
		return this.algorithm;
	}

	public String getSigAlgOID() {
		throw new IllegalStateException("not support");
	}

	public byte[] getSigAlgParams() {
		throw new IllegalStateException("not support");
	}

	public boolean[] getIssuerUniqueID() {
		throw new IllegalStateException("not support");
	}

	public boolean[] getSubjectUniqueID() {
		throw new IllegalStateException("not support");
	}

	public boolean[] getKeyUsage() {
		throw new IllegalStateException("not support");
	}

	public int getBasicConstraints() {
		throw new IllegalStateException("not support");
	}

	public byte[] getEncoded() throws CertificateEncodingException {
		throw new IllegalStateException("not support");
	}

	public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
		throw new IllegalStateException("not support");
	}

	public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
		throw new IllegalStateException("not support");
	}

	public String toString() {
		return "FakeX509Certificate{serialNumber=" +
				this.serialNumber +
				", issuerDN='" + this.issuerDN + '\'' +
				", subjectDN='" + this.subjectDN + '\'' +
				", effectiveTime=" + this.effectiveTime +
				", expirationTime=" + this.expirationTime +
				", algorithm='" + this.algorithm + '\'' +
				'}';
	}

	public PublicKey getPublicKey() {
		throw new IllegalStateException("not support");
	}

	public boolean hasUnsupportedCriticalExtension() {
		return false;
	}

	public Set<String> getCriticalExtensionOIDs() {
		throw new IllegalStateException("not support");
	}

	public Set<String> getNonCriticalExtensionOIDs() {
		throw new IllegalStateException("not support");
	}

	public byte[] getExtensionValue(String oid) {
		throw new IllegalStateException("not support");
	}
}
