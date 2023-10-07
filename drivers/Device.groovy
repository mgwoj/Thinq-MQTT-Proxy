import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

/*
 *  Copyright 2021 Michał Wójcik
 */
@Slf4j
class Device {
    def deviceNetworkId
    def meta = [:]
    def displayName = "TODO"
    def friendlyName
    def parent
    Device device = this
    Interfaces interfaces
    boolean logDescText
    def location = [timeZone: TimeZone.default]

    def uninstalled() {
        logger("debug", "uninstalled()")
        parent.stopRTIMonitoring(device)
    }

    def initialize() {
        logger("debug", "initialize()")

        if (getDataValue("master") == "true") {
            if (interfaces.mqtt.isConnected())
                interfaces.mqtt.disconnect()

            mqttConnectUntilSuccessful()
        }

        parent.registerRTIMonitoring(device)
    }

    def mqttConnectUntilSuccessful() {
        logger("debug", "mqttConnectUntilSuccessful()")

        try {
            def mqtt = parent.retrieveMqttDetails()

            interfaces.mqtt.connect(mqtt.server,
                    mqtt.clientId,
                    null,
                    null,
                    tlsVersion: "1.2",
                    privateKey: mqtt.privateKey,
                    caCertificate: mqtt.caCertificate,
                    clientCertificate: mqtt.certificate,
                    ignoreSSLIssues: true)
            pauseExecution(3000)
            for (sub in mqtt.subscriptions) {
                interfaces.mqtt.subscribe(sub, 0, this)
            }
            return true
        }
        catch (e)
        {
            logger("warn", "Lost connection to MQTT, retrying in 15 seconds ${e}")
            runIn(15, "mqttConnectUntilSuccessful")
            return false
        }
    }

    def parse(message) {
        def topic = interfaces.mqtt.parseMessage(message)
        def payload = new JsonSlurper().parseText(topic.payload)
        logger("info", "parse(${payload})")

        parent.processMqttMessage(this, payload)
    }

    def mqttClientStatus(String message) {
        logger("debug", "mqttClientStatus(${message})")

        if (message.startsWith("Error:")) {
            logger("error", "MQTT Error: ${message}")

            try {
                interfaces.mqtt.disconnect() // Guarantee we're disconnected
            }
            catch (e) {
            }
            mqttConnectUntilSuccessful()
        }
    }

    /**
     * @param level Level to log at, see LOG_LEVELS for options
     * @param msg Message to log
     */
    def logger(level, msg) {
        log."${level}" "${device.displayName} ${msg}"
    }

    def processStateData(stateData) {
        sendEvent(name: "lastUpdate", value: new Date().format("yyyy-MM-dd'T'HH:mm:ssZ", TimeZone.default))
    }

    void sendEvent(LinkedHashMap<String, Object> eventMap) {
        log.info("{}", eventMap)
        interfaces.pubMqtt.send(parent.state.pubMqttTopic + "/" + friendlyName + "/event/" + eventMap.name, eventMap.value.toString())
    }

    void pauseExecution(int timeInMs) {

    }

    void runIn(int timeInSeconds, String method) {
        log.info("Sleeping for $timeInSeconds before calling $method")
        sleep(timeInSeconds * 1000)
        "${method}"()
    }

    void updateDataValue(String key, String value) {
        log.debug("updateDataValue($key, $value)");
        meta[key] = value
    }

    String getDataValue(String key) {
        return meta[key]
    }

    Boolean hasCapability(String capability) {
        return true
    }

    void removeDataValue(String key) {
        meta.remove(key)
    }

    String currentValue(String s) {
        null
    }

    static String getTemperatureScale() {
        "C"
    }

    static def fahrenheitToCelsius(def temp) {
        null
    }

    static def celsiusToFahrenheit(def temp) {
        null
    }
}
