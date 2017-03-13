Camel ISDS Component
=======================
[![Build Status](https://travis-ci.org/czgov/camel-isds.svg?branch=master)](https://travis-ci.org/czgov/camel-isds)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.czgov/camel-isds/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cz.abclinuxu.datoveschranky/isds)
[![Javadocs](http://javadoc.io/badge/com.github.czgov/camel-isds.svg)](http://javadoc.io/doc/com.github.czgov/camel-isds)


ISDS is shortcut for system run by Czech government called _Informační systém datových schránek_.

This camel components allows easy sending and receiving of messages to ISDS.  
Underneath this component leverages library [Java ISDS](https://github.com/czgov/java-isds).

Component is currently under development and any contribution is welcome and appreciated.

## Examples

Camel route in Java DSL for downloading of `*.zfo` messages to directory `dir-with-zfo-files`:
```java
from("isds:messages?environment=test&username=YOUR_LOGIN&password=YOUR_PASSWORD?zfo=true")
	.log("New message ${body}")
	.to("file:dir-with-zfo-files");
```

See component documentation [here](src/main/docs/isds.adoc).


## OSGi ready
Component is ready for OSGi deployment.  
Example of deployment into JBoss Fuse 6.3:
```bash
# download features.xml from maven central
JBossFuse:karaf@root> features:addurl mvn:com.github.czgov/camel-isds/0.1.0/xml/features

# download jars and install into Fuse
JBossFuse:karaf@root> features:install camel-isds

# verify features are installed
JBossFuse:karaf@root> features:list | grep isds
[installed  ] [1.1.0                ] isds                                          javaisds-1.1.0                         
[installed  ] [0.1.0-SNAPSHOT       ] camel-isds                                    camel-isds-0.1.0   
```

## How to contribute

Pull requests are welcome and appreciated as well as github issue reports.

### Building 
Use maven to build project from sources:
```shell
# skip tests if you don't have login credentials for test accounts in ISDS system
mvn install -DskipTests

# optionally run tests during the build
mvn install
```

To run tests it's required to **have 3 test (staging) accounts**.  
Login credentials should be defined in file `isds-config.properties` in project root (next to `pom.xml`).

Example content of `isds-config.properties`:
```properties
# type of account - FO (fyzicka osoba)
isds.fo.id=ID_OF_DATABOX
isds.fo.login=USERNAME_OF_DATABOX
isds.fo.password=PASSWORD_OF_DATABOX

# type of account - FO (fyzicka osoba)
isds.fo2.id=ID_OF_DATABOX
isds.fo2.login=USERNAME_OF_DATABOX
isds.fo2.password=PASSWORD_OF_DATABOX

# type of account - OVM (organ verejne moci)
isds.ovm.id=ID_OF_DATABOX
isds.ovm.login=USERNAME_OF_DATABOX
isds.ovm.password=PASSWORD_OF_DATABOX
```

### Getting access to ISDS

There is production and "staging" environment. 
Staging environment is commonly used for integration tests with ISDS.

To get access to production system one needs to personally file a request.
More information on page https://www.datoveschranky.info/zakladni-informace/zrizeni-datove-schranky-na-zadost.

To get access to test (staging) system one needs to download `*.zfo` form from page
https://www.datoveschranky.info/o-datovych-schrankach/vyzkousejte-si-datovou-schranku
and send it either directly from Form Filler (if one already has his ISDS account) 
or send via e-mail to "Ministerstvo vnitra" (`posta@mvcr.cz`).
