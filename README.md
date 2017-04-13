# reactive-streams-jvm-extensions-xp

<a href='https://travis-ci.org/akarnokd/reactive-streams-jvm-extensions-xp/builds'><img src='https://travis-ci.org/akarnokd/reactive-streams-jvm-extensions-xp.svg?branch=master'></a>
[![codecov.io](http://codecov.io/github/akarnokd/reactive-streams-jvm-extensions-xp/coverage.svg?branch=master)](http://codecov.io/github/akarnokd/reactive-streams-jvm-extensions-xp?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/reactive-streams-extensions/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/reactive-streams-extensions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.reactivestreams/reactive-streams/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.reactivestreams/reactive-streams)

Experimental extensions to the Reactive-Streams API and TCKs: fusion, queues, standard tools.

## Dependency

```groovy
// The main extension API
compile 'com.github.akarnokd:reactive-streams-extensions:0.2.0'

// Test Compatibility Kit for verifying implementors of the extension API
testCompile 'com.github.akarnokd:reactive-streams-extensions-tck:0.2.0'

// Standard tools for both the regular Reactive-Streams and this extension API
compile 'com.github.akarnokd:reactive-streams-extensions-tools:0.2.0'

// Example Publishers, Processors and Subscribers implemented with the extension API
compile 'com.github.akarnokd:reactive-streams-extensions-examples:0.2.0'
```
