#Data Feeder

Used for feeding data, via a standard socket, to puka in order to test real time implementation.

* Serves data based on client wishes
 * n times or infinite loop

Communication is based on a simple HTTP inspired text protocol using post fixed
status codes as predefined commands. Parameters are delimited with ',' (at the moment), and allows user to pass multiple parameters to the Data Feeder server.

----------------------------


| CODE       | Parameters    | Comment  |
|:----------:|:-------------:|:-----:|
|REQ| -empty- | Empty REQ is used for retrieving available signals |  
|REQ| file name, num=0| REQest file name for usage in stream, TODO: num: of itterations (default=0 infinite) |
|300| requested rate| TODO: Ask the server to reduce sending rate |
|200| message| Used as a verification, message for human readable feedback |
|400| -empty-| General error message, abort communication|
