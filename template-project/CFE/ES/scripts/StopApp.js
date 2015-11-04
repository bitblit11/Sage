importPackage(Packages.org.csstudio.opibuilder.scriptUtil);
importPackage(Packages.org.yamcs.studio.script);

var appName = display.getWidget("inAppName").getPropertyValue("text");

Yamcs.issueCommand('/CFS/CFE_ES/StopApp(Name: ' + appName + ')');
