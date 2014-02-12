var host; 
var Hostknow=false;

chrome.browserAction.onClicked.addListener(function() {
	if (!Hostknow){
		findHost(false);
	}
	if (Hostknow) {
		chrome.tabs.create({'url': getBmUrl()});
	}
});

function onAlarm(alarm) {
	updatecookiesBMHPS();
}

function getBmUrl() {
  return "https://"+host+"/";
}

function updatecookiesBMHPS(){
	if (Hostknow) setCookiesBMHPS(getHPS());
	else console.debug("but server unknow");
}

function setCookiesBMHPS(hps){
	chrome.cookies.set({"url":getBmUrl(),"name":"BMHPS","value":hps,"path":"/"},function(cookie) {console.debug(cookie)});	
}

function getHPS(){
	var hps;
	var xhr = new XMLHttpRequest();
	xhr.open("GET", "http://localhost:51985/hps", false);
	xhr.onreadystatechange = function() {
		if (xhr.readyState == 4 && xhr.status>=200 & xhr.status<300) {
			// JSON.parse does not evaluate the attacker's scripts.
			var resp = JSON.parse(xhr.responseText);
			hps=resp.HPS;
			console.debug(hps);
			
		}
	};
	xhr.onerror= function() {
		Hostknow=false;
		updateIcon();
	};
	
	xhr.send();
	return hps;
}

function updateIcon() {
  if (Hostknow) {
    chrome.browserAction.setIcon({path:"bm.png"});
    //chrome.browserAction.setBadgeText({text:"Go to "+ host});
  } else {
    chrome.browserAction.setIcon({path: "bmwarning.png"});
    //chrome.browserAction.setBadgeText({  text: "host unknow"  });
  }
}

function findHost(async){	
	var xhr = new XMLHttpRequest();
	xhr.open("GET", "http://localhost:51985/host", async);
	xhr.onreadystatechange = function() {
		if (xhr.readyState == 4 && xhr.status>=200 & xhr.status<300) {
			// JSON.parse does not evaluate the attacker's scripts.
			var resp = JSON.parse(xhr.responseText);
			host=resp.HOST;
			console.debug(host);
			Hostknow=true;
			updateIcon();
		}
	};
	xhr.onerror= function() {
		Hostknow=false;
		updateIcon();
	};

	xhr.send();
}
updateIcon();
findHost(false);
updatecookiesBMHPS();
chrome.cookies.onChanged.addListener(function(changeInfo) {
if (changeInfo.removed && changeInfo.cookie.name=="BMHPS" && changeInfo.cause!="overwrite") {
	if (!Hostknow){
		findHost(false);
	}
	if (Hostknow) {
		setCookiesBMHPS(getHPS());
  }
}
});
chrome.alarms.create('refresh', {periodInMinutes: 5});
chrome.alarms.onAlarm.addListener(onAlarm);
