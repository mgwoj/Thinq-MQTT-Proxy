/*
 *  Copyright 2021 Michał Wójcik
 */

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientSslConfig
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMDecryptorProvider
import org.bouncycastle.openssl.PEMEncryptedKeyPair
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import java.security.KeyPair
import java.security.KeyStore
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

@Slf4j
class Mqtt {
    Mqtt3AsyncClient client

    boolean isConnected() {
        return client?.getState()?.equals(MqttClientState.CONNECTED)
    }

    void disconnect() {
        client.disconnect()
    }

    void connect(String serverUrl, String clientId, String userName, String password) {
        connect(serverUrl, clientId, username, password, null)
    }
    void connect(String serverUrl, String clientId, String userName, String password, String lwtTopic) {
        URI uri = new URI(serverUrl)
        client = MqttClient.builder()
                .identifier(clientId)
                .serverHost(uri.getHost())
                .serverPort(uri.getPort())
                .useMqttVersion3()
                .automaticReconnectWithDefaultConfig()
                .buildAsync()

        log.info("starting connection the server {}...", serverUrl)
        if (userName && password && lwtTopic) {
            client.connectWith()
                .willPublish()
                .topic(lwtTopic)
                .payload("offline".getBytes())
                .qos(MqttQos.AT_MOST_ONCE)
                .retain(true)
                .applyWillPublish()
                .simpleAuth().username(userName).password(password.getBytes()).applySimpleAuth().send()
                .whenComplete((connAck, throwable) -> {
                    send(lwtTopic, "online")
                });
        } else if (lwtTopic) {
            client.connectWith()
                .willPublish()
                .topic(lwtTopic)
                .payload("offline".getBytes())
                .qos(MqttQos.AT_MOST_ONCE)
                .retain(true)
                .applyWillPublish()
                .send()
                .whenComplete((connAck, throwable) -> {
                    send(lwtTopic, "online")
                });
        } else{
            client.connect()
        }
        log.info("connected!")
    }

    /*
    Using snippet from https://gist.github.com/jimrok/d25cb45b840f5a4ad700
     */
    void connect(LinkedHashMap<String, Object> map, Object serverUrl, Object clientId, def o3, def o4) {
        MqttClientSslConfig sslConfig  = getSocketFactory(map.caCertificate,
                map.clientCertificate, map.privateKey, "")
        URI uri = new URI(serverUrl)

        client = MqttClient.builder()
                .identifier(clientId)
                .serverHost(uri.getHost())
                .serverPort(uri.getPort())
                .useMqttVersion3()
                .automaticReconnectWithDefaultConfig()
                .sslConfig(sslConfig)
                .addDisconnectedListener(new MqttClientDisconnectedListener() {
                    @Override
                    public void onDisconnected( MqttClientDisconnectedContext context) {
                        log.warn("Disconnected")
                    }
                })
                .addConnectedListener(new MqttClientConnectedListener() {
                    @Override
                    public void onConnected(MqttClientConnectedContext context) {
                        log.info("Connected")
                    }
                })
                .buildAsync()


        log.info("starting connection the server {}...", serverUrl)
        client.connect()
        //client.connectWith().keepAlive(30)
        log.info("connected!")
    }

    void subscribe(String sub, int qos, def driver) {
        client.subscribeWith()
                .topicFilter(sub)
                .qos(MqttQos.AT_MOST_ONCE)
                .callback(publish -> driver.parse(new String(publish.getPayloadAsBytes())))
                .send()
    }

    Object parseMessage(Object o) {
        log.info("Parsing received message: {}", o)
        def result = [:]
        result.payload = o.toString()
        return result
    }

    private static MqttClientSslConfig getSocketFactory(final String caCrtFile,
                                                     final String crtFile, final String keyFile, final String password)
            throws Exception {
        Security.addProvider(new BouncyCastleProvider())

        CertificateFactory cf = CertificateFactory.getInstance("X.509")
        // load CA certificate
        X509Certificate caCert = getCaCert(caCrtFile, cf)
        // load client certificate
        X509Certificate cert = getCert(crtFile, cf)
        // load client private key
        KeyPair key = getKey(keyFile, password)
        // CA certificate is used to authenticate server
        TrustManagerFactory tmf = getTrustManagerFactory(caCert)
        // client key and certificates are sent to server so it can authenticate us
        KeyManagerFactory kmf = getKeyManagerFactory(cert, key, password)

        MqttClientSslConfig sslConfig = MqttClientSslConfig.builder()
                .keyManagerFactory(kmf)
                .trustManagerFactory(tmf)
                .protocols(Collections.singletonList("TLSv1.2"))
                .build()

        return sslConfig
    }

    private static X509Certificate getCert(String crtFile, CertificateFactory cf) {
        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(crtFile.getBytes()))
        X509Certificate cert = null
        while (bis.available() > 0) {
            cert = (X509Certificate) cf.generateCertificate(bis)
        }
        return cert
    }

    private static X509Certificate getCaCert(String caCrtFile, CertificateFactory cf) {
        X509Certificate caCert = null
        InputStream fis = new ByteArrayInputStream(caCrtFile.getBytes())
        BufferedInputStream bis = new BufferedInputStream(fis)

        while (bis.available() > 0) {
            caCert = (X509Certificate) cf.generateCertificate(bis)
        }
        return caCert
    }

    private static KeyPair getKey(String keyFile, String password) {
        PEMParser pemParser = new PEMParser(new StringReader(keyFile))
        Object object = pemParser.readObject()
        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray())
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC")
        KeyPair key
        if (object instanceof PEMEncryptedKeyPair) {
            log.info("Encrypted key - we will use provided password")
            key = converter.getKeyPair(((PEMEncryptedKeyPair) object)
                    .decryptKeyPair(decProv))
        } else {
            log.info("Unencrypted key - no password needed")
            key = converter.getKeyPair((PEMKeyPair) object)
        }
        pemParser.close()
        return key
    }

    private static KeyManagerFactory getKeyManagerFactory(X509Certificate cert, KeyPair key, String password) {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType())
        ks.load(null, null)
        ks.setCertificateEntry("certificate", cert)
        ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(),
                new Certificate[]{cert})
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, password.toCharArray())
        return kmf
    }

    private static TrustManagerFactory getTrustManagerFactory(X509Certificate caCert) {
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType())
        caKs.load(null, null)
        caKs.setCertificateEntry("ca-certificate", caCert)
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509")
        tmf.init(caKs)
        return tmf
    }

    void send(String topic, String payload) {
        log.info("Sending to topic {} message {}", topic, payload)
        client.publishWith()
                .topic(topic)
                .qos(MqttQos.AT_MOST_ONCE)
                .retain(true)
                .payload(payload.getBytes())
                .send()
    }
}
