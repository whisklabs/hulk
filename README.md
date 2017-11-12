Hulk
=============

[![Build Status](https://travis-ci.org/whisklabs/hulk.svg?branch=master)](https://travis-ci.org/whisklabs/hulk)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.whisk/hulk-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.whisk/hulk-core_2.12)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Hulk is async driver for Postgresql and CockroachDB.

It uses Mauricio Linhares [async driver](https://github.com/mauricio/postgresql-async) to interact with PostgreSQL and CockroachDB databases in a non blocking way.

Driver's API is hugely inspired by [finagle-postgres](https://github.com/finagle/finagle-postgres), which provides richer interface and type-safete on top of underlying driver. 

## Using the driver

### Installation

```scala
libraryDependencies += "com.whisk" % "hulk-core_2.12" % "0.0.1"
```
