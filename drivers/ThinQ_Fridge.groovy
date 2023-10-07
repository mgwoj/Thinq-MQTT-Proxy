import groovy.util.logging.Slf4j

/**
 *  LG Fridge
 *
 *  Copyright 2020 Dominick Meglio
 *
 */
@Slf4j
class ThinQ_Fridge extends Device {

//metadata {
//    definition(name: "LG ThinQ Fridge", namespace: "dcm.thinq", author: "dmeglio@gmail.com") {
//        capability "Sensor"
//        capability "Initialize"
//        capability "ContactSensor"
//
//        attribute "fridgeTemp", "number"
//        attribute "freezerTemp", "number"
//        attribute "craftIceMode", "number"
//        attribute "icePlus", "string"
//        attribute "waterFilterStatus", "string"
//        attribute "freshAirFilterStatus", "string"
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

    if (data.DoorOpenState == "OPEN")
      sendEvent(name: "contact", value: "open")
    else if (data.DoorOpenState == "CLOSE")
      sendEvent(name: "contact", value: "closed")

    if (data.TempFreezer != null) {
      def temp = data.TempFreezer
      if (getTemperatureScale() == "C" && device.getDataValue("tempUnit") == "FAHRENHEIT")
        temp = fahrenheitToCelsius(temp)
      else if (getTemperatureScale() == "F" && device.getDataValue("tempUnit") == "CELSIUS")
        temp = celsiusToFahrenheit(temp)
      sendEvent(name: "freezerTemp", value: temp)
    }
    if (data.TempRefrigerator != null) {
      def temp = data.TempRefrigerator
      if (getTemperatureScale() == "C" && device.getDataValue("tempUnit") == "FAHRENHEIT")
        temp = fahrenheitToCelsius(temp)
      else if (getTemperatureScale() == "F" && device.getDataValue("tempUnit") == "CELSIUS")
        temp = celsiusToFahrenheit(temp)
      sendEvent(name: "fridgeTemp", value: temp)
    }

    if (data.craftIceMode) {
      if (data.craftIceMode == "@RE_TERM_CRAFT_6B_W")
        sendEvent(name: "craftIceMode", value: 6)
      else
        sendEvent(name: "craftIceMode", value: 3)
    }

    if (data.IcePlus) {
      sendEvent(name: "icePlus", value: parent.cleanEnumValue(data.IcePlus, "@CP_"))
    }

    if (data.WaterFilterUsedMonth) {
      sendEvent(name: "waterFilterStatus", value: parent.cleanEnumValue(data.WaterFilterUsedMonth, ["@RE_TERM_","@RE_STATE_"]))
    }

    if (data.FreshAirFilter) {
      sendEvent(name: "freshAirFilterStatus", value:
        parent.cleanEnumValue(data.FreshAirFilter, ["@RE_STATE_FRESH_AIR_FILTER_MODE_","@RE_FILTER_STATE_","@RE_STATE_"])
      )
    }
}

}