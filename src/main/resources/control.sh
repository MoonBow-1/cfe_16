#!/usr/bin/bash
#
# HTTP Event Capture to RFC5424 CFE_16
# Copyright (C) 2019-2025 Suomen Kanuuna Oy
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
#
# Additional permission under GNU Affero General Public License version 3
# section 7
#
# If you modify this Program, or any covered work, by linking or combining it
# with other code, such other code is not for that reason alone subject to any
# of the requirements of the GNU Affero GPL version 3 as long as this Program
# is the same Program as licensed from Suomen Kanuuna Oy without any additional
# modifications.
#
# Supplemented terms under GNU Affero General Public License version 3
# section 7
#
# Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
# versions must be marked as "Modified version of" The Program.
#
# Names of the licensors and authors may not be used for publicity purposes.
#
# No rights are granted for use of trade names, trademarks, or service marks
# which are in The Program if any.
#
# Licensee must indemnify licensors and authors for any liability that these
# contractual assumptions impose on licensors and authors.
#
# To the extent this program is licensed as part of the Commercial versions of
# Teragrep, the applicable Commercial License may apply to this file if you as
# a licensee so wish it.
#

ulimit -n 65535

RETVAL=0
prog="Teragrep HTTP Event Capture"

start() {
    echo "Starting $prog: "
    
    if [ ! -r /opt/teragrep/cfe_16/etc/application.properties ]; then
        echo "Configuration file /opt/teragrep/cfe_16/etc/application.properties does not exist"
        exit 1
    fi
        
    if [ -r /opt/teragrep/cfe_16/var/cfe_16.pid ]; then
        # check if running
        cfe16_pid=$(cat /opt/teragrep/cfe_16/var/cfe_16.pid)
        
        (ps -f -p $cfe16_pid | grep cfe_16.jar > /dev/null 2>&1)
        running=$?

        if [ "$running" = 0 ]; then
           echo "Already running with ${cfe16_pid}"
           RETVAL=1
           return 1
        fi
    fi
    
    ## 
    GENERAL_OPTS="-d64"
    HEAPSIZE_OPTS="-Xmx192M -Xms192M"
    AGENT_OPTS="-javaagent:/opt/teragrep/cfe_16/share/aspectjweaver.jar"

    #JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=1100 -Djava.rmi.server.hostname=localhost"

    GC_OPTS="-XX:+UseConcMarkSweepGC  -XX:+CMSIncrementalPacing -XX:+CMSParallelRemarkEnabled -XX:+CMSClassUnloadingEnabled"
    GC_DEBUG_OPTS="" # -XX:+PrintTenuringDistribution  -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintTLAB

    /usr/bin/nohup /usr/bin/java $GENERAL_OPTS $HEAPSIZE_OPTS $AGENT_OPTS $JMX_OPTS $THREAD_OPTS $GC_OPTS $GC_DEBUG_OPTS -jar /opt/teragrep/cfe_16/share/cfe_16.jar --spring.config.location=file:///opt/teragrep/cfe_16/etc/application.properties > /opt/teragrep/cfe_16/var/stdout.log 2> /opt/teragrep/cfe_16/var/stderr.log &
    RETVAL=$?

    # store pid
    echo $! > /opt/teragrep/cfe_16/var/cfe_16.pid

    [ "$RETVAL" = 0 ] && echo "OK"
    echo
}

stop() {
    RETVAL=1
    cfe16_pid=""
    echo "Stopping $prog: "
        
    if [ -r /opt/teragrep/cfe_16/var/cfe_16.pid ]; then
        # check if running
        cfe16_pid=$(cat /opt/teragrep/cfe_16/var/cfe_16.pid)
        
        (ps -f -p $cfe16_pid | grep cfe_16.jar > /dev/null 2>&1)
        running=$?

        if [ "$running" = 0 ]; then
            kill $cfe16_pid
            while `ps -p $cfe16_pid > /dev/null 2>&1`; do sleep 1; done
            RETVAL=$?
        fi
    fi

    [ "$RETVAL" = 0 ] && echo "OK"
    echo
}

status() {
    RETVAL=1
    echo "$prog status: "

    if [ -r /opt/teragrep/cfe_16/var/cfe_16.pid ]; then
        # check if running
        cfe16_pid=$(cat /opt/teragrep/cfe_16/var/cfe_16.pid)
        
        (ps -f -p $cfe16_pid | grep cfe_16.jar > /dev/null 2>&1)
        running=$?

        if [ "$running" = 0 ]; then
            echo "Pid: ${cfe16_pid}"
            RETVAL=0
            return 0
        fi
    fi
    echo
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        start
        ;;
    reload)
        reload
        ;;
    status)
        status
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|reload|status}"
        RETVAL=1
        ;;
esac
