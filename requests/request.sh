#!/bin/bash

BASE_URL=$1
EVENT_TYPE=$2

if [[ "$EVENT_TYPE" == "user" ]]; then
  curl --request POST ${BASE_URL}'/menu/random' \
	  --header 'Content-Type: application/json' \
	  --data '{}'
elif [[ "$EVENT_TYPE" == "user" ]]; then
  curl --request POST ${BASE_URL}"/user" \
    --header "Content-Type: application/json" \
    --data "$(cat ./json/user.json)"
elif [[ "$EVENT_TYPE" == "menu" ]]; then
  curl --request POST ${BASE_URL}"/menu" \
    --header "Content-Type: application/json" \
    --data "$(cat ./json/menu.json)"
elif [[ "$EVENT_TYPE" == "user_view" ]]; then
  curl --request POST ${BASE_URL}'/user/view' \
  	--header 'Content-Type: application/json' \
  	--data "$(cat ./json/user_view.json)"
elif [[ "$EVENT_TYPE" == "menu_view" ]]; then
  curl --request POST ${BASE_URL}'/menu/view' \
  	--header 'Content-Type: application/json' \
  	--data "$(cat ./json/menu_view.json)"
else
  echo "No such event type defined"
fi
