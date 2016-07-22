#Data Feeder

Used for feeding data, via a standard socket, to puka in order to test real time implementation.

* Serves data based on client wishes
 * n times or infinite loop

Communication is based on a simple HTTP inspired text protocol using post fixed
status codes as predefined commands. Parameters are delimited with ',' (at the moment), and allows user to pass multiple parameters to the Data Feeder server.

----------------------------

| Comment       | Parameters    | CODE  |
|:-------------:|:-------------:|:-----:|
|TODO: Empty REQ is used for retrieving available signals | -empty- | REQ|
|TODO: REQest file name for usage in stream, num: of itterations (default=0 infinite) | file name, num=0| REQ|
|TODO: Ask the server to reduce sending rate | requested rate| 300|
|---|---|---|
| Used as a verification, message for human readable feedback | message|200|
| General error message, abort communication| -empty-|400|
