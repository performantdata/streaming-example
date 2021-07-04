# Streaming example

Copy the `blackbox.amd64` binary into `src/docker/opt/docker/bin/` and
```shell
chmod ug+x src/docker/opt/docker/bin/blackbox.amd64
```

To build a docker image, run:
```shell
sbt docker:publishLocal
```
The server listens on port 8080. Run it in the usual way:
```shell
docker run -p 8080:8080 streaming-example
```
Using the above port number, `http://localhost:8080/words` should return the current word count
(if it is ready; an HTTP 204 until then).
The word count is collected every 30 seconds.

## Design choices

This project is based on my earlier [`unit-conversion`](https://github.com/performantdata/unit-conversion/) one.
It employs Scala 3, and extends the use of http4s and fs2.

* Since we don't know what's in the black box binary that generates the input, we can't trust it,
  so we run it in a Docker container.

* In order to keep this application as the PID 1 process in the Docker container,
  we keep the sbt Docker plugin's defaults: having this application be the `ENTRYPOINT`.
  Thus, we have the _application_ start the black box process (rather than run a pipe in a PID 1 shell).

* The black box software that generates the input data is only run once.
  That is, if it terminates, no attempt is made to restart it.
  With this choice, one also avoids the question of whether one should concatenate the streams from multiple invocations
  as if the generator maintains its state,
  versus considering the streams as independent inputs.

* If the input generator terminates, then this application also terminates.
  This seems acceptable since this application would have nothing new to produce,
  and its termination serves as a notice that the input generator has terminated,
  which is probably an unusual condition.
  (Not yet implemented.)

* The JSON event objects are assumed to be newline-delimited.
  This appears to be the case for a sample of the black box output.
  Something like this needs to be assumed,
  since the bad output from the black box typically starts with an opening brace that doesn't have a matching closing brace.

* Events from the black box are collected for a specific time interval (currently 30 seconds)
  _after which_ their summary is made available via HTTP.
  That is, the summary of the events being collected in the current interval are not published.
