#!/usr/bin/env bash
mvn clean && mvn clean install -U -Dgpg.skip

echo '[INFO] Updating jar in FlowJob'

#PROPERTIES
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home

export WORKSPACE_HOME='/Users/jasonbruwer/GDrive/Workspace'
export WF_HOME='/Users/jasonbruwer/Applications/wildfly-20.0.1.Final'

rm -rfv $WORKSPACE_HOME/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidwebkit/fluidwebkit-*.jar
cp target/fluidwebkit-*.jar $WORKSPACE_HOME/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidbpmwebkit

rm -rfv $WORKSPACE_HOME/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidwebkit/fluidwebkit-*-sources.jar
rm -rfv $WORKSPACE_HOME/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidwebkit/fluidwebkit-*-javadoc.jar

echo '[INFO] Updating WildFly-20'
cp -fv $WORKSPACE_HOME/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidbpmwebkit/fluidwebkit-*.jar $WF_HOME/modules/com/fluidbpmwebkit/main/

echo '[INFO] *** *** *** ***'
echo '[INFO] * DONE. Date: '$(date)
echo '[INFO] *** *** *** ***'
