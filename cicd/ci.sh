#!/bin/bash

sbt -jvm-debug 5005 \
-DPOSTGRES_HOST=localhost \
-DPOSTGRES_PORT=5432 \
-DPOSTGRES_PASSWORD=fake \
-DPOSTGRES_DB=random_menu_selector \
-DPOSTGRES_SSL_MODE=disable \
-DWRITE_PASSWORD=fake \
-DEMAIL_USER=fake@mail.com \
-DEMAIL_PASSWORD=fake \
";clean ;scapegoat ;coverage ;test ;coverageReport"
