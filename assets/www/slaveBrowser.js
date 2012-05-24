/*
 * cordova is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) 2005-2010, Nitobi Software Inc.
 * Copyright (c) 2011, IBM Corporation
 */

/**
 * Constructor
 */
function SlaveBrowser() {
  this.currentLocation='';
};

SlaveBrowser.CLOSE_EVENT = 0;
SlaveBrowser.LOCATION_CHANGED_EVENT = 1;
SlaveBrowser.PAGE_LOADED = 2;

SlaveBrowser.prototype.onPageLoaded=function(html) {
//  http://localhost/?state=dda&amp;code=
  if(this.currentLocation.match(/^http:\/\/localhost/)!=null) {
    console.log("\n\nWe're in the right page: "+this.currentLocation);
    console.log(this.currentLocation.match("http://localhost"));
    // we be in da right place, bro
    var patt=new RegExp(/http:\/\/localhost\/[^;]+;code=([^"]+)/);
    a=patt.exec(html);
    if(a==null) {
      oAuth2Failed();
    } else {
      oAuth2Success(a[1]);
    }
  } else {
  console.log("\n\nNot the right page: "+this.currentLocation);
  }
}

SlaveBrowser.prototype.onLocationChange=function(location) {
  this.currentLocation=location;
  console.log("\n\nPage changed to: "+location);
}


/**
 * Display a new browser with the specified URL.
 * This method loads up a new web view in a dialog.
 *
 * @param url           The url to load
 * @param options       An object that specifies additional options
 */
SlaveBrowser.prototype.showWebPage = function(url, options, myNewTitle) {
    if (options === null || options === "undefined") {
        var options = new Object();
        options.showLocationBar = true;
    }
    cordova.exec(this._onEvent, this._onError, "SlaveBrowser", "showWebPage", [url, options, myNewTitle]);
};

/**
 * Close the browser opened by showWebPage.
 */
SlaveBrowser.prototype.close = function() {
    cordova.exec(null, null, "SlaveBrowser", "close", []);
};

/**
 * Display a new browser with the specified URL.
 * This method starts a new web browser activity.
 *
 * @param url           The url to load
 * @param usecordova   Load url in cordova webview [optional]
 */
SlaveBrowser.prototype.openExternal = function(url, usecordova) {
    if (usecordova === true) {
        navigator.app.loadUrl(url);
    }
    else {
        cordova.exec(null, null, "SlaveBrowser", "openExternal", [url, usecordova]);
    }
};

/**
 * Method called when the child browser has an event.
 */
SlaveBrowser.prototype._onEvent = function(data) {
    if (data.type == SlaveBrowser.CLOSE_EVENT && typeof window.plugins.slaveBrowser.onClose === "function") {
        window.plugins.slaveBrowser.onClose();
    }
    if (data.type == SlaveBrowser.LOCATION_CHANGED_EVENT && typeof window.plugins.slaveBrowser.onLocationChange === "function") {
        window.plugins.slaveBrowser.onLocationChange(data.location);
    }
    if (data.type == SlaveBrowser.PAGE_LOADED && typeof window.plugins.slaveBrowser.onPageLoaded === "function") {
        window.plugins.slaveBrowser.onPageLoaded(data.html);
    }
};

/**
 * Method called when the child browser has an error.
 */
SlaveBrowser.prototype._onError = function(data) {
    if (typeof window.plugins.slaveBrowser.onError === "function") {
        window.plugins.slaveBrowser.onError(data);
    }
};

/**
 * Maintain API consistency with iOS
 */
SlaveBrowser.install = function(){
    return window.plugins.slaveBrowser;
};

/**
 * Load SlaveBrowser
 */
cordova.addConstructor(function() {
    cordova.addPlugin("slaveBrowser", new SlaveBrowser());
});
