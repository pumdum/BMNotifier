var host; 
var Hostknow=false;

chrome.browserAction.onClicked.addListener(function() {
	if (!Hostknow){
		findHost(false);
	}
	if (Hostknow) {
		chrome.cookies.get({"url":getBmUrl(),"name":"BMHPS"},cookiesBMHPS);
		chrome.tabs.create({'url': getBmUrl()});
	}
});

function getBmUrl() {
  return "https://"+host;
}

function cookiesBMHPS(cookie){
	console.debug(cookie);
	if (cookie==null || cookie.value==""){
	// session expired or not created make new cookies
		console.debug("Cookies BM not present");
		setCookiesBMHPS(getHPS());
	}else{
		// already a cookies 
		//
		console.debug("cookies BM present");
	}	
}

function setCookiesBMHPS(hps){
	chrome.cookies.set({"url":getBmUrl(),"name":"BMHPS","value":hps,"path":"/","domain":host},function(cookie) {console.debug(cookie)});	
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
findHost(true);
chrome.cookies.get({"url":getBmUrl(),"name":"BMHPS"},cookiesBMHPS);
chrome.cookies.onChanged.addListener(function(changeInfo) {
console.debug(changeInfo.cookie);
if (changeInfo.removed && changeInfo.cookie.name=="BMHPS"){
	if (!Hostknow){
		findHost(false);
	}
	if (Hostknow) {
		setCookiesBMHPS(getHPS());
  }
}
});
