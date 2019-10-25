/**
 *  Sonoff TH - Tasmota
 *
 *  Copyright 2019 Mark Hutchings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Sonoff TH - Tasmota", namespace: "mhutchy", author: "Mark Hutchings", mnmn: "SmartThings", vid: "generic-temperature") {
		capability "Switch"
		capability "Temperature Measurement"
        capability "Refresh"
        
        attribute "FriendlyName" , "string"
        attribute "Version" , "string"
        attribute "Mac" , "string"
        attribute "Hostname" , "string"
        attribute "Mac" , "string"        
        
	}
// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "generic", width: 2, height: 2, canChangeIcon: true){
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState("on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc")
				attributeState("off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff")
                			}
        }
        
        valueTile("temperature", "device.temperature", decoration: "flat", width: 4, height: 2) {
			state "temperature", label: '${currentValue} C'
		}
        
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		
		main "temperature"
		details(["switch","temperature","refresh"])
	}
    
 
 preferences {

		}

	simulator { 
		// TODO: define status and reply messages here
	}


}

// parse events into attributes

  //  Sonoff doesn't send any data so parse does not work

// handle commands



def installed(){
	log.debug "$device.displayName installed ${settings}"
    Update()
}

def updated(){
	log.debug "$device.displayName updated ${settings}"
    Update()
}

def initialise(){
	log.debug "$device.displayName initialising $evt"
  
  // as the sonoff doesn't send parse information we need to ask for updates every minute
  // You can remove this if you do not need updates, ie if the device state is not changed
  // outside of smartthings (by a switch, the Sonoff push button etc)
   runEvery1Minute(Update)
}	
    

def Update(){
	//log.debug "$device.displayName refreshing status"
    action("status%200")
    }

def parse(){
// If future versions of Tasmota support parsing
	log.debug "$device.displayName Parsing"
    log.debug "description: $description"
    }
    
def refresh() {
log.debug "$device.displayName $device.deviceNetworkId refreshing"
	Update()
    }

def push() {
	action("Power%20Toggle")
    runIn (5, Update)
	}

def on() {
	action ("Power%20On")
    runIn (5, Update)
    }

def off() {
	action("Power%20Off")
    runIn (5, Update)
}




//action - Sends command to Sonoff
//based on code found here https://community.smartthings.com/t/any-httpget-smatapps-or-code-anywhere/117945/4
def action(option)
	{
    
    state.action = option
    
	//log.debug "Sending command : '${option}' to $device.displayName at IP:${device.deviceNetworkId}"
    
    def theAction = new physicalgraph.device.HubAction("""GET /cm?cmnd=${option} HTTP/1.1\r\n Accept: */*\r\nHOST: ${device.deviceNetworkId}:80\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${device.deviceNetworkId}:80", [callback: calledBackHandler])

    sendHubCommand(theAction)
    

}

void calledBackHandler(physicalgraph.device.HubResponse hubResponse)
{
 
 state.status = hubResponse.body 
   //log.debug "$device.displayName responded with ${state.status}"
   def reportedState
   if (state.status == """{"POWER":"ON"}""") {
        reportedState = "on"}
   else     
   if (state.status == """{"POWER":"OFF"}""") {
     reportedState = "off"}
     
   else {
        def msg = new groovy.json.JsonSlurper().parseText(hubResponse.body)  
       
   		def SonoffPower = msg.Status.Power
        def SonoffTemperature = msg.StatusSNS.DS18B20.Temperature
        def SonoffIPAddress = msg.StatusNET.IPAddress
        def SonoffFriendlyName = msg.Status.FriendlyName
        def SonoffVersion = msg.StatusFWR.Version
        def SonoffHostname = msg.StatusNET.Hostname
        def SonoffMac = msg.StatusNET.Mac
        sendEvent(name: "temperature", value: "$SonoffTemperature")
        sendEvent(name: "FriendlyName", value: "$SonoffFriendlyName")
   		sendEvent(name: "Version", value: "$SonoffVersion")
        sendEvent(name: "Hostname", value: "$SonoffHostname")
        sendEvent(name: "Mac", value: "$SonoffMac")
        
        if (SonoffPower == 1) {reportedState = "on"} else {reportedState = "off"}
        //log.debug "$device.displayName UPDATE: Switched ${reportedState} ${SonoffTemperature}C ${SonoffFriendlyName} ${SonoffVersion} ${HostnaSonoffHostnameme} ${SonoffMac}"
        }

	sendEvent(name: "switch", value: reportedState)
    sendEvent(name: "refresh", value: "active")
    log.debug "$device.displayName is switched ${device.currentValue("switch")} : ${device.currentValue("temperature")}C"
}


//all status information can be found here https://github.com/arendst/Sonoff-Tasmota/wiki/Commands