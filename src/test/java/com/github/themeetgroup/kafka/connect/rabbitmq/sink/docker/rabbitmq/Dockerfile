FROM rabbitmq:3.7.10-management

RUN apt-get update && apt-get install -y --no-install-recommends curl bash

RUN rabbitmq-plugins enable --offline rabbitmq_shovel

ADD scripts/wait-for-it.sh /wait-for-it.sh
ADD scripts/entrypoint.sh /entrypoint.sh

CMD ["/entrypoint.sh"]
