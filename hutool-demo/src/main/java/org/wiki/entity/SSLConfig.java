package org.wiki.entity;

// SSL配置信息
public class SSLConfig {
    private String name;
    private String protocol;
    private boolean trustAll;
    private String keystorePath;
    private String keystorePassword;
    private String truststorePath;
    private String truststorePassword;

    public SSLConfig(String name) {
        this.name = name;
        this.protocol = "TLS";
        this.trustAll = false;
    }

    // Getters and Setters
    public String getName() { return name; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public boolean isTrustAll() { return trustAll; }
    public void setTrustAll(boolean trustAll) { this.trustAll = trustAll; }
    public String getKeystorePath() { return keystorePath; }
    public void setKeystorePath(String keystorePath) { this.keystorePath = keystorePath; }
    public String getKeystorePassword() { return keystorePassword; }
    public void setKeystorePassword(String keystorePassword) { this.keystorePassword = keystorePassword; }
    public String getTruststorePath() { return truststorePath; }
    public void setTruststorePath(String truststorePath) { this.truststorePath = truststorePath; }
    public String getTruststorePassword() { return truststorePassword; }
    public void setTruststorePassword(String truststorePassword) { this.truststorePassword = truststorePassword; }
}