# Baseline

## Overview

Baseline is a framework tailored to building JAX-RS 2.x
backend web services. It is heavily influenced by DropWizard and bundles:

* Netty 4
* Jersey 2
* Jackson 2
* JDBI 2
* Tomcat DBCP 8
* Hibernate Validator 5
* Metrics 3
* SLF4J/Logback

## Features

Baseline provides the following features:

* HTTP 1.1 support, including chunked upload/download, long-poll, HTTP pipelining and server-sent-events
* HTTP request tagging and error tagging
* JSON request and response entity support
* Request parameter validation
* JSON-formatted error responses
* Metrics, via a pluggable metrics framework
* DB access and connection pooling
* YAML-based configuration
* Pluggable admin tasks interface
* Pluggable admin check interface

## Motivation

Baseline attempts to simplify the creation of backend
web-services with good out-of-the-box behavior. It is
opinionated, and errs on the side of sane, understandable
defaults over full customizability. It attempts to
take care of much of the boilerplate involved in developing
production web-services, including having to wire together
configuration, metrics, logging, etc.

## Bugs

To report bugs or request features please contact:

* "Allen George" <allen@aerofs.com>
* "Allen George" <allen.george@gmail.com>

## TODO
* Make auth and db baseline *modules* (adds injection, lifecycles, etc. i.e. a package of 'stuff')
* I feel like Environment can be the hub around which modules operate
* Add substantially more tests
* Test all default exception mappers
* Test error conditions (for example, when incorrect JSON is sent to service)
* Simplify base exception mapper interface
* Reduce the number of supplied exceptions
* Configuration exceptions do not print *why* they fail
* Add ability to set number of accept threads
* Flow control incoming connections
* Properly flow control runnables
* Specify application request executor
* Do not run async tasks in timer thread
* Think about async request processing (investigate jersey internals)
* Probably can have the async thread pool and the request processing thread pool be the same
* Do single decode (test performance implications?)
* Add documentation (jekyll?)
* Create apache-http-style request log
* Figure out where SecureRandom is used and why (LazyUid maybe)
* Change timers to use micros instead of nanos
* Re-add CORS filter
* Allow user to specify tracing header
* log direct memory metrics
* Change loglevel via task
* Commands should be able to return an error code (without exception)
* Re-enable use of low-byte-watermark
* Think about streaming http use-case
* Split auth, http and core apart
* Use a single injector across the system (remove injector hierarchy)
