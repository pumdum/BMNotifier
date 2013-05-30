var host; 
var hps; 


chrome.browserAction.onClicked.addListener(function() {
	chrome.tabs.create({'url': "https://"+host});
});

function checkForBMUrl(tabId, changeInfo, tab) {
  // If the letter 'g' is found in the tab's URL...
  if (tab.url.indexOf(host) > -1) {
    console.info("open bluemind");
	chrome.cookies.get({"url":tab.url,"name":"BMHPS"},cookiesBMHPS);
  }
};

function cookiesBMHPS(cookie){
	if (cookie==null){
		console.info("BM NOT LOGGED");
	}else{
		console.info("BM LOGGED");
		newHps = getHPS();
		setCookiesBMHPS(newHps);
	}
}
function setCookiesBMHPS(hps, url){
	chrome.cookies.set({"url":"https://"+host,"name":"BMHPS","value":hps,"path":"/"});	
}


function getHPS(){
xhr.open("GET", "http://localhost:51985/hps", false);
xhr.onreadystatechange = function() {
  if (xhr.readyState == 4) {
    // JSON.parse does not evaluate the attacker's scripts.
    var resp = JSON.parse(xhr.responseText);
	hps=resp.HPS;
	console.info(hps);
	
  }
}
xhr.send();
return hps;
}

// Listen for any changes to the URL of any tab.
chrome.tabs.onUpdated.addListener(checkForBMUrl);
	
var xhr = new XMLHttpRequest();
xhr.open("GET", "http://localhost:51985/host", true);
xhr.onreadystatechange = function() {
  if (xhr.readyState == 4) {
    // JSON.parse does not evaluate the attacker's scripts.
    var resp = JSON.parse(xhr.responseText);
	host=resp.HOST;
	console.info(host);
	
  }
}
xhr.send();