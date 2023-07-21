# sle\_proxy_server

Must set the file path context parameters in `web.xml` to actual file locations. File `src/main/webapp/WEB-INF/web.xml` currently has the following context parameter defined:


```
	<context-param>
		<param-name>sle-interface-internal-config-file</param-name>
		<param-value>/ammos/ampcs/sle_proxy/config/sle-interface-internal-config.properties</param-value>
	</context-param>
	<context-param>
		<param-name>sle-interface-profiles-file</param-name>
		<param-value>/ammos/ampcs/sle_proxy/config/sle-interface-profiles.properties</param-value>
	</context-param>
	<context-param>
		<param-name>sle-interface-passwords-file</param-name>
		<param-value>/ammos/ampcs/sle_proxy/config/sle-interface-passwords.properties</param-value>
	</context-param>
		<context-param>
		<param-name>chill-interface-config-file</param-name>
		<param-value>/ammos/ampcs/sle_proxy/config/chill-interface-config.properties</param-value>
	</context-param>
```

Here are the example contents:

## sle-interface-profiles.properties
```
dsnsle-test.INTERFACE_TYPE=RETURN_ALL
dsnsle-test.PROVIDER_NAME=dsnsle-test-provider
dsnsle-test.PROVIDER_HOST=opsana17.jpl.nasa.gov
dsnsle-test.PROVIDER_PORT=24565
dsnsle-test.PROVIDER_AUTHENTICATION_MODE=NONE
dsnsle-test.SERVICE_INSTANCE_ID=sagr=yaddayadda
dsnsle-test.USER_NAME=myuser
dsnsle-test.USER_AUTHENTICATION_MODE=ALL
dsnsle-test.SERVICE_MODE=COMPLETE_ONLINE

uplink-prof.INTERFACE_TYPE=FORWARD
uplink-prof.PROVIDER_NAME=an-uplink-provider
uplink-prof.PROVIDER_HOST=uplinkserver.jpl.nasa.gov
uplink-prof.PROVIDER_PORT=8823
uplink-prof.PROVIDER_AUTHENTICATION_MODE=ALL
uplink-prof.SERVICE_INSTANCE_ID=sagr=uplinkdsn
uplink-prof.USER_NAME=uplinkuser
uplink-prof.USER_AUTHENTICATION_MODE=ALL
uplink-prof.SERVICE_MODE=FIFO
```

## sle-interface-passwords.properties
```
dsnsle-test.USER_PASSWORD=blabla
dsnsle-test.PROVIDER_PASSWORD=hi-ho
uplink-prof.USER_PASSWORD=uplinkuser
uplink-prof.PROVIDER_PASSWORD=yeyeye
```

## chill-interface-config.properties
```
DOWNLINK_HOST=localhost
DOWNLINK_PORT=23232
SESSION=4
DATABASE_HOST=dbhost
DATABASE_PORT=3344
UPLINK_LISTENING_PORT=8899
```

## sle-interface-config.properties
```
SERVICE_VERSION=3
FORWARD_BIND_UNBIND_TIMEOUT_MILLIS=6000
FORWARD_START_STOP_TIMEOUT_MILLIS=6000
FORWARD_GET_PARAMETER_TIMEOUT_MILLIS=4000
FORWARD_PEER_ABORT_TIMEOUT_MILLIS=2000
FORWARD_THROW_EVENT_TIMEOUT_MILLIS=6000
# Currently only the DSN scheme is supported below. The DSN scheme is based on
# 820-013 0239-Telecomm document's Table 3–2 "FCLTU Service Throw Events"
FORWARD_THROW_EVENT_SCHEME=DSN
# Below is a comma-delimited list of bitrates in their exact string character form
FORWARD_THROW_EVENT_ALLOWABLE_BITRATES=7.8125,15.625,16.000,31.250,32.000,62.500,125.000,250.000,500.000,1,000.000,2,000.000,4,000.000
# Below is a pair of integers, separated by a comma, that specifies the range of modulation index
FORWARD_THROW_EVENT_ALLOWABLE_MODINDEX_RANGE=200,1570
```