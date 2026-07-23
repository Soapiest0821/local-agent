@echo off
cd /d "C:\4rp2\dev\developing\assistant"
javaw --module-path "C:\Java Module\javafx-sdk-21.0.11\lib" --add-modules javafx.controls,javafx.fxml -jar target\myapp-1.0-SNAPSHOT.jar
