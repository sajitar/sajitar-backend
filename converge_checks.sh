#!/bin/bash
./mvnw clean verify
open target/site/jacoco/index.html
