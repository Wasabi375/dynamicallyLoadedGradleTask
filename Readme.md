# Dynamically Loaded Gradle Task

[![](https://jitpack.io/v/Wasabi375/dynamicallyLoadedGradleTask.svg)](https://jitpack.io/#Wasabi375/dynamicallyLoadedGradleTask)

A gradle task that let's you run a program and delegate the incremental task execution to it. 

## Startup

The communication with the program is done using the standard in and out channels. 
At the start the program gets all data about the files in the following way:

| Master | |
| --- | --- |
| incremental ||
| true *or* false ||
| input dir ||
| \<input dir> ||
| output dir ||
| \<output dir> ||
| total count|  |
| \<total file count> | |
| total modified | |
| \<total modified count> ||
| total added ||
| \<total added count> ||
| total removed ||
| \<total removed count> ||
| total unchanged ||
| \<total unchanged count> ||

After that client can ask for each of those file categories. The following client messages are by the client are answered
by the master

| Client | Master | |
| --- | --- | --- |
| all | a list of all file names one per line | \<change type>: \<filename> |
| all modified | a list of all modified files on per line | \<filename> |
| all added | a list of all added files on per line | \<filename> |
| all removed | a list of all removed files on per line | \<filename> |
| all unchanged | a list of all unchanged files on per line | \<filename> |
| | | |
| next | the next file or empty line | \<filename> *or* \<newline> |
| next modified | the next modified file or empty line | \<filename> *or* \<newline> |
| next added | the next added file or empty line | \<filename> *or* \<newline> |
| next removed | the next removed file or empty line | \<filename> *or* \<newline> |
| next unchanged | the next unchanged file or empty line | \<filename> *or* \<newline> |

*next* commands and *all* commands are not exclusive. *all* commands will always send all files, even if *next*  
has been used.  

## Result

If the program terminates properly, this is handled as a success case. In case of an unexpected termination (Exception) 
the gradle task fails. 

## Logging

Anything written to the standard err stream will be logged as an error. In order to achieve different logging levels
the first message send to the client should be the log level `{ trace, info, warn, error }` followed in a new line by the message.
If the message is multiple lines long, the log level should be followed by the line count, e.g `warn 3\n...`.

## New Line

The new line character should be `\n`