import groovy.util.logging.Slf4j

/**
 *  LG Dryer
 *
 *  Copyright 2020 Dominick Meglio
 *
 */
@Slf4j
class ThinQ_Dryer extends Device {

//metadata {
//    definition(name: "LG ThinQ Dryer", namespace: "dcm.thinq", author: "dmeglio@gmail.com") {
//        capability "Sensor"
//        capability "Switch"
//        capability "Initialize"
//
//        attribute "runTime", "number"
//        attribute "runTimeDisplay", "string"
//        attribute "remainingTime", "number"
//        attribute "remainingTimeDisplay", "string"
//        attribute "finishTimeDisplay", "string"
//        attribute "currentState", "string"
//        attribute "error", "string"
//        attribute "course", "string"
//        attribute "smartCourse", "string"
//        attribute "dryLevel", "string"
//        attribute "temperatureLevel", "string"
//        attribute "timeDry", "string"
//    }
//
//    preferences {
//      section { // General
//        input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: false
//        input name: "logDescText", title: "Log Description Text", type: "bool", defaultValue: false, required: false
//      }
//    }
//}

def processStateData(data) {
    logger("debug", "processStateData(${data})")
    super.processStateData(data)

    def runTime = 0
    def runTimeDisplay = '00:00'
    def remainingTime = 0
    def remainingTimeDisplay = '00:00'
    def delayTime = 0
    def delayTimeDisplay = '00:00'
    def error

    if (parent.checkValue(data,'Initial_Time_H')) {
      runTime += (data["Initial_Time_H"]*60*60)
      updateDataValue("initialHours", data["Initial_Time_H"].toString())
    } else {
      runTime += (getDataValue("initialHours") ?: "0").toFloat().toInteger()*60*60
    }
    if (parent.checkValue(data,'Initial_Time_M')) {
      runTime += (data["Initial_Time_M"]*60)
    }
    runTimeDisplay = parent.convertSecondsToTime(runTime)

    if (parent.checkValue(data,'Remain_Time_H')) {
      remainingTime += (data["Remain_Time_H"]*60*60)
      updateDataValue("remainHours", data["Remain_Time_H"].toString())
    } else {
      remainingTime += (getDataValue("remainHours") ?: "0").toFloat().toInteger()*60*60
    }
    if (parent.checkValue(data,'Remain_Time_M')) {
      remainingTime += (data["Remain_Time_M"]*60)
    }
    remainingTimeDisplay = parent.convertSecondsToTime(remainingTime)

    Date currentTime = new Date()
    use(groovy.time.TimeCategory) {
      currentTime = currentTime + (remainingTime as int).seconds
    }
    def finishTimeDisplay = currentTime.format("yyyy-MM-dd'T'HH:mm:ssZ", location.timeZone)

    if (parent.checkValue(data,'Reserve_Time_H')) {
      delayTime += (data["Reserve_Time_H"]*60*60)
      updateDataValue("reserveHours", data["Reserve_Time_H"].toString())
    } else {
      delayTime += (getDataValue("reserveHours") ?: "0").toFloat().toInteger()*60*60
    }
    if (parent.checkValue(data,'Reserve_Time_M')) {
      delayTime += (data["Reserve_Time_M"]*60)
    }
    delayTimeDisplay = parent.convertSecondsToTime(delayTime)

    if (parent.checkValue(data,'State')) {
      String currentStateName = parent.cleanEnumValue(data["State"], "@WM_STATE_")
      if (device.currentValue("currentState") != currentStateName) {
        if(logDescText) {
          log.info "${device.displayName} CurrentState: ${currentStateName}"
        } else {
          logger("info", "CurrentState: ${currentStateName}")
        }
      }
      sendEvent(name: "currentState", value: currentStateName)

      def currentStateSwitch = (currentStateName =~ /power off/ ? 'off' : 'on')
      if (device.currentValue("switch") != currentStateSwitch) {
        if(logDescText) {
            log.info "${device.displayName} Was turned ${currentStateSwitch}"
        } else {
          logger("info", "Was turned ${currentStateSwitch}")
        }
      }
      sendEvent(name: "switch", value: currentStateSwitch, descriptionText: "Was turned ${currentStateSwitch}")
    }

    sendEvent(name: "runTime", value: runTime, unit: "seconds")
    sendEvent(name: "runTimeDisplay", value: runTimeDisplay, unit: "hh:mm")
    sendEvent(name: "remainingTime", value: remainingTime, unit: "seconds")
    sendEvent(name: "remainingTimeDisplay", value: remainingTimeDisplay, unit: "hh:mm")
    sendEvent(name: "delayTime", value: delayTime, unit: "seconds")
    sendEvent(name: "delayTimeDisplay", value: delayTimeDisplay, unit: "hh:mm")
    sendEvent(name: "finishTimeDisplay", value: finishTimeDisplay, unit: "hh:mm")

    if (parent.checkValue(data, "reservation"))
      sendEvent(name: "reservation", value: (data["reservation"] =~ /ON/ ? "on" : "off"))
    if (parent.checkValue(data, "antiCrease"))
      sendEvent(name: "antiCrease", value: (data["antiCrease"] =~ /ON/ ? "on" : "off"))
    if (parent.checkValue(data, "childLock"))
      sendEvent(name: "childLock", value: (data["childLock"] =~ /ON/ ? "on" : "off"))
    if (parent.checkValue(data, "selfClean"))
      sendEvent(name: "selfClean", value: (data["selfClean"] =~ /ON/ ? "on" : "off"))
    if (parent.checkValue(data, "dampDryBeep"))
      sendEvent(name: "dampDryBeep", value: (data["dampDryBeep"] =~ /ON/ ? "on" : "off"))
    if (parent.checkValue(data, "handIron"))
      sendEvent(name: "handIron", value: (data["handIron"] =~ /ON/ ? "on" : "off"))
    if (parent.checkValue(data, "remoteStart"))
      sendEvent(name: "remoteStart", value: (data["remoteStart"] =~ /ON/ ? "on" : "off"))
    if (parent.checkValue(data, "standby"))
      sendEvent(name: "standby", value: (data["standby"] =~ /ON/ ? "on" : "off"))
    if (parent.checkValue(data, "dnnReady"))
      sendEvent(name: "dnnReady", value: (data["dnnReady"] =~ /ON/ ? "on" : "off"))
    if (parent.checkValue(data, "courseDryer24inchBase"))
      sendEvent(name: "courseDryer24inchBase", value: data["courseDryer24inchBase"])

    if (parent.checkValue(data,'Error')) {
      if(data["Error"] == "ERROR_NOERROR")
        sendEvent(name: "error", value: "noerror")
      else
        sendEvent(name: "error", value: parent.cleanEnumValue(data["Error"], "@WM_US_DRYER_ERROR_"))
    }

    if (parent.checkValue(data,'Course'))
        sendEvent(name: "course", value: data["Course"] != 0 ? data["Course"]?.toLowerCase() : "none")
    if (parent.checkValue(data,'SmartCourse'))
        sendEvent(name: "smartCourse", value: data["SmartCourse"] != 0 ? data["SmartCourse"]?.toLowerCase() : "none")
    if (parent.checkValue(data,'DryLevel')) {
      if(data["DryLevel"] == "@WM_TERM_NO_SELECT_W")
        sendEvent(name: "dryLevel", value: "no drylevel")
      else
        sendEvent(name: "dryLevel", value: parent.cleanEnumValue(data["DryLevel"], "@WM_DRY24_DRY_LEVEL_"))
    }
    if (parent.checkValue(data,'ecoHybrid')) {
      if(data["ecoHybrid"] == "@WM_TERM_NO_SELECT_W")
        sendEvent(name: "ecoHybrid", value: "no ecohybrid")
      else
        sendEvent(name: "ecoHybrid", value: parent.cleanEnumValue(data["ecoHybrid"], "@WM_DRY24_ECO_HYBRID_"))
    }
    if (parent.checkValue(data,'TempControl'))
        sendEvent(name: "temperatureLevel", value: parent.cleanEnumValue(data["TempControl"], "@WM_DRY24_TEMP_"))
    if (parent.checkValue(data,'TimeDry'))
        sendEvent(name: "timeDry", value: data["TimeDry"])
}

}