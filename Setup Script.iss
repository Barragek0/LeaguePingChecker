[Setup]
AppName=LeaguePingChecker
AppVersion=1.1
DefaultDirName={pf}\LeaguePingChecker
DefaultGroupName=LeaguePingChecker
UninstallDisplayIcon={app}\LeaguePingChecker.exe
Compression=lzma2
SolidCompression=yes
OutputDir=userdocs:LeaguePingChecker
OutputBaseFilename=LeaguePingChecker
PrivilegesRequired=admin

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "C:\Users\Alex\Dropbox\Other eclipse projects\LeaguePingChecker\LeaguePingChecker.exe"; DestDir: "{app}"
;Source: "C:\Users\Alex\Dropbox\Other eclipse projects\LeaguePingChecker\jre\*"; DestDir: "{app}\jre\"; Flags: recursesubdirs createallsubdirs -- bundled jre TODO
Source: "C:\Users\Alex\Dropbox\Other eclipse projects\LeaguePingChecker\smile.ico"; DestDir: "{app}"

[Icons]
Name: "{group}\LeaguePingChecker"; Filename: "{app}\LeaguePingChecker.exe"
Name: "{commondesktop}\LeaguePingChecker"; Filename: "{app}\LeaguePingChecker"; IconFilename: "{app}\smile.ico"; Tasks: desktopicon
