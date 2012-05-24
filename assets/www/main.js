var sb;
function TestSlaveBrowser() {
  var client_id="GET YOUTR OWN, BUDDY.apps.googleusercontent.com";
  var state="dda";
  var myTitle="Google oAuth2";
  var redirect_uri="http://localhost&scope=http://maps.google.com/maps/feeds/";
  sb = new SlaveBrowser();
  sb.showWebPage(
    "https://accounts.google.com/o/oauth2/auth?response_type=code&client_id="+ client_id +"&state="+ state +"&redirect_uri="+ redirect_uri, {showLocationBar:true}, myTitle
  );
}


function oAuth2Failed() {
  console.log('oAuth2Failed');
  document.getElementById('info').innerHTML='login failed';
  cb.close();
}

function oAuth2Success(token) {
  console.log("I can haz code: "+token);
  document.getElementById('info').innerHTML=token;
  cb.close();
}

function init() {
//  document.addEventListener("deviceready", TestSlaveBrowser, true);
}