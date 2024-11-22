---
date: 2023-09-06
---

> Use Docker compositions in tests with Test-Containers and ScalaTest.

<!-- truncate -->

[Sample code repository](https://github.com/AdamJKing/blackbox-testing-sample).

We use `docker-compose` as a tool that can build, deploy, and manage multiple containers on the same
network. These configurations, known as compositions, remove a lot of the manual setup we often associate with running
infrastructure locally. It provides local versions of dependencies for a faster, more realistic
feedback loop when developing applications.

Our application level tests could look as simple as this;

<script src="https://emgithub.com/embed-v2.js?target=https%3A%2F%2Fgithub.com%2FAdamJKing%2Fblackbox-testing-sample%2Fblob%2Feed9b2a6bef9fa2049c9be1908415ee43c3a9347%2Fintegration-tests%2Fsrc%2Ftest%2Fscala%2Fblackbox%2Ftesting%2Fsample%2FAppSpec.scala%23L19-L34&style=a11y-light&type=code&showBorder=on&showLineNumbers=on&showFileMeta=on&showFullPath=on&showCopy=on"></script>

### Test Containers

For our tests we will use [test-containers](https://github.com/testcontainers/testcontainers-scala). It offers us;

* Access to a Docker context from within tests.
* Integration with test harnesses provided by popular testing frameworks.
* The automatic creation and clean-up of Docker containers.

`Test-Containers` has a `Docker-Compose` plugin that allows us to run compositions from our tests. First, let’s define a
simple composition file.

<script src="https://emgithub.com/embed-v2.js?target=https%3A%2F%2Fgithub.com%2FAdamJKing%2Fblackbox-testing-sample%2Fblob%2Feed9b2a6bef9fa2049c9be1908415ee43c3a9347%2Fdocker-compose.yaml&style=a11y-light&type=code&showBorder=on&showLineNumbers=on&showFileMeta=on&showFullPath=on&showCopy=on"></script>

I’ve kept it simple by only including our application, but from this point on I can extend it with whatever setup we
need without having to change our tests at all. To start using this in our tests we’ll need to do three things.

1. Build and publish our application image locally.
2. Integrate our composition setup into our test-suite.
3. Create a client that can call our application.

### 1. Build and publish our application image locally.

Before we can use it in a test suite we need to build our application into a Docker image. Unfortunately
`Docker-Compose` does not have a concept of running external build tasks to get an image. That said most build tools
will
allow us to build an image before running our test suite. With SBT
and [native-packager](https://www.scala-sbt.org/sbt-native-packager/) plugin makes it easy to add our Docker build stage
as a prerequisite for our integration tests.

> An SBT quirk means we need to specify our Docker build step for every test task.

```
    Test / test := (Test / test).dependsOn(server / Docker / publishLocal).value,
    Test / testOnly := (Test / testOnly).dependsOn(server / Docker / publishLocal).evaluated,
    Test / testQuick := (Test / testQuick).dependsOn(server / Docker / publishLocal).evaluated
```

### 2. Integrate our composition setup into our test-suite.

Next, we add the image details to our tests.

The `Test-Containers` library has integrations for many test frameworks, which makes this easier, but we’ll still need
to do some wiring. The `Docker-Compose` module needs to know the location of our compose-file. You might want to
hard-code this but that affects the portability of your tests. Instead, I recommend passing the location in as a
property and using SBT to locate the file. This has the benefits us by enabling testing against multiple compose-files;
useful for testing against different environments or setups.

<script src="https://emgithub.com/embed-v2.js?target=https%3A%2F%2Fgithub.com%2FAdamJKing%2Fblackbox-testing-sample%2Fblob%2F388fe4e99956353c83e99f38f60a1c90f903c3fc%2Fbuild.sbt%23L39&style=a11y-light&type=code&showBorder=on&showLineNumbers=on&showFileMeta=on&showFullPath=on&showCopy=on"></script>

If you use an IDE to run test-suites you may need to update the runner config before this will work. In IntelliJ, you
can save a run configuration and share it by committing it to the repo.

[//]: # (![IntelliJ Run Configuration]&#40;/img/IntelliJRunConfig.png&#41;)

We need to wait for our containers to start before using them. We can get around this by making our access details lazy
or use the Test-Containers helpers (which run after the containers finish starting).

<script src="https://emgithub.com/embed-v2.js?target=https%3A%2F%2Fgithub.com%2FAdamJKing%2Fblackbox-testing-sample%2Fblob%2Fmain%2Fintegration-tests%2Fsrc%2Ftest%2Fscala%2Fblackbox%2Ftesting%2Fsample%2FAppFixture.scala%23L34-L36&style=a11y-light&type=code&showBorder=on&showLineNumbers=on&showFileMeta=on&showFullPath=on&showCopy=on"></script>

Lastly we need to instruct Test-Containers on how to use our containers.

<script src="https://emgithub.com/embed-v2.js?target=https%3A%2F%2Fgithub.com%2FAdamJKing%2Fblackbox-testing-sample%2Fblob%2Fabed609fdbe0c4a17dfedebf7005bbc89eead893%2Fintegration-tests%2Fsrc%2Ftest%2Fscala%2Fblackbox%2Ftesting%2Fsample%2FAppFixture.scala%23L39-L47&style=a11y-light&type=code&showBorder=on&showLineNumbers=on&showFileMeta=on&showFullPath=on&showCopy=on"></script>

Some additional considerations you might make;

* We added a wait condition for the service’s health-check so that `Test-Containers` knows to start testing. For more
  complex start conditions check out
  the [test-containers documentation](https://java.testcontainers.org/features/startup_and_waits/#:~:text=not%20a%20daemon.-,Wait%20Strategies,container%20is%20ready%20for%20use).
* We only listen to logs of the main container as logging everything at once creates too much noise. To help with that
  we can either:
    * Set noisy application’s log-levels through the Docker environment.
    * Use Test-Containers logging classes to build something custom.

### 3. Create a client to call our application.

Finally let’s create our HTTP client to call our service. We have full access to the Docker containers and, if needed,
we can connect to our dependencies directly. Alternatively if you provide clients for your service you can use them
here instead.

<script src="https://emgithub.com/embed-v2.js?target=https%3A%2F%2Fgithub.com%2FAdamJKing%2Fblackbox-testing-sample%2Fblob%2Fabed609fdbe0c4a17dfedebf7005bbc89eead893%2Fintegration-tests%2Fsrc%2Ftest%2Fscala%2Fblackbox%2Ftesting%2Fsample%2FAppClient.scala%23L12-L24&style=a11y-light&type=code&showBorder=on&showLineNumbers=on&showFileMeta=on&showFullPath=on&showCopy=on"></script>

## Summary

With our test fixture code in place we can now start writing code without too much concern about the state of our
containers.

<script src="https://emgithub.com/embed-v2.js?target=https%3A%2F%2Fgithub.com%2FAdamJKing%2Fblackbox-testing-sample%2Fblob%2Feed9b2a6bef9fa2049c9be1908415ee43c3a9347%2Fintegration-tests%2Fsrc%2Ftest%2Fscala%2Fblackbox%2Ftesting%2Fsample%2FAppSpec.scala%23L19-L34&style=a11y-light&type=code&showBorder=on&showLineNumbers=on&showFileMeta=on&showFullPath=on&showCopy=on"></script>

Now if we hit a test failure we can launch our `docker-composition` to debug our test case. This helps us switch to an
iterative, live feedback loop for manual testing and experimentation.
