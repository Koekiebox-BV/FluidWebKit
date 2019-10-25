#!/usr/bin/env bash
mvn clean && mvn clean install -U

echo '[INFO] Updating jar in FlowJob'
rm -rfv /Users/jasonbruwer/Workspace/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidwebkit/fluidwebkit-*.jar
cp target/fluidwebkit-*.jar /Users/jasonbruwer/Workspace/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidbpmwebkit

rm -rfv /Users/jasonbruwer/Workspace/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidwebkit/fluidwebkit-*-sources.jar
rm -rfv /Users/jasonbruwer/Workspace/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidwebkit/fluidwebkit-*-javadoc.jar

echo '[INFO] Updating WildFly-17'
cp -fv /Users/jasonbruwer/Workspace/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidbpmwebkit/fluidwebkit-*.jar /Users/jasonbruwer/Applications/wildfly-17.0.0.Final/modules/com/fluidbpmwebkit/main/

echo '[INFO] *** *** *** ***'
echo '[INFO] * DONE. Date: '$(date)
echo '[INFO] *** *** *** ***'
