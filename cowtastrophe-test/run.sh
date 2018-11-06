#!/bin/sh
java -server -Xmx4096m -XX:-UseGCOverheadLimit -XX:+UseParallelGC -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Done-jar.silent=true -jar target/cowtastrophe-test-0.0.0.one-jar.jar $*

