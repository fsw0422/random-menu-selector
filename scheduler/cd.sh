#!/usr/bin/env bash

echo ""
echo "=========================="
echo "Menu scheduler is starting"
echo "=========================="
echo ""
ssh kev@fsw0422.com <<-EOF
	git clone git@github.com:fsw0422/random-menu-selector.git || git -C ~/random-menu-selector pull

  cd random-menu-selector/scheduler
  docker run fsw0422/random_menu_selector_menu_scheduler:latest
EOF

