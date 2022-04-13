FROM mysql:8.0.19

COPY init/* /docker-entrypoint-initdb.d/
COPY my.cnf /etc/mysql/

CMD ["mysqld"]
