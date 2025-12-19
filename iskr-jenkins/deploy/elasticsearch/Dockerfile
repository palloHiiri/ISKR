FROM elastic/elasticsearch:9.1.3
COPY --chown=elasticsearch:elasticsearch ./elasticsearch.yml /usr/share/elasticsearch/config/
RUN mkdir -p /usr/share/elasticsearch/data
RUN mkdir -p /usr/share/elasticsearch/log