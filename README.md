#Data Feeder

Used for feeding data, via a standard socket, to puka in order to test real time implementation.

* Serves data based on client wishes
 * n times or infinite loop

Communication is based on a simple HTTP inspired text protocol using post fixed
status codes for

_______________
| Comment       | Parameters    | CODE  |
|:-------------:|:-------------:|:-----:|
| Used as a verification, message for human readable feedback | Message|200|
| General error message, abort communication| empty|400|

