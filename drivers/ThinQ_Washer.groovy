import groovy.util.logging.Slf4j

/**
 *  LG Washer
 *
 *  Copyright 2020 Dominick Meglio
 *
 */
@Slf4j
class ThinQ_Washer extends Device {

//    metadata {
//        definition(name: "LG ThinQ Washer", namespace: "dcm.thinq", author: "dmeglio@gmail.com") {
//            capability "Sensor"
//            capability "Switch"
//            capability "Initialize"
//
//            attribute "runTime", "number"
//            attribute "runTimeDisplay", "string"
//            attribute "remainingTime", "number"
//            attribute "remainingTimeDisplay", "string"
//            attribute "delayTime", "number"
//            attribute "delayTimeDisplay", "string"
//            attribute "finishTimeDisplay", "string"
//            attribute "currentState", "string"
//            attribute "error", "string"
//            attribute "course", "string"
//            attribute "smartCourse", "string"
//            attribute "remoteStart", "string"
//            attribute "soilLevel", "string"
//            attribute "spinSpeed", "string"
//            attribute "temperatureLevel", "string"
//            attribute "temperatureTarget", "string"
//            attribute "doorLock", "string"
//        }
//
//        preferences {
//            section { // General
//                input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: false
//                input name: "logDescText", title: "Log Description Text", type: "bool", defaultValue: false, required: false
//            }
//        }
//    }

def processStateData(data) {
    logger("debug", "processStateData(${data})")
    super.processStateData(data)

    if (parent.checkValue(data,'Initial_Time_H') || parent.checkValue(data,'Initial_Time_M')) {
        def runTime = 0
        if (parent.checkValue(data, 'Initial_Time_H')) {
            runTime += (data["Initial_Time_H"] * 60 * 60)
            updateDataValue("initialHours", data["Initial_Time_H"].toString())
        } else {
            runTime += (getDataValue("initialHours") ?: "0").toFloat().toInteger()*60*60
        }
        if (parent.checkValue(data, 'Initial_Time_M')) {
            runTime += (data["Initial_Time_M"] * 60)
        }
        def runTimeDisplay = parent.convertSecondsToTime(runTime)
        sendEvent(name: "runTime", value: runTime, unit: "seconds")
        sendEvent(name: "runTimeDisplay", value: runTimeDisplay, unit: "hh:mm")
    }

    if (parent.checkValue(data,'Remain_Time_H') || parent.checkValue(data,'Remain_Time_M')) {
        def remainingTime = 0
        if (parent.checkValue(data, 'Remain_Time_H')) {
            remainingTime += (data["Remain_Time_H"] * 60 * 60)
            updateDataValue("remainHours", data["Remain_Time_H"].toString())
        } else {
            remainingTime += (getDataValue("remainHours") ?: "0").toFloat().toInteger()*60*60
        }
        if (parent.checkValue(data, 'Remain_Time_M')) {
            remainingTime += (data["Remain_Time_M"] * 60)
        }
        def remainingTimeDisplay = parent.convertSecondsToTime(remainingTime)

        Date currentTime = new Date()
        use(groovy.time.TimeCategory) {
            currentTime = currentTime + (remainingTime as int).seconds
        }
        def finishTimeDisplay = currentTime.format("yyyy-MM-dd'T'HH:mm:ssZ", location.timeZone)
        sendEvent(name: "remainingTime", value: remainingTime, unit: "seconds")
        sendEvent(name: "remainingTimeDisplay", value: remainingTimeDisplay, unit: "hh:mm")
        sendEvent(name: "finishTimeDisplay", value: finishTimeDisplay, unit: "hh:mm")
    }

    if (parent.checkValue(data,'Reserve_Time_H') || parent.checkValue(data,'Reserve_Time_M')) {
        def delayTime = 0
        if (parent.checkValue(data, 'Reserve_Time_H')) {
            delayTime += (data["Reserve_Time_H"] * 60 * 60)
            updateDataValue("reserveHours", data["Reserve_Time_H"].toString())
        } else {
            delayTime += (getDataValue("reserveHours") ?: "0").toFloat().toInteger()*60*60
        }
        if (parent.checkValue(data, 'Reserve_Time_M')) {
            delayTime += (data["Reserve_Time_M"] * 60)
        }
        def delayTimeDisplay = parent.convertSecondsToTime(delayTime)
        sendEvent(name: "delayTime", value: delayTime, unit: "seconds")
        sendEvent(name: "delayTimeDisplay", value: delayTimeDisplay, unit: "hh:mm")
    }

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

    if (parent.checkValue(data,'Error')) {
      sendEvent(name: "error", value: data["Error"].toLowerCase())
    }

    if (parent.checkValue(data,'Course'))
        sendEvent(name: "course", value: data["Course"] != 0 ? data["Course"]?.toLowerCase() : "none")
    if (parent.checkValue(data,'SmartCourse'))
        sendEvent(name: "smartCourse", value: data["SmartCourse"] != 0 ? data["SmartCourse"]?.toLowerCase() : "none")
    if (parent.checkValue(data,'remoteStart'))
        sendEvent(name: "remoteStart", value: parent.cleanEnumValue(data["remoteStart"], "@CP_"))
    if (parent.checkValue(data,'Soil'))
        sendEvent(name: "soilLevel", value: parent.cleanEnumValue(data["Soil"], ["@WM_.*_OPTION_SOIL_","@WM_.*_OPTION_WASH_", "@WM_TERM_"]))
    if (parent.checkValue(data,'SpinSpeed'))
        sendEvent(name: "spinSpeed", value: parent.cleanEnumValue(data["SpinSpeed"], ["@WM_.*_OPTION_SPIN_","@WM_TERM_"]))
    if (parent.checkValue(data,'TempControl'))
        sendEvent(name: "temperatureLevel", value: parent.cleanEnumValue(data["TempControl"], ["@WM_.*_OPTION_TEMP_","@WM_TERM_"]))
    if (parent.checkValue(data,'doorLock'))
        sendEvent(name: "doorLock", value: parent.cleanEnumValue(data["doorLock"], "@CP_"))
    if (parent.checkValue(data,'temp'))
        sendEvent(name: "temperatureTarget", value: data["temp"]?.split("_")?.getAt(1))
}

}