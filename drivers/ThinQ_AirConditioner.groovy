import groovy.util.logging.Slf4j

/**
 *  LG AirConditioner
 *
 *  Copyright 2020 Jean Bilodeau
 *
 */
@Slf4j
class ThinQ_AirConditioner extends Device {

//@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]
//@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[2]
//
//metadata {
//    definition(name: "LG ThinQ AirConditioner", namespace: "dcm.thinq", author: "Jean Bilodeau") {
//        capability "Sensor"
//        capability "Switch"
//        capability "Initialize"
//
//        attribute "diagCode", "number"
//        attribute "dirStep", "number"
//        attribute "lightingDisplay", "number"
//        attribute "operation", "number"
//        attribute "opMode", "number"
//        attribute "powerSaveBasic", "number"
//        attribute "qualitySensorMon", "number"
//        attribute "sleepTime", "number"
//        attribute "targetTimeToStart", "number"
//        attribute "targetTimeToStop", "number"
//        attribute "tempCurrent", "number"
//        attribute "tempTarget", "number"
//        attribute "windStrength", "number"
//
//        command "dirStep",            [[name: "dirStep",type:"NUMBER", description: "dirStep, Wind direction (Left Right, Up Down), possible values at https://github.com/jbilodea/ThinQ_AC/blob/main/DirStep"]]
//        command "decreaseTarget" //,     [[name: "decreaseTarget",type:"NUMBER", description: "Decrease target temperature"]]
//        command "increaseTarget" //,     [[name: "increaseTarget",type:"NUMBER", description: "Increase target temperature"]]
//        command "opMode",             [[name: "opMode",type:"NUMBER", description: "opMode (Cool, Fan,...) , possible values at https://github.com/jbilodea/ThinQ_AC/blob/main/opMode"]]
//        command "temperatureTarget",  [[name: "temperatureTarget",type:"NUMBER", description: "Target temperature"]]
//        command "windStrength",       [[name: "windStrength",type:"NUMBER", description: "Wind Strength, possible values at https://github.com/jbilodea/ThinQ_AC/blob/main/windStrengthValues"]]
//        command "targetTimeToStart",  [[name: "targetTimeToStart",type:"NUMBER", description: "From 0 to 1440 minutes"]]
//        command "targetTimeToStop",   [[name: "targetTimeToStop",type:"NUMBER", description: "From 0 to 1440 minutes"]]
//    }
//
//    preferences {
//      section { // General
//        input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: false
//        input name: "logDescText", title: "Log Description Text", type: "bool", defaultValue: false, required: false
//      }
//    }
//}

    def currentStateSwitch

def on() {
    result = parent.sendCommand(device, "Set",  "basicCtrl", "airState.operation", 1)
}

def off() {
    result = parent.sendCommand(device, "Set",  "basicCtrl", "airState.operation", 0)
}

def decreaseTarget() {

    def nextTarget =  device.currentValue("tempTarget") - 1
    result = parent.sendCommand(device, "Set",  "basicCtrl", "airState.tempState.target", nextTarget)
    sendEvent(name: "tempTarget", value: nextTarget, displayed: false)
}

def increaseTarget() {

    def nextTarget =  device.currentValue("tempTarget") + 1
    result = parent.sendCommand(device, "Set",  "basicCtrl", "airState.tempState.target", nextTarget)
    sendEvent(name: "tempTarget", value: nextTarget, displayed: false)
}

def temperatureTarget(ptemperatureTarget) {

    if (ptemperatureTarget != null) {
        result = parent.sendCommand(device, "Set",  "basicCtrl", "airState.tempState.target", ptemperatureTarget)
        sendEvent(name: "tempTarget", value: ptemperatureTarget, displayed: false)
    }
}

def targetTimeToStart(ptargetTimeToStart) {

    if (ptargetTimeToStart != null) {
        result = parent.sendCommand(device, "Set",  "basicCtrl", "airState.reservation.targetTimeToStart", ptargetTimeToStart)
        sendEvent(name: "targetTimeToStart", value: ptemperatureTarget, displayed: false)
    }
}

def targetTimeToStop(ptargetTimeToStop) {

    if (ptargetTimeToStop != null) {
        result = parent.sendCommand(device, "Set",  "basicCtrl", "airState.reservation.targetTimeToStop", ptargetTimeToStop)
        sendEvent(name: "targetTimeToStop", value: ptargetTimeToStop, displayed: false)
    }
}

def dirStep(pdirStep) {
   
    if (pdirStep != null) {
        result = parent.sendCommand(device, "Set",  "wDirCtrl", "airState.wDir.hStep", pdirStep)
        sendEvent(name: "dirStep", value: pdirStep, displayed: false)
    }
}

def opMode(popMode) {
   
    if (popMode != null) {
        result = parent.sendCommand(device, "Set",  "basicCtrl", "airState.opMode", popMode)
        sendEvent(name: "opMode", value: popMode, displayed: false)
    }
}

def windStrength(pwindStrength) {
   
    if (pwindStrength != null) {
        result = parent.sendCommand(device, "Set",  "basicCtrl", "airState.windStrength", pwindStrength)
        sendEvent(name: "windStrength", value: pwindStrength, displayed: false)
    }
}

def processStateData(data) {
    
    logger("debug", "processStateData(${data})")
    super.processStateData(data)

    if (parent.checkValue(data, "airState.diagCode"))
        sendEvent(name: "diagCode", value: data["airState.diagCode"], displayed: false)
    if (parent.checkValue(data,'airState.wDir.hStep'))
        sendEvent(name: "DirStep", value: data["airState.wDir.hStep"], displayed: false)
    if (parent.checkValue(data, "airState.lightingState.displayControl"))
        sendEvent(name: "lightingDisplay", value: data["airState.lightingState.displayControl"], displayed: false)
    if (parent.checkValue(data, "airState.operation")) {
        sendEvent(name: "operation", value: data["airState.operation"], displayed: false)
        if (data["airState.operation"] == 1)
         currentStateSwitch = "on"
        else
         currentStateSwitch = "off"
        sendEvent(name: "switch", value: currentStateSwitch, descriptionText: "Was turned ${currentStateSwitch}")
    }
    if (parent.checkValue(data, "airState.opMode"))
        sendEvent(name: "opMode", value: data["airState.opMode"], displayed: false)
    if (parent.checkValue(data, "airState.quality.sensorMon"))
        sendEvent(name: "qualitySensorMon", value: data["airState.quality.sensorMon"], displayed: false)
    if (parent.checkValue(data, "airState.reservation.sleepTime"))
        sendEvent(name: "sleepTime", value: data["airState.reservation.sleepTime"], displayed: false)
    if (parent.checkValue(data, "airState.reservation.targetTimeToStart"))
		sendEvent(name: "targetTimeToStart", value: data["airState.reservation.targetTimeToStart"], displayed: false)
    if (parent.checkValue(data, "airState.reservation.targetTimeToStop"))
        sendEvent(name: "targetTimeToStop", value: data["airState.reservation.targetTimeToStop"], displayed: false)
    if (parent.checkValue(data, "airState.tempState.current"))
		sendEvent(name: "tempCurrent", value: data["airState.tempState.current"], displayed: false)
    if (parent.checkValue(data, "airState.tempState.target"))
        sendEvent(name: "tempTarget", value: data["airState.tempState.target"], displayed: false)
    if (parent.checkValue(data, "airState.windStrength"))
        sendEvent(name: "windStrength", value: data["airState.windStrength"], displayed: false)
}

}