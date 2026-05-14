@echo off
setlocal EnableExtensions

set "SONAR_HOST_URL=http://localhost:9000"
set "TOKEN=%~1"

if "%TOKEN%"=="" set "TOKEN=%SONAR_TOKEN%"

if "%TOKEN%"=="" (
  echo Usage: run-sonar.bat YOUR_SONAR_TOKEN
  echo Or set SONAR_TOKEN, then run: run-sonar.bat
  exit /b 1
)

for %%S in (
  ai-service
  api-gateway
  auth-service
  eureka-server
  export-service
  jobmatch-service
  notification-service
  resume-service
  template-service
) do (
  call :run_service %%S
  if errorlevel 1 exit /b 1
)

echo.
echo All ResumePilot services passed JaCoCo checks and SonarQube analysis.
exit /b 0

:run_service
set "SERVICE=%~1"
echo.
echo ===== %SERVICE%: mvnw clean verify =====
pushd "%SERVICE%"
call mvnw.cmd clean verify
if errorlevel 1 (
  popd
  echo %SERVICE% failed during mvnw clean verify. JaCoCo coverage may be below 80%% or tests may be failing.
  exit /b 1
)

echo.
echo ===== %SERVICE%: SonarQube scan =====
call mvnw.cmd sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%TOKEN% -Dsonar.projectKey=resumepilot-%SERVICE% "-Dsonar.projectName=ResumePilot %SERVICE%"
if errorlevel 1 (
  popd
  echo %SERVICE% failed during SonarQube analysis.
  exit /b 1
)

popd
exit /b 0
