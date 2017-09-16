[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](http://opensource.org/licenses/MIT)&#8194;&#8194;
[![upload-pgyer](https://img.shields.io/badge/upload--pgyer-1.24-brightgreen.svg)](https://github.com/myroid/jenkins-upload-pgyer-plugin)&#8194;&#8194;
[![downloads](https://img.shields.io/badge/downloads-%3C1K-orange.svg)](https://github.com/myroid/jenkins-upload-pgyer-plugin)&#8194;&#8194;
[![blog](https://img.shields.io/badge/blog-dafan.tech-red.svg)](http://dafan.tech)

### upload-pgyer-jenkins-plugin

[中文文档](./README_cn.md)

Pgyer's official website is [https://www.pgyer.com/](https://www.pgyer.com/)

Pgyer can upload the application to the site, generate installation link and qr code user to open the installation link, or scan code qr code, can start installation.

So this plugin can be uploaded to the pgyer platform！**And it can put the fields returned by pgyer into an environment variable, which you can use in other build steps**, You can select `upload to pgyer` by adding build steps or adding post-build steps.

Screenshot
![](./img/upload-pgyer.png)

field|explanation
----:|:----------
pgyer uKey|`(Required)` User Key, used to identify the current user's identity, <br/>for the same pgyer registered users, the value of the fixed!<br/>[Click to get pgyer uKey](https://www.pgyer.com/account/api)
pgyer api_key|`(Required)` API Key, used to identify the identity of the API caller, <br/>if not specified, each interface needs to contain this parameter.<br/>For the same pgyer registered users, this value is fixed.<br/>[Click to get pgyer api_key](https://www.pgyer.com/account/api)
scandir|`(Required)` need to upload ipa or apk file base dir path!<br/>  The default is ${WORKSPACE}, It means the path of the current project!<br/>It is using ant's DirectoryScanner class, [click to see DirectoryScanner class](https://ant.apache.org/manual/api/org/apache/tools/ant/DirectoryScanner.html)<br/>**It is equivalent to the parameters of the basedir method in the DirectoryScanner class!** [click to see basedir method](https://ant.apache.org/manual/api/org/apache/tools/ant/DirectoryScanner.html#basedir)
file wildcard|`(Required)` need to upload ipa or apk file name, Support wildcards,<br/>like this: **/Test?/*.apk<br/>It is using ant's DirectoryScanner class, [click to see DirectoryScanner class](https://ant.apache.org/manual/api/org/apache/tools/ant/DirectoryScanner.html)<br/> **It is equivalent to the parameters of the includes method in the DirectoryScanner class!** [click to see includes method](https://ant.apache.org/manual/api/org/apache/tools/ant/DirectoryScanner.html#includes)
installType|`(Optional)` application installation, the value is (1,2,3).<br/>1: public, <br/>2: password installed, <br/>3: invitation to install.<br/>The default is 1 public!
password|(Optional) set the App installation password, if you do not want to set the password, please pass empty string, or not pass.
updateDescription|`(Optional)` version update description, please pass empty string, or not pass.
qrcodePath|`(Optional)` If you need to download the qrcode, please enter the save path of the qrcode!otherwise, not download!
envVarsPath|`(Optional)` if you need to save info, please enter save file path! otherwise, not save!

Running log
![](./img/upload-pgyer-2.png)

When it runs successfully, you can use the environment variables that are used! for example:

![](./img/upload-pgyer-3.png)

environment variables|explanation
----:|:----------
appKey|App Key
appType|Application type (1:iOS; 2: Android)
appIsLastest|Is it the latest version (1: yes; 2: no)
appFileSize|App file size
appName|App Name
appVersion|App Version
appVersionNo|For Android version Numbers, iOS is always 0
appBuildVersion|pgyer builds build Numbers that distinguish historical versions
appIdentifier|Application package name, iOS for BundleId, Android for package name
appIcon|Application the icon of the key, get the address http://o1wh05aeh.qnssl.com/image/view/app_icons/[appIcon]
appDescription|Introduction to the Application
appUpdateDescription|Application update description
appScreenshots|Application the screenshot of the key, get the address http://o1whyeemo.qnssl.com/image/view/app_screenshots/[appScreenshots]
appShortcutUrl|Application short links
appCreated|Application upload time
appUpdated|Application update time
appQRCodeURL|Application the qr code address
appPgyerURL|Application pgyer url
appBuildURL|Application build pgyer url