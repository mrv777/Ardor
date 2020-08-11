call npm update
call browserify -s ledgerdevice -p esmify src/connector.template.js -o lib/ledger.connector.js
copy /Y lib\ledger.connector.js ..\..\..\..\html\www\js\hardware\3rdparty\ledger.connector.js