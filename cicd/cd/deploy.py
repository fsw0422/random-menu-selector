if [ $? -eq 0 ]; then
	echo ""
	echo "===================================="
	echo "Following operations have succeeded"
	echo "- Database evolution"
	echo "- Container image publish"
	echo "Deployment Commencing"
	echo "===================================="
	echo ""
	ssh kev@fsw0422.com <<-EOF
		git clone git@github.com:fsw0422/random-menu-selector.git || git -C ~/random-menu-selector pull

		cd random-menu-selector/cicd || exit
		echo $password | gpg --batch --yes --passphrase-fd 0 production.env.gpg
		docker-compose -f docker-compose.yml pull
		docker-compose -f docker-compose.yml down
		docker-compose -f docker-compose.yml up -d

		"""
        version: "3"
        services:
          random-menu-selector:
            image: "fsw0422/random-menu-selector:latest"
            restart: always
            ports:
              - "9000:9000"
            env_file:
              - production.env
        """
	EOF

	echo ""
	echo "===================="
	echo "Deployment Succeeded"
	echo "===================="
	echo ""
	exit 0
else
	echo ""
	echo "================="
	echo "Deployment Failed"
	echo "================="
	echo ""
	exit 1
fi
