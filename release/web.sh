#!/bin/bash

CONTROLLER_URL=${1:-http://127.0.0.1:19088/controller/index.html}

if command -v open >/dev/null 2>&1; then
	open "$CONTROLLER_URL"
	EXIT_CODE=$?
elif command -v xdg-open >/dev/null 2>&1; then
	xdg-open "$CONTROLLER_URL"
	EXIT_CODE=$?
else
	echo "No browser opener found. Open this URL manually: $CONTROLLER_URL"
	exit 1
fi

if [ $EXIT_CODE -eq 0 ]; then
	echo "Opened: $CONTROLLER_URL"
	echo "Default login: anonymous / cosbench"
	if [ "$CONTROLLER_URL" = "http://127.0.0.1:19088/controller/index.html" ]; then
		echo "Use index.html as the entry page; j_security_check is only the FORM login submit endpoint."
	fi
fi

exit $EXIT_CODE