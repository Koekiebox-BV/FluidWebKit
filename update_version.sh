#!/bin/bash
CURRENT_VERSION=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

echo "Current Version : $CURRENT_VERSION"
read -p 'New Version: ' newVersionVar
mvn versions:set -DnewVersion=$newVersionVar -DgenerateBackupPoms=false
#do git things here

