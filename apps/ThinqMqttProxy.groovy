import groovy.util.logging.Slf4j

/*
 *  Copyright 2021 Michał Wójcik
 */
@Slf4j
class ThinqMqttProxy {
    static void main(String[] args) {
        log.info "Starting Thinq Mqtt Proxy..."

        if (args.length != 1) {
            printHelp()
            System.exit(1)
        }
        switch (args[0]) {
            case "init":doInit()
                break
            case "run":doRun()
                break
            default:printHelp()
                System.exit(1)
        }
        System.exit(0)
    }

    static void printHelp() {
        println "Usage:"
        println "init"
        println "run"
    }

    static void doInit() {
        def integration = new ThinQ_Integration()
        integration.prefMain()
        integration.generateKeyAndCSR()
        integration.state.save(integration.STATE_FILE)
    }

    static void doRun() {
        def integration = new ThinQ_Integration()
        integration.interfaces.pubMqtt.connect(integration.state.pubMqttServer, integration.state.pubClientId, integration.state.pubUserName, integration.state.pubPassword, integration.state.pubMqttLWTTopic)
        integration.prefDevices()
        initializeFriendlyNames(integration)
        integration.state.save(integration.STATE_FILE)
        log.info "Devices identified..."
        integration.installed()
        integration.state.save(integration.STATE_FILE)
        log.info "Waiting forever..."
        Thread.currentThread().join()
        log.info "Shut down..."
    }

    private static void initializeFriendlyNames(ThinQ_Integration integration) {
        integration.state.foundDevices.forEach(device -> {
            if (!integration.state.friendlyNames.get(device.id)) {
                integration.state.friendlyNames[device.id] = device.name.toLowerCase()
            }
        })
    }
}
