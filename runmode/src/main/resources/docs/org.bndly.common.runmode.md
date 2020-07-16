# Runmode
## Introduction
Runmodes can be used to toggle different features and configurations of a pluripotent application. A runmode is defined by a simple technical name. This technical name appears in the provisioning of the application and may appear at the startup of the application. When a runmode provided at the startup of the application, the application will install those bundles and configurations, that are bound to the runmode.

## Defining runmodes
Names for runmodes should match to the pattern `[a-z0-9]+`. Having `,` or `.` symbols in the runmode name leads to conflicts.

In the provisioning of an application, the runmode name is defined as the most outer element. Here is an example, that shows a runmode called `dev`:

```
{
	"dev": {
		"bundles": {
			"25": [
				"com.acme:com.acme.foo:jar:1.0"
			]
		}
	}
}
```

In the provisioning the runmode element can also contain configuration elements. Here is an extended example:

```
{
	"dev": {
		"bundles": {
			"25": [
				"com.acme:com.acme.foo:jar:1.0"
			]
		},
		"configs": {
			"com.acme.foo.Bar": {
				"property": "someValue"
			}
		}
	}
}
```

This example would install the `com.acme.foo-1.0.jar` and the `com.acme.foo.Bar.cfg` if the runmode `dev` is active.
In a real world example the configuration of `com.acme.foo.Bar` could be different depending on the execution environment. Runmodes can be used to pre-package the different possible configurations right into the application.

Image the following scenario:
There are two environments `local` and `stage`. In both environments the bundle `com.acme.foo-1.0.jar` is required but the service `com.acme.foo.Bar` needs an environment specific configuration. Instead of filtering Maven properties into the provisioning we defer the configuration selection to the runtime by introducing two new runmodes:

```
{
	"dev": {
		"bundles": {
			"25": [
				"com.acme:com.acme.foo:jar:1.0"
			]
		}
	},
	"local": {
		"configs": {
			"com.acme.foo.Bar": {
				"property": "http://localhost"
			}
		}
	},
	"stage": {
		"configs": {
			"com.acme.foo.Bar": {
				"property": "http://stage.myfancyapp.com"
			}
		}
	}
}
```

With this provisioning we can start the application with the runmodes `dev,local` or `dev,stage` and we would get different configurations without having to rebuild the application.

## Activating runmodes at startup
The active runmodes can be defined in multiple different ways.

### Executable JAR
If the application jar is started via `java -jar` the active run modes can be defined by the system property `bndly.application.runmodes`.

For the example above this would lead to the following command: `java -Dbndly.application.runmodes=dev,local -jar app.jar`

### Implicit executable JAR or WAR
If the application jar is started, the runmodes can be predefined in the `config.properties`. Here is an example:

```
bndly.application.runmodes=dev,local
```

If the system property `bndly.application.runmodes` is defined, then it will override the value defined in the `config.properties`.

This kind of setup will be useful, when the application is packaged as a WAR, that runs in an application container such as Apache Tomcat.

### Maven
If the application is started with the `bndly-maven-plugin` by calling `bndly:run` or `bndly:start`, then the runmodes can be defined in the `configuration` of the plugin:

```
<plugin>
	<groupId>org.bndly.common</groupId>
	<artifactId>bndly-maven-plugin</artifactId>
	<extensions>true</extensions>
	<configuration>
		<runModes>dev,local</runModes>
	</configuration>
</plugin>
```

## Legacy Support
Existing application may not be aware of runmodes, but they already use the provisioning format, that allows runmode usage. Those applications need to be build with the `bndly-maven-plugin` and the following configuration:

```
<plugin>
	<groupId>org.bndly.common</groupId>
	<artifactId>bnldy-maven-plugin</artifactId>
	<extensions>true</extensions>
	<configuration>
		<runModeConfigurations>false</runModeConfigurations>
	</configuration>
</plugin>
```

The configuration value `runModeConfigurations` will change the provisioning behavior of the application. If `runModeConfigurations` is `true` (default), then the configuration files will be stored in `framework/conf/app/runmode-NAMEOFTHERUNMODE`.
The legacy applications may expect the configurations in `framework/conf/app`. This behavior can be activated by setting `runModeConfigurations` to `false`. If no `bndly.application.runmodes` property is available in the `config.properties` or in the system properties, then all bundles from all runmodes will be activated.

The assignment of a runmode to a bundle is stored within the start level folders `framework/auto-deploy/STARTLEVEL/runmode.properties`. If a bundle is manually added to a start level folder and not listed in the `runmode.properties`, then it will automatically activated.

__Important__: An application that has been packaged with `runModeConfigurations` set to `false` will not support runmodes at all. The application jar will not contain any runmode information.
