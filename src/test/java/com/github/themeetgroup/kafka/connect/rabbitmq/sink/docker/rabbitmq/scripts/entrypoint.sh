docker-compose.yml#!/usr/bin/env bash

# To configure vhosts we need to use rabbitmqctl, but rabbitmqctl is not usable
# until rabbitmq-server is running, which needs PID 0 on our container.
#
# The kludge to fix both is to run a background process that waits for
# rabbitmq-server to start and then does the necessary config.

function configure_rabbit {
  rabbitmqctl add_vhost depop-local;
  rabbitmqctl add_user django django;
  rabbitmqctl set_permissions -p depop-local django ".*" ".*" ".*";
  rabbitmqctl set_user_tags django administrator;
  curl -o /usr/local/bin/rabbitmqadmin http://0.0.0.0:15672/cli/rabbitmqadmin \
    && chmod +x /usr/local/bin/rabbitmqadmin \
    && rabbitmqadmin -u django -p django declare exchange --vhost=depop-local name=data.dead type=topic \
    && rabbitmqadmin -u django -p django declare exchange --vhost=depop-local name=data type=topic

}

# start in the background
RABBITMQ_NODE_IP_ADDRESS=127.0.0.1 rabbitmq-server &

# wait for it to initialise and then execute all we need, stop it and restart in normal "bind to all ports" mode.
./wait-for-it.sh -t 60 localhost:5672
configure_rabbit
rabbitmqctl stop
sleep 5
exec rabbitmq-server $@
