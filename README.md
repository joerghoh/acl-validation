# ACl Validation

This project enables you to check if your AEM principals have (or have not) certain permissions on the repository; it works independently of the permission setup itself but checks the resulting permissions of the principals.

## Motivation

When working with larger and complex permission setups, any change can cause unwanted side effects. And when changes are performed, often only a few permissions are tested afterwards, although the change might yield side-effects for other users and groups as well. Also the testing of these changes is often time-consuming, therefor it's neglected.

With the ACL Validation tool you can write rules which check the permissions of principals, so the result of any change can be evaluated quickly if it has an unknown side effect.
You can consider it as unit-tests for ACLs.

## Build & deploy

Like any AEM project which is based on an archetype you can build and deploy the code quite easily:

    mvn clean install -DdeployPackage -Daem.port=<port>
    
Out of the box the tool no code is active, but it requires an OSGI configuration.
    
## OSGI Configuration

Create an OSGI configuration for the PID `de.joerghoh.aem.aclvalidation.impl.Validator` and provide these elements:

* configurationPaths (multi-value): A repository path which contain the rule definitions. This path must point to a location below /apps (otherwise you would need to change the permission of the systemuser which is used to read these rules).
* checkOnActivate (boolean): if `true` the rules are checked on startup of the service.

## Rules

Rules define the checks which are executed to validate the resulting permissions of a `principal`; a principal can be a JCR user or a JCR group. These rules are stored in JSON files and reside in folders below /apps; the path to these folders must be provided by the OSGI configuration. A file must have the extension ".json".

The structure of the file is like this:

   [
     { "principal: "<the principal>",
       "itempath": "<the path to check>",
       "privileges": "<privilegest to check for>",
       "policy" : "<ALLOW|DENY>"
     },
     {
       ...
     }
   ] 

(It's a simple JSON array of tupels.)


