#!/usr/bin/env bash
mvn clean && mvn clean install -U -Dgpg.skip

echo '[INFO] Updating jar in FlowJob'

#PROPERTIES
export WORKSPACE_HOME='/home/jbruwer/GoogleDrive/Workspace'
export WF_HOME='/home/jbruwer/Applications/wildfly-20.0.1.Final'

rm -rfv $WORKSPACE_HOME/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidwebkit/fluidwebkit-*.jar
cp target/fluidwebkit-*.jar $WORKSPACE_HOME/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidbpmwebkit

rm -rfv $WORKSPACE_HOME/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidwebkit/fluidwebkit-*-sources.jar
rm -rfv $WORKSPACE_HOME/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidwebkit/fluidwebkit-*-javadoc.jar

echo '[INFO] Updating WildFly-20'
cp -fv $WORKSPACE_HOME/FlowJob/flow-job-setup/docker/koekiebox/fluid_base/external_lib/com/fluidbpmwebkit/fluidwebkit-*.jar $WF_HOME/modules/com/fluidbpmwebkit/main/

echo '[INFO] *** *** *** ***'
echo '[INFO] * DONE. Date: '$(date)
echo '[INFO] *** *** *** ***'
