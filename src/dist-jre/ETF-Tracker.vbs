' ETF Holdings Tracker - GUI Launcher with Embedded JRE (VBScript)
' No Java installation required! Just double-click to run.

Option Explicit

Dim objFSO, objShell, strAppDir, strJavaExe, strJarFile, strCmdLine, objWshScriptExec

Set objFSO = CreateObject("Scripting.FileSystemObject")
Set objShell = CreateObject("WScript.Shell")

' Get the directory where this script is located
strAppDir = objFSO.GetParentFolderName(WScript.ScriptFullName)

' Path to embedded JRE's javaw.exe
strJavaExe = objFSO.BuildPath(strAppDir, "jre\bin\javaw.exe")

' Path to the application JAR
strJarFile = objFSO.BuildPath(strAppDir, "etf-holdings-tracker.jar")

' Check if embedded JRE exists
If Not objFSO.FileExists(strJavaExe) Then
    MsgBox "錯誤：內嵌 JRE 未找到於 " & strJavaExe & vbCrLf & vbCrLf & "請確保應用程式已正確解壓縮。", vbCritical, "ETF Holdings Tracker - 啟動失敗"
    WScript.Quit 1
End If

' Check if JAR exists
If Not objFSO.FileExists(strJarFile) Then
    MsgBox "錯誤：應用程式 JAR 未找到於 " & strJarFile, vbCritical, "ETF Holdings Tracker - 啟動失敗"
    WScript.Quit 1
End If

' Build the command line
strCmdLine = """" & strJavaExe & """ -Xms128m -Xmx512m --enable-preview -jar """ & strJarFile & """"

' Run the application without showing console window
objShell.Run strCmdLine, 0, False

WScript.Quit 0
