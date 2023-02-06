#! /bin/bash
TO_INSERT="<Valve className=\"org.apache.catalina.valves.ErrorReportValve\" showReport=\"false\" showServerInfo=\"false\" />"
SERVER_XML="/etc/tomcat/server.xml" 
END_HOST="</Host>"
COUNT="$(grep -c  "$TO_INSERT"  $SERVER_XML)"
echo $COUNT
if [ $COUNT = 0 ]
	then
		sed -i "s|$END_HOST|$TO_INSERT\n\t$END_HOST|" $SERVER_XML
		echo "Inserted value into $SERVER_XML"
	else
		echo "$SERVER_XML already contains the value"
 fi