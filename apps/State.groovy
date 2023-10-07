/*
 *  Copyright 2021 Michał Wójcik
 */
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class State {
    def pollTime
    def pubMqttServer
    def pubMqttTopic
    def pubClientId
    def pubMqttLWTTopic
    def client_id
    def auth_retry_cnt
    def langCode
    def countryCode
    def oauthUrl
    def empUrl
    def thinqUrl
    def thinq1Url
    def empSpxUri
    def rtiUri
    def mqttServer
    def prevUrl
    def access_token
    def user_number
    def refresh_token
    def cert
    def subscriptions
    def foundDevices
    def privateKey
    def csr
    String oauth_url
    def jsession
    def region
    def certSource
    def pubUserName
    def pubPassword
    def friendlyNames = [:]

    void save(String fileName) {
        def json = JsonOutput.toJson(this)
        new File(fileName).write(json)
    }

    static State load(String fileName) {
        def jsonSlurper = new JsonSlurper()
        String text = new File(fileName).getText("UTF-8")
        return jsonSlurper.parseText(text) as State
    }
}
