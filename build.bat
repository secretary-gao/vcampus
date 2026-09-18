@echo off
rem Compile the whole project (Common + Server + Client) into bin\.
rem Double-click to run, or it is called by run-server.bat / run-client.bat / start-all.bat.
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"

if not exist bin mkdir bin

echo [1/3] Collecting source files...
set "SOURCES_LIST=%TEMP%\vcampus_sources_%RANDOM%.txt"
rem javac's @argfile parser treats backslashes inside quotes as escapes,
rem so paths are written with forward slashes to keep it safe.
for /r "Common\src" %%f in (*.java) do (set "p=%%f" & set "p=!p:\=/!" & echo "!p!">>"%SOURCES_LIST%")
for /r "Server\src" %%f in (*.java) do (set "p=%%f" & set "p=!p:\=/!" & echo "!p!">>"%SOURCES_LIST%")
for /r "Client\src" %%f in (*.java) do (set "p=%%f" & set "p=!p:\=/!" & echo "!p!">>"%SOURCES_LIST%")

echo [2/3] Compiling...
javac -encoding UTF-8 -d bin -cp "lib\mysql-connector-j-9.7.0.jar" --module-path "lib\javafx\lib" --add-modules javafx.controls,javafx.fxml "@%SOURCES_LIST%"

set "BUILD_RESULT=%ERRORLEVEL%"
del "%SOURCES_LIST%" >nul 2>&1

if not "%BUILD_RESULT%"=="0" (
  echo.
  echo Build FAILED, see errors above.
  exit /b 1
)

echo [3/3] Copying UI resources...
copy /y "Client\src\vcampus\client\view\seu_logo.jpeg" "bin\vcampus\client\view\seu_logo.jpeg" >nul
if errorlevel 1 (
    echo Resource copy FAILED.
    exit /b 1
)
copy /y "Client\src\vcampus\client\view\student-management.css" "bin\vcampus\client\view\student-management.css" >nul
if errorlevel 1 (
  echo Student stylesheet copy FAILED.
  exit /b 1
)
if not exist "bin\vcampus\client\view\course" mkdir "bin\vcampus\client\view\course"
copy /y "Client\src\vcampus\client\view\course\course.css" "bin\vcampus\client\view\course\course.css" >nul
if errorlevel 1 (
  echo Course stylesheet copy FAILED.
  exit /b 1
)
rem 商店模块样式表（虚拟商店）
copy /y "Client\src\vcampus\client\view\store.css" "bin\vcampus\client\view\store.css" >nul
if errorlevel 1 (
  echo Store stylesheet copy FAILED.
  exit /b 1
)
rem 图片资源目录（校徽、登录背景轮播、医院药品图、商店商品图、AI 图标与背景图）
rem 这些现在都通过 classpath getResource 读取，必须随源码一起拷进 bin，才能打包进 jar。
xcopy /e /i /y "Client\src\vcampus\client\view\assets" "bin\vcampus\client\view\assets" >nul
if errorlevel 1 (
  echo Assets copy FAILED.
  exit /b 1
)

echo Build succeeded.
exit /b 0
